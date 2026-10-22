package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Zdjecia drzew (ETAP 13): /api/projects/{projectId}/trees/{treeId}/photos/**. Prawdziwy SecurityConfig, JWT, H2.
 * Pliki trafiaja do katalogu tymczasowego (nigdy do developerskiego storage); limit zdjecia obnizony do 2 KB.
 * Uzytkownik A ma projekt A1 z drzewami A1-1 i A1-2, uzytkownik B - projekt B1 z drzewem B1-1.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treephotoapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-photo-api-test-secret",
        "app.tree-photos.max-file-size=2KB"
})
@AutoConfigureMockMvc
class TreePhotoApiIntegrationTest {

    private static final long NON_EXISTENT_ID = 999_999L;
    private static final byte[] JPEG_BYTES = withSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 300);
    private static final byte[] PNG_BYTES = withSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 200);
    private static final byte[] WEBP_BYTES = withSignature(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, 100);

    @TempDir
    static Path photoStorageDirectory;

    @DynamicPropertySource
    static void usePhotoStorageInTemporaryDirectory(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("app.tree-photos.storage-directory", () -> photoStorageDirectory.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired
    private TreePhotoRepository treePhotoRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA1;
    private long treeIdA1First;
    private long treeIdA1Second;
    private long projectIdB1;
    private long treeIdB1;

    private static byte[] withSignature(byte[] signature, int totalLength) {
        byte[] fileBytes = Arrays.copyOf(signature, totalLength);
        for (int index = signature.length; index < totalLength; index++) fileBytes[index] = (byte) index;
        return fileBytes;
    }

    @BeforeEach
    void createUsersProjectsAndTrees() throws Exception {
        userAccountA = saveConfirmedArborist("photo.user.a@example.com");
        userAccountB = saveConfirmedArborist("photo.user.b@example.com");
        projectIdA1 = createProjectAs(userAccountA);
        treeIdA1First = createTreeAs(userAccountA, projectIdA1);
        treeIdA1Second = createTreeAs(userAccountA, projectIdA1);
        projectIdB1 = createProjectAs(userAccountB);
        treeIdB1 = createTreeAs(userAccountB, projectIdB1);
    }

    @AfterEach
    void deleteAllDataAndFiles() throws IOException {
        // Kolejnosc wynika z kluczy obcych: tree_photo -> tree -> project -> user_account.
        treePhotoRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
        for (Path storedFile : storedFiles()) Files.delete(storedFile);
    }

    private UserAccount saveConfirmedArborist(String email) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Test").lastName("User").email(email).passwordHash("unused")
                .role(UserRole.ARBORIST).emailConfirmed(true)
                .build());
    }

    private String bearerTokenOf(UserAccount userAccount) {
        return "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail());
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long createProjectAs(UserAccount owner) throws Exception {
        return readId(mockMvc.perform(post("/api/projects").header("Authorization", bearerTokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Projekt\"}"))
                .andExpect(status().isCreated()));
    }

    private long createTreeAs(UserAccount userAccount, long projectId) throws Exception {
        return readId(mockMvc.perform(post("/api/projects/{projectId}/trees", projectId)
                        .header("Authorization", bearerTokenOf(userAccount))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"1\"}"))
                .andExpect(status().isCreated()));
    }

    private static List<Path> storedFiles() throws IOException {
        if (!Files.isDirectory(photoStorageDirectory)) return List.of();
        try (Stream<Path> files = Files.list(photoStorageDirectory)) {
            return files.toList();
        }
    }

    private ResultActions uploadAs(UserAccount userAccount, long projectId, long treeId, MockMultipartFile file)
            throws Exception {
        return mockMvc.perform(multipart("/api/projects/{projectId}/trees/{treeId}/photos", projectId, treeId)
                .file(file)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    private ResultActions getContentAs(UserAccount userAccount, long projectId, long treeId, long photoId) throws Exception {
        return mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/photos/{photoId}/content",
                projectId, treeId, photoId).header("Authorization", bearerTokenOf(userAccount)));
    }

    private ResultActions deleteAs(UserAccount userAccount, long projectId, long treeId, long photoId) throws Exception {
        return mockMvc.perform(delete("/api/projects/{projectId}/trees/{treeId}/photos/{photoId}",
                projectId, treeId, photoId).header("Authorization", bearerTokenOf(userAccount)));
    }

    private static MockMultipartFile jpegFile(String originalFileName) {
        return new MockMultipartFile("file", originalFileName, "image/jpeg", JPEG_BYTES);
    }

    private long uploadJpegOfUserA() throws Exception {
        return readId(uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("IMG_0001.jpg"))
                .andExpect(status().isCreated()));
    }

    // --- etap 33: idempotentny upload (zdjecia offline wysylane ponownie po zerwanym polaczeniu) ---

    private ResultActions uploadWithClientIdAs(UserAccount userAccount, long projectId, long treeId, String clientUploadId)
            throws Exception {
        return mockMvc.perform(multipart("/api/projects/{projectId}/trees/{treeId}/photos", projectId, treeId)
                .file(jpegFile("IMG_OFFLINE.jpg"))
                .param("clientUploadId", clientUploadId)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    @Test
    void repeatedUploadWithSameClientUploadIdReturnsExistingPhotoWithoutDuplicate() throws Exception {
        String clientUploadId = "0f8fad5b-d9cb-469f-a165-70867728950e";
        long firstPhotoId = readId(uploadWithClientIdAs(userAccountA, projectIdA1, treeIdA1First, clientUploadId)
                .andExpect(status().isCreated()));
        long repeatedPhotoId = readId(uploadWithClientIdAs(userAccountA, projectIdA1, treeIdA1First, clientUploadId.toUpperCase())
                .andExpect(status().isCreated()));

        assertThat(repeatedPhotoId).isEqualTo(firstPhotoId);
        assertThat(treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeIdA1First)).hasSize(1);
        assertThat(storedFiles()).hasSize(1);
    }

    @Test
    void sameClientUploadIdInAnotherTreeAndUploadsWithoutIdCreateSeparatePhotos() throws Exception {
        String clientUploadId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        uploadWithClientIdAs(userAccountA, projectIdA1, treeIdA1First, clientUploadId).andExpect(status().isCreated());
        uploadWithClientIdAs(userAccountA, projectIdA1, treeIdA1Second, clientUploadId).andExpect(status().isCreated());
        uploadJpegOfUserA();
        uploadJpegOfUserA();

        assertThat(treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeIdA1First)).hasSize(3);
        assertThat(treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeIdA1Second)).hasSize(1);
    }

    @Test
    void clientUploadIdThatIsNotUuidIsRejectedWithoutStoringFile() throws Exception {
        uploadWithClientIdAs(userAccountA, projectIdA1, treeIdA1First, "nie-uuid").andExpect(status().isBadRequest());
        assertThat(treePhotoRepository.count()).isZero();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void clientUploadIdOfAnotherUsersTreeIsNotFound() throws Exception {
        uploadWithClientIdAs(userAccountB, projectIdA1, treeIdA1First, "0f8fad5b-d9cb-469f-a165-70867728950e")
                .andExpect(status().isNotFound());
    }

    // --- dodawanie, lista, tresc, usuwanie ---

    @Test
    void ownerCanUploadPhotoAndMetadataAndFileAreStored() throws Exception {
        long photoId = readId(uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("IMG_0001.jpg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treeId").value(treeIdA1First))
                .andExpect(jsonPath("$.originalFileName").value("IMG_0001.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.fileSizeBytes").value(JPEG_BYTES.length))
                .andExpect(jsonPath("$.createdAt").exists())
                // Frontend nie dostaje klucza storage ani sciezki pliku.
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(content().string(not(containsString(photoStorageDirectory.toString().replace("\\", "\\\\"))))));

        TreePhoto storedPhoto = treePhotoRepository.findById(photoId).orElseThrow();
        assertThat(storedPhoto.getTree().getId()).isEqualTo(treeIdA1First);
        assertThat(storedPhoto.getStorageKey()).matches("[0-9a-f-]{36}\\.jpg");
        Path storedFile = photoStorageDirectory.resolve(storedPhoto.getStorageKey());
        assertThat(storedFile).exists();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(JPEG_BYTES);
    }

    @Test
    void pngAndWebpAreAccepted() throws Exception {
        uploadAs(userAccountA, projectIdA1, treeIdA1First, new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"));
        uploadAs(userAccountA, projectIdA1, treeIdA1First, new MockMultipartFile("file", "a.webp", "image/webp", WEBP_BYTES))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/webp"));
        assertThat(storedFiles()).hasSize(2);
    }

    @Test
    void ownerCanListPhotosOfTreeOnlyFromThatTree() throws Exception {
        uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("pierwsze.jpg")).andExpect(status().isCreated());
        uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("drugie.jpg")).andExpect(status().isCreated());
        uploadAs(userAccountA, projectIdA1, treeIdA1Second, jpegFile("inne.jpg")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/photos", projectIdA1, treeIdA1First)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalFileName").value("pierwsze.jpg"))
                .andExpect(jsonPath("$[1].originalFileName").value("drugie.jpg"))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());
    }

    @Test
    void ownerCanGetPhotoContent() throws Exception {
        long photoId = uploadJpegOfUserA();
        getContentAs(userAccountA, projectIdA1, treeIdA1First, photoId)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes(JPEG_BYTES));
    }

    @Test
    void ownerCanDeletePhotoAndItsFile() throws Exception {
        long photoId = uploadJpegOfUserA();
        Path storedFile = photoStorageDirectory.resolve(treePhotoRepository.findById(photoId).orElseThrow().getStorageKey());

        deleteAs(userAccountA, projectIdA1, treeIdA1First, photoId).andExpect(status().isNoContent());

        assertThat(treePhotoRepository.findById(photoId)).isEmpty();
        assertThat(storedFile).doesNotExist();
        getContentAs(userAccountA, projectIdA1, treeIdA1First, photoId).andExpect(status().isNotFound());
    }

    // --- wlasnosc i hierarchia (IDOR) ---

    @Test
    void userBCannotUploadPhotoToTreeOfUserA() throws Exception {
        uploadAs(userAccountB, projectIdA1, treeIdA1First, jpegFile("x.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
        uploadAs(userAccountB, projectIdB1, treeIdA1First, jpegFile("x.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree not found"));
        assertThat(treePhotoRepository.count()).isZero();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void userBCannotGetPhotoOfUserA() throws Exception {
        long photoId = uploadJpegOfUserA();
        getContentAs(userAccountB, projectIdA1, treeIdA1First, photoId).andExpect(status().isNotFound());
        getContentAs(userAccountB, projectIdB1, treeIdB1, photoId)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree photo not found"));
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/photos", projectIdA1, treeIdA1First)
                        .header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userBCannotDeletePhotoOfUserA() throws Exception {
        long photoId = uploadJpegOfUserA();
        deleteAs(userAccountB, projectIdA1, treeIdA1First, photoId).andExpect(status().isNotFound());
        deleteAs(userAccountB, projectIdB1, treeIdB1, photoId).andExpect(status().isNotFound());
        assertThat(treePhotoRepository.findById(photoId)).isPresent();
        assertThat(storedFiles()).hasSize(1);
    }

    @Test
    void photoOfOneTreeIsNotAccessibleThroughUrlOfAnotherTreeOfSameOwner() throws Exception {
        long photoIdOfFirstTree = uploadJpegOfUserA();
        getContentAs(userAccountA, projectIdA1, treeIdA1Second, photoIdOfFirstTree)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree photo not found"));
        deleteAs(userAccountA, projectIdA1, treeIdA1Second, photoIdOfFirstTree).andExpect(status().isNotFound());
        assertThat(treePhotoRepository.findById(photoIdOfFirstTree)).isPresent();
    }

    @Test
    void nonExistentProjectTreeAndPhotoAreHandledAs404() throws Exception {
        long photoId = uploadJpegOfUserA();
        getContentAs(userAccountA, NON_EXISTENT_ID, treeIdA1First, photoId)
                .andExpect(status().isNotFound()).andExpect(content().string("Project not found"));
        getContentAs(userAccountA, projectIdA1, NON_EXISTENT_ID, photoId)
                .andExpect(status().isNotFound()).andExpect(content().string("Tree not found"));
        getContentAs(userAccountA, projectIdA1, treeIdA1First, NON_EXISTENT_ID)
                .andExpect(status().isNotFound()).andExpect(content().string("Tree photo not found"));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/trees/{treeId}/photos", projectIdA1, treeIdA1First)
                        .file(jpegFile("x.jpg")))
                .andExpect(status().isUnauthorized());
        assertThat(storedFiles()).isEmpty();
    }

    // --- walidacja pliku ---

    @Test
    void unsupportedContentTypeIsRejected() throws Exception {
        uploadAs(userAccountA, projectIdA1, treeIdA1First,
                new MockMultipartFile("file", "notatka.txt", "text/plain", "zwykly tekst".getBytes()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string("Unsupported photo type"));
        uploadAs(userAccountA, projectIdA1, treeIdA1First,
                new MockMultipartFile("file", "plan.pdf", "application/pdf", "%PDF-1.7".getBytes()))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(treePhotoRepository.count()).isZero();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void fileNamedJpgWithoutJpegContentIsRejected() throws Exception {
        // Rozszerzenie i zadeklarowany typ to image/jpeg, ale zawartosc to tekst.
        uploadAs(userAccountA, projectIdA1, treeIdA1First,
                new MockMultipartFile("file", "zdjecie.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes()))
                .andExpect(status().isUnsupportedMediaType());
        // Deklaracja PNG, zawartosc JPEG - typ musi sie zgadzac z zawartoscia.
        uploadAs(userAccountA, projectIdA1, treeIdA1First, new MockMultipartFile("file", "a.png", "image/png", JPEG_BYTES))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void tooLargeFileIsRejected() throws Exception {
        byte[] tooLargeJpeg = withSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 2 * 1024 + 1);
        uploadAs(userAccountA, projectIdA1, treeIdA1First, new MockMultipartFile("file", "duze.jpg", "image/jpeg", tooLargeJpeg))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().string("Photo file is too large"));
        assertThat(treePhotoRepository.count()).isZero();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void missingOrEmptyFileIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/trees/{treeId}/photos", projectIdA1, treeIdA1First)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Photo file is mandatory"));
        uploadAs(userAccountA, projectIdA1, treeIdA1First, new MockMultipartFile("file", "pusty.jpg", "image/jpeg", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Photo file is mandatory"));
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void dangerousFileNameDoesNotAffectStoragePath() throws Exception {
        long photoId = readId(uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("../../../etc/evil.jpg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("evil.jpg")));
        uploadAs(userAccountA, projectIdA1, treeIdA1First, jpegFile("C:\\Windows\\system32\\evil.jpg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("evil.jpg"));

        // Oba pliki leza bezposrednio w katalogu storage pod nazwami UUID, nic nie trafilo poza niego.
        assertThat(storedFiles()).hasSize(2)
                .allSatisfy(storedFile -> assertThat(storedFile.getFileName().toString()).matches("[0-9a-f-]{36}\\.jpg"));
        assertThat(photoStorageDirectory.getParent().resolve("evil.jpg")).doesNotExist();
        assertThat(treePhotoRepository.findById(photoId).orElseThrow().getStorageKey()).doesNotContain("evil");
    }
}
