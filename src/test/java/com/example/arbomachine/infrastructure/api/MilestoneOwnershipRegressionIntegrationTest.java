package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresja bezpieczenstwa milestone'u 1 (ETAP 16): uzytkownik B zna wszystkie id danych uzytkownika A (projekt,
 * drzewo, zdjecie, praca) i probuje kazdego endpointu - odczytu i zapisu. Kazda proba konczy sie 404 i nie zmienia
 * danych A. Ukrycie linkow w React nie jest zabezpieczeniem - sprawdzamy samo API z prawdziwym JWT.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:milestoneownershiptest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=milestone-ownership-test-secret"
})
@AutoConfigureMockMvc
class MilestoneOwnershipRegressionIntegrationTest {

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5, 6, 7, 8};

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
    private TreeWorkRepository treeWorkRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeIdA;
    private long photoIdA;
    private long workIdA;
    private long projectIdB;

    @BeforeEach
    void createDataOfUserAAndProjectOfUserB() throws Exception {
        userAccountA = saveConfirmedArborist("milestone.user.a@example.com");
        userAccountB = saveConfirmedArborist("milestone.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Projekt A\"}")));
        treeIdA = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"A-1\"}")));
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52.1,\"longitude\":21.1}"))
                .andExpect(status().isOk());
        photoIdA = readId(performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA)
                .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES))));
        workIdA = readId(performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeIdA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Cięcia redukcyjne\"}")));
        projectIdB = readId(performAs(userAccountB, post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Projekt B\"}")));
    }

    @AfterEach
    void deleteAllDataAndFiles() throws IOException {
        treePhotoRepository.deleteAll();
        treeWorkRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
        try (Stream<Path> storedFiles = Files.list(photoStorageDirectory)) {
            for (Path storedFile : storedFiles.toList()) Files.delete(storedFile);
        }
    }

    private UserAccount saveConfirmedArborist(String email) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Test").lastName("User").email(email).passwordHash("unused")
                .role(UserRole.ARBORIST).emailConfirmed(true)
                .build());
    }

    private ResultActions performAs(UserAccount userAccount,
                                    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header("Authorization",
                "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail())));
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    /** Wszystkie operacje na danych A - zarowno przez URL projektu A, jak i przez wlasny projekt B z id A. */
    private List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> requestsOfUserBForDataOfUserA() {
        return List.of(
                get("/api/projects/{p}", projectIdA),
                patch("/api/projects/{p}", projectIdA).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Przejęty\"}"),
                get("/api/projects/{p}/summary", projectIdA),
                get("/api/projects/{p}/trees", projectIdA),
                post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"X\"}"),
                get("/api/projects/{p}/trees/{t}", projectIdA, treeIdA),
                get("/api/projects/{p}/trees/{t}", projectIdB, treeIdA),
                patch("/api/projects/{p}/trees/{t}", projectIdA, treeIdA).contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"X\"}"),
                put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":1,\"longitude\":1}"),
                put("/api/projects/{p}/trees/{t}/location", projectIdB, treeIdA).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":1,\"longitude\":1}"),
                delete("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA),
                get("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA),
                get("/api/projects/{p}/trees/{t}/photos/{f}/content", projectIdA, treeIdA, photoIdA),
                get("/api/projects/{p}/trees/{t}/photos/{f}/content", projectIdB, treeIdA, photoIdA),
                delete("/api/projects/{p}/trees/{t}/photos/{f}", projectIdA, treeIdA, photoIdA),
                multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeIdA).file(new MockMultipartFile("file", "x.jpg", "image/jpeg", JPEG_BYTES)),
                get("/api/projects/{p}/trees/{t}/works", projectIdA, treeIdA),
                get("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA, workIdA),
                get("/api/projects/{p}/trees/{t}/works/{w}", projectIdB, treeIdA, workIdA),
                post("/api/projects/{p}/trees/{t}/works", projectIdA, treeIdA).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"),
                patch("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA, workIdA).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"),
                delete("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA, workIdA));
    }

    @Test
    void userBGets404ForEveryOperationOnDataOfUserAAndNothingChanges() throws Exception {
        for (var request : requestsOfUserBForDataOfUserA()) {
            performAs(userAccountB, request)
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .as("%s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                            .isEqualTo(404));
        }

        // Dane A sa nietkniete - sprawdzamy je jako A.
        performAs(userAccountA, get("/api/projects/{p}", projectIdA)).andExpect(jsonPath("$.name").value("Projekt A"));
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}", projectIdA, treeIdA))
                .andExpect(jsonPath("$.inventoryNumber").value("A-1"))
                .andExpect(jsonPath("$.location.latitude").value(52.1));
        performAs(userAccountA, get("/api/projects/{p}/summary", projectIdA))
                .andExpect(jsonPath("$.treeCount").value(1))
                .andExpect(jsonPath("$.photoCount").value(1))
                .andExpect(jsonPath("$.workCount").value(1));
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/photos/{f}/content", projectIdA, treeIdA, photoIdA))
                .andExpect(status().isOk());
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA, workIdA))
                .andExpect(jsonPath("$.name").value("Cięcia redukcyjne"));
        assertThat(treeRepository.count()).isEqualTo(1);
        assertThat(treePhotoRepository.count()).isEqualTo(1);
        assertThat(treeWorkRepository.count()).isEqualTo(1);
    }

    @Test
    void requestsWithoutTokenAreRejectedWith401() throws Exception {
        for (var request : requestsOfUserBForDataOfUserA()) {
            mockMvc.perform(request).andExpect(result -> assertThat(result.getResponse().getStatus())
                    .as("%s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                    .isEqualTo(401));
        }
    }
}
