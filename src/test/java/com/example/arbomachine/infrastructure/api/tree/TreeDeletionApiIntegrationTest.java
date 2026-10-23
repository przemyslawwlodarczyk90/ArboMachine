package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Usuwanie drzewa (ETAP 44): DELETE /api/projects/{projectId}/trees/{treeId} usuwa drzewo razem ze zdjeciami (takze
 * plikami), pracami, waloryzacja, kontrolami, oznaczeniami i przynaleznoscia do grup; inne drzewa zostaja nietkniete.
 * Prawdziwy SecurityConfig, JWT, H2, pliki zdjec w katalogu tymczasowym.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treedeletionapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-deletion-api-test-secret"
})
@AutoConfigureMockMvc
class TreeDeletionApiIntegrationTest {

    private static final byte[] JPEG_BYTES = jpegBytes();
    private static final String SQUARE = "{\"type\":\"Polygon\",\"coordinates\":[[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.409],[21.149,50.408]]]}";

    @TempDir
    static Path photoStorageDirectory;

    @DynamicPropertySource
    static void usePhotoStorageInTemporaryDirectory(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("app.tree-photos.storage-directory", () -> photoStorageDirectory.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreePhotoRepository treePhotoRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private TreeValuationRepository treeValuationRepository;
    @Autowired private TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    @Autowired private TreeMarkingRepository treeMarkingRepository;
    @Autowired private TreeStandRepository treeStandRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeToDelete;
    private long treeToKeep;
    private long standId;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("deletion.user.a@example.com");
        userAccountB = saveConfirmedArborist("deletion.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        treeToDelete = createTreeWithAllData("1");
        treeToKeep = createTreeWithAllData("2");
        standId = readId(performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Aleja\",\"geometry\":" + SQUARE + ",\"treeIds\":[" + treeToDelete + "," + treeToKeep
                        + "],\"assignTreesInside\":false}")));
    }

    @AfterEach
    void deleteAllData() throws IOException {
        treeStandRepository.deleteAll();
        treeMarkingRepository.deleteAll();
        treeMonitoringRecordRepository.deleteAll();
        treeValuationRepository.deleteAll();
        treeWorkRepository.deleteAll();
        treePhotoRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
        try (Stream<Path> files = Files.walk(photoStorageDirectory)) {
            files.sorted((first, second) -> second.compareTo(first)).filter(path -> !path.equals(photoStorageDirectory))
                    .forEach(path -> path.toFile().delete());
        }
    }

    private long createTreeWithAllData(String inventoryNumber) throws Exception {
        long treeId = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"" + inventoryNumber + "\"}")));
        performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeId)
                .file(new MockMultipartFile("file", "IMG_" + inventoryNumber + ".jpg", "image/jpeg", JPEG_BYTES)))
                .andExpect(status().isCreated());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cięcie sanitarne\"}")).andExpect(status().is2xxSuccessful());
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"overallCategory\":\"HIGH\"}")).andExpect(status().isOk());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/monitoring", projectIdA, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inspectionDate\":\"2026-05-10\",\"healthStatus\":\"GOOD\"}")).andExpect(status().is2xxSuccessful());
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/markings", projectIdA, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codes\":[\"SNAG\",\"HAZARD\"]}")).andExpect(status().isOk());
        return treeId;
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

    private static byte[] jpegBytes() {
        byte[] bytes = new byte[300];
        Arrays.fill(bytes, (byte) 7);
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        return bytes;
    }

    private long storedFileCount() throws IOException {
        try (Stream<Path> files = Files.walk(photoStorageDirectory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void deletingTreeRemovesItsDataAndFilesButKeepsOtherTree() throws Exception {
        assertThat(storedFileCount()).isEqualTo(2);

        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}", projectIdA, treeToDelete)).andExpect(status().isNoContent());

        performAs(userAccountA, get("/api/projects/{p}/trees/{t}", projectIdA, treeToDelete)).andExpect(status().isNotFound());
        assertThat(treeRepository.count()).isEqualTo(1);
        assertThat(treePhotoRepository.count()).isEqualTo(1);
        assertThat(treeWorkRepository.count()).isEqualTo(1);
        assertThat(treeValuationRepository.count()).isEqualTo(1);
        assertThat(treeMonitoringRecordRepository.count()).isEqualTo(1);
        assertThat(treeMarkingRepository.findAllByTreeId(treeToKeep)).hasSize(2);
        assertThat(treeMarkingRepository.count()).isEqualTo(2);
        assertThat(storedFileCount()).isEqualTo(1);

        JsonNode stand = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/stands", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).get(0);
        assertThat(stand.get("id").asLong()).isEqualTo(standId);
        List<Long> standTreeIds = StreamSupport.stream(stand.get("treeIds").spliterator(), false).map(JsonNode::asLong).toList();
        assertThat(standTreeIds).containsExactly(treeToKeep);

        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos", projectIdA, treeToKeep)).andExpect(status().isOk());
    }

    @Test
    void deletingAgainOrForeignTreeGives404AndChangesNothing() throws Exception {
        performAs(userAccountB, delete("/api/projects/{p}/trees/{t}", projectIdA, treeToDelete)).andExpect(status().isNotFound());
        assertThat(treeRepository.count()).isEqualTo(2);

        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}", projectIdA, treeToDelete)).andExpect(status().isNoContent());
        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}", projectIdA, treeToDelete)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/projects/{p}/trees/{t}", projectIdA, treeToKeep)).andExpect(status().isUnauthorized());
        assertThat(treeRepository.count()).isEqualTo(1);
    }
}
