package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Adnotacje zdjec (ETAP 20): GET/PUT/DELETE .../photos/{photoId}/annotation, .../annotation/content,
 * .../documentation-content, GET /api/projects/{projectId}/photos. Prawdziwy SecurityConfig, JWT, H2;
 * pliki w katalogu tymczasowym. Uzytkownik A: projekt A, drzewa A1 i A2, zdjecie na A1; uzytkownik B: projekt B.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:photoannotationapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=photo-annotation-api-test-secret",
        "app.tree-photos.max-file-size=4KB"
})
@AutoConfigureMockMvc
class TreePhotoAnnotationApiIntegrationTest {

    private static final byte[] JPEG_BYTES = withSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 300);
    private static final byte[] PNG_BYTES = withSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 200);
    private static final String VALID_DOCUMENT = """
            {"schemaVersion":1,"elements":[
              {"id":"l1","type":"LINE","points":[{"x":0.1,"y":0.2},{"x":0.5,"y":0.6}],"color":"RED","strokeSize":"MEDIUM"},
              {"id":"a1","type":"ARROW","points":[{"x":0,"y":0},{"x":1,"y":1}],"color":"YELLOW","strokeSize":"LARGE"},
              {"id":"f1","type":"FREEHAND","points":[{"x":0.3,"y":0.3},{"x":0.31,"y":0.35},{"x":0.33,"y":0.4}],"color":"BLUE","strokeSize":"SMALL"},
              {"id":"t1","type":"TEXT","points":[{"x":0.7,"y":0.1}],"color":"WHITE","strokeSize":"MEDIUM","text":"  Ciąć tutaj  "}
            ]}""";

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
    private TreePhotoAnnotationRepository treePhotoAnnotationRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeIdA1;
    private long treeIdA2;
    private long photoIdA1;
    private long projectIdB;
    private long treeIdB1;

    private static byte[] withSignature(byte[] signature, int totalLength) {
        byte[] fileBytes = Arrays.copyOf(signature, totalLength);
        for (int index = signature.length; index < totalLength; index++) fileBytes[index] = (byte) index;
        return fileBytes;
    }

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("annotation.user.a@example.com");
        userAccountB = saveConfirmedArborist("annotation.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\"}")));
        treeIdA1 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"A1\"}")));
        treeIdA2 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"A2\"}")));
        photoIdA1 = readId(performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA1)
                .file(new MockMultipartFile("file", "IMG_0001.jpg", "image/jpeg", JPEG_BYTES))));
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B\"}")));
        treeIdB1 = readId(performAs(userAccountB, post("/api/projects/{p}/trees", projectIdB).contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"B1\"}")));
    }

    @AfterEach
    void deleteAllDataAndFiles() throws IOException {
        treePhotoAnnotationRepository.deleteAll();
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

    private ResultActions performAs(UserAccount userAccount, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization",
                "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail())));
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private static List<Path> storedFiles() throws IOException {
        if (!Files.isDirectory(photoStorageDirectory)) return List.of();
        try (Stream<Path> files = Files.list(photoStorageDirectory)) {
            return files.toList();
        }
    }

    private static String annotationUrl(long projectId, long treeId, long photoId) {
        return "/api/projects/" + projectId + "/trees/" + treeId + "/photos/" + photoId + "/annotation";
    }

    private static MockMultipartHttpServletRequestBuilder putAnnotation(String url, String documentJson, MockMultipartFile renderedImage) {
        MockMultipartHttpServletRequestBuilder request = multipart(HttpMethod.PUT, url);
        if (documentJson != null) request.file(new MockMultipartFile("document", "", "application/json", documentJson.getBytes()));
        if (renderedImage != null) request.file(renderedImage);
        return request;
    }

    private static MockMultipartFile png(byte[] bytes) {
        return new MockMultipartFile("renderedImage", "annotation.png", "image/png", bytes);
    }

    private ResultActions saveValidAnnotationOfUserA() throws Exception {
        return performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), VALID_DOCUMENT, png(PNG_BYTES)));
    }

    private static String documentWithElements(String... elementsJson) {
        return "{\"schemaVersion\":1,\"elements\":[" + String.join(",", elementsJson) + "]}";
    }

    private static String line(String id, String points) {
        return "{\"id\":\"" + id + "\",\"type\":\"LINE\",\"points\":" + points + ",\"color\":\"RED\",\"strokeSize\":\"SMALL\"}";
    }

    private void expectRejectedDocument(String documentJson, String expectedMessage) throws Exception {
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), documentJson, png(PNG_BYTES)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));
    }

    // --- odczyt, zapis, zmiana, usuniecie ---

    @Test
    void missingAnnotationIs404AndPhotoHasNoAnnotation() throws Exception {
        performAs(userAccountA, get(annotationUrl(projectIdA, treeIdA1, photoIdA1)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree photo annotation not found"));
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA1))
                .andExpect(jsonPath("$[0].hasAnnotation").value(false))
                .andExpect(jsonPath("$[0].annotationUpdatedAt").doesNotExist());
    }

    @Test
    void createAnnotationStoresValidatedDocumentAndSeparatePngAndKeepsOriginal() throws Exception {
        List<Path> filesBefore = storedFiles();
        saveValidAnnotationOfUserA()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoId").value(photoIdA1))
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.elements.length()").value(4))
                .andExpect(jsonPath("$.elements[0].type").value("LINE"))
                .andExpect(jsonPath("$.elements[3].text").value("Ciąć tutaj"))
                .andExpect(jsonPath("$.elements[0].text").doesNotExist())
                .andExpect(jsonPath("$.renderedStorageKey").doesNotExist());

        TreePhotoAnnotation annotation = treePhotoAnnotationRepository.findAll().getFirst();
        assertThat(annotation.getRenderedStorageKey()).matches("[0-9a-f-]{36}\\.png");
        assertThat(annotation.getDocumentJson()).doesNotContain("  Ciąć");
        assertThat(storedFiles()).hasSize(filesBefore.size() + 1);
        // Oryginal zdjecia bez zmian.
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos/{f}/content", projectIdA, treeIdA1, photoIdA1))
                .andExpect(content().bytes(JPEG_BYTES));
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA1))
                .andExpect(jsonPath("$[0].hasAnnotation").value(true))
                .andExpect(jsonPath("$[0].annotationUpdatedAt").exists());
        performAs(userAccountA, get(annotationUrl(projectIdA, treeIdA1, photoIdA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[2].points.length()").value(3));
    }

    @Test
    void updateAnnotationReplacesDocumentAndRenderedFileAndDeletesOldFile() throws Exception {
        saveValidAnnotationOfUserA().andExpect(status().isOk());
        String firstRenderedKey = treePhotoAnnotationRepository.findAll().getFirst().getRenderedStorageKey();
        byte[] secondPng = withSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 250);

        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1),
                documentWithElements(line("x", "[{\"x\":0,\"y\":0},{\"x\":0.5,\"y\":0.5}]")), png(secondPng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements.length()").value(1));

        assertThat(treePhotoAnnotationRepository.count()).isEqualTo(1);
        String secondRenderedKey = treePhotoAnnotationRepository.findAll().getFirst().getRenderedStorageKey();
        assertThat(secondRenderedKey).isNotEqualTo(firstRenderedKey);
        assertThat(photoStorageDirectory.resolve(firstRenderedKey)).doesNotExist();
        performAs(userAccountA, get(annotationUrl(projectIdA, treeIdA1, photoIdA1) + "/content"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(secondPng));
    }

    @Test
    void deleteAnnotationRemovesMetadataAndRenderedFileButKeepsOriginalPhoto() throws Exception {
        saveValidAnnotationOfUserA().andExpect(status().isOk());
        String renderedKey = treePhotoAnnotationRepository.findAll().getFirst().getRenderedStorageKey();

        performAs(userAccountA, delete(annotationUrl(projectIdA, treeIdA1, photoIdA1))).andExpect(status().isNoContent());

        assertThat(treePhotoAnnotationRepository.count()).isZero();
        assertThat(photoStorageDirectory.resolve(renderedKey)).doesNotExist();
        assertThat(treePhotoRepository.findById(photoIdA1)).isPresent();
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos/{f}/content", projectIdA, treeIdA1, photoIdA1))
                .andExpect(status().isOk()).andExpect(content().bytes(JPEG_BYTES));
        performAs(userAccountA, delete(annotationUrl(projectIdA, treeIdA1, photoIdA1))).andExpect(status().isNotFound());
    }

    @Test
    void documentationContentReturnsOriginalWithoutAnnotationAndRenderedPngWithAnnotation() throws Exception {
        String documentationUrl = "/api/projects/" + projectIdA + "/trees/" + treeIdA1 + "/photos/" + photoIdA1 + "/documentation-content";
        performAs(userAccountA, get(documentationUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(JPEG_BYTES));

        saveValidAnnotationOfUserA().andExpect(status().isOk());
        performAs(userAccountA, get(documentationUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("IMG_0001-adnotacja.png")))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void deletingPhotoDeletesItsAnnotationAndBothFiles() throws Exception {
        saveValidAnnotationOfUserA().andExpect(status().isOk());
        assertThat(storedFiles()).hasSize(2);

        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}/photos/{f}", projectIdA, treeIdA1, photoIdA1))
                .andExpect(status().isNoContent());

        assertThat(treePhotoAnnotationRepository.count()).isZero();
        assertThat(treePhotoRepository.findById(photoIdA1)).isEmpty();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void projectPhotosListPhotosOfAllTreesWithTreeNumberAndAnnotationState() throws Exception {
        long photoIdA2 = readId(performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA2)
                .file(new MockMultipartFile("file", "a2.png", "image/png", PNG_BYTES))));
        saveValidAnnotationOfUserA().andExpect(status().isOk());

        performAs(userAccountA, get("/api/projects/{p}/photos", projectIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].photoId").value(photoIdA1))
                .andExpect(jsonPath("$[0].inventoryNumber").value("A1"))
                .andExpect(jsonPath("$[0].hasAnnotation").value(true))
                .andExpect(jsonPath("$[1].photoId").value(photoIdA2))
                .andExpect(jsonPath("$[1].treeId").value(treeIdA2))
                .andExpect(jsonPath("$[1].hasAnnotation").value(false))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());
        performAs(userAccountB, get("/api/projects/{p}/photos", projectIdA)).andExpect(status().isNotFound());
    }

    // --- walidacja dokumentu ---

    @Test
    void invalidSchemaVersionIsRejected() throws Exception {
        expectRejectedDocument(VALID_DOCUMENT.replace("\"schemaVersion\":1", "\"schemaVersion\":2"), "Unsupported annotation schema version");
        expectRejectedDocument("{\"elements\":[]}", "Unsupported annotation schema version");
    }

    @Test
    void coordinatesOutsideZeroToOneAreRejected() throws Exception {
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":-0.01,\"y\":0},{\"x\":0.5,\"y\":0.5}]")),
                "Annotation point coordinates must be between 0 and 1");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":0.5,\"y\":1.2}]")),
                "Annotation point coordinates must be between 0 and 1");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":0.5}]")),
                "Annotation point coordinates must be between 0 and 1");
    }

    @Test
    void invalidTypeColorStrokeAndIdAreRejected() throws Exception {
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]").replace("LINE", "POLYGON")),
                "Unsupported annotation element type");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]").replace("RED", "#FF0000")),
                "Unsupported annotation color");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]").replace("SMALL", "HUGE")),
                "Unsupported annotation stroke size");
        expectRejectedDocument(documentWithElements(line("<script>", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]")),
                "Invalid annotation element id");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]"),
                line("a", "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]")), "Duplicate annotation element id");
    }

    @Test
    void invalidPointCountsAndTextAreRejected() throws Exception {
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0}]")), "Invalid number of annotation points");
        expectRejectedDocument(documentWithElements(line("a", "[{\"x\":0,\"y\":0},{\"x\":0.1,\"y\":0.1},{\"x\":0.2,\"y\":0.2}]")
                .replace("LINE", "ARROW")), "Invalid number of annotation points");
        expectRejectedDocument(documentWithElements("{\"id\":\"t\",\"type\":\"TEXT\",\"points\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}],\"color\":\"RED\",\"strokeSize\":\"SMALL\",\"text\":\"x\"}"),
                "Invalid number of annotation points");
        expectRejectedDocument(documentWithElements("{\"id\":\"t\",\"type\":\"TEXT\",\"points\":[{\"x\":0,\"y\":0}],\"color\":\"RED\",\"strokeSize\":\"SMALL\",\"text\":\"  \"}"),
                "Annotation text is mandatory");
        expectRejectedDocument(documentWithElements("{\"id\":\"t\",\"type\":\"TEXT\",\"points\":[{\"x\":0,\"y\":0}],\"color\":\"RED\",\"strokeSize\":\"SMALL\",\"text\":\"" + "a".repeat(501) + "\"}"),
                "Annotation text must have at most 500 characters");
        expectRejectedDocument(documentWithElements(), "Annotation must contain at least one element");
    }

    @Test
    void tooManyElementsAndTooMuchFreehandDataAreRejected() throws Exception {
        String twoHundredOneLines = IntStream.range(0, 201)
                .mapToObj(index -> line("l" + index, "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]")).collect(Collectors.joining(","));
        expectRejectedDocument(documentWithElements(twoHundredOneLines), "Too many annotation elements");
        String thousandOnePoints = IntStream.range(0, 1001).mapToObj(index -> "{\"x\":0.5,\"y\":0.5}").collect(Collectors.joining(",", "[", "]"));
        expectRejectedDocument(documentWithElements(line("f", thousandOnePoints).replace("LINE", "FREEHAND")),
                "Invalid number of annotation points");
        assertThat(treePhotoAnnotationRepository.count()).isZero();
    }

    @Test
    void nonPngOrMissingRenderedImageIsRejectedAndNothingIsStored() throws Exception {
        List<Path> filesBefore = storedFiles();
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), VALID_DOCUMENT,
                new MockMultipartFile("renderedImage", "a.jpg", "image/jpeg", JPEG_BYTES)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string("Rendered annotation image must be a PNG"));
        // Zadeklarowany PNG, ale zawartosc JPEG.
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), VALID_DOCUMENT,
                new MockMultipartFile("renderedImage", "a.png", "image/png", JPEG_BYTES)))
                .andExpect(status().isUnsupportedMediaType());
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), VALID_DOCUMENT, null))
                .andExpect(status().isBadRequest());
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), null, png(PNG_BYTES)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Annotation document is mandatory"));
        byte[] tooLargePng = withSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 5 * 1024);
        performAs(userAccountA, putAnnotation(annotationUrl(projectIdA, treeIdA1, photoIdA1), VALID_DOCUMENT, png(tooLargePng)))
                .andExpect(status().isPayloadTooLarge());
        assertThat(treePhotoAnnotationRepository.count()).isZero();
        assertThat(storedFiles()).hasSameSizeAs(filesBefore);
    }

    // --- wlasnosc i hierarchia ---

    @Test
    void userBCannotReadSaveOrDeleteAnnotationOfUserA() throws Exception {
        saveValidAnnotationOfUserA().andExpect(status().isOk());
        String url = annotationUrl(projectIdA, treeIdA1, photoIdA1);
        performAs(userAccountB, get(url)).andExpect(status().isNotFound());
        performAs(userAccountB, get(url + "/content")).andExpect(status().isNotFound());
        performAs(userAccountB, get(url.replace("/annotation", "/documentation-content"))).andExpect(status().isNotFound());
        performAs(userAccountB, putAnnotation(url, VALID_DOCUMENT, png(PNG_BYTES))).andExpect(status().isNotFound());
        performAs(userAccountB, putAnnotation(annotationUrl(projectIdB, treeIdB1, photoIdA1), VALID_DOCUMENT, png(PNG_BYTES)))
                .andExpect(status().isNotFound());
        performAs(userAccountB, delete(url)).andExpect(status().isNotFound());
        assertThat(treePhotoAnnotationRepository.count()).isEqualTo(1);
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void photoOfOneTreeCannotBeReachedThroughUrlOfAnotherTree() throws Exception {
        saveValidAnnotationOfUserA().andExpect(status().isOk());
        String wrongTreeUrl = annotationUrl(projectIdA, treeIdA2, photoIdA1);
        performAs(userAccountA, get(wrongTreeUrl)).andExpect(status().isNotFound()).andExpect(content().string("Tree photo not found"));
        performAs(userAccountA, putAnnotation(wrongTreeUrl, VALID_DOCUMENT, png(PNG_BYTES))).andExpect(status().isNotFound());
        performAs(userAccountA, delete(wrongTreeUrl)).andExpect(status().isNotFound());
        assertThat(treePhotoAnnotationRepository.count()).isEqualTo(1);
    }
}
