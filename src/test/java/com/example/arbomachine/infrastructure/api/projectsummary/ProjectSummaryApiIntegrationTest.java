package com.example.arbomachine.infrastructure.api.projectsummary;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Podsumowanie projektu (ETAP 15): GET /api/projects/{projectId}/summary. Prawdziwy SecurityConfig, JWT, H2.
 * Zdjecia zapisujemy wprost jako rekordy metadanych - podsumowanie nie czyta plikow, liczy rekordy TreePhoto.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:projectsummaryapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "auth.jwt.secret=project-summary-api-test-secret"
})
@AutoConfigureMockMvc
class ProjectSummaryApiIntegrationTest {

    private static final long NON_EXISTENT_ID = 999_999L;

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long projectIdB;

    @BeforeEach
    void createUsersAndProjects() throws Exception {
        userAccountA = saveConfirmedArborist("summary.user.a@example.com");
        userAccountB = saveConfirmedArborist("summary.user.b@example.com");
        projectIdA = createProjectAs(userAccountA);
        projectIdB = createProjectAs(userAccountB);
    }

    @AfterEach
    void deleteAllData() {
        treePhotoRepository.deleteAll();
        treeWorkRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
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
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park\"}"))
                .andExpect(status().isCreated()));
    }

    private long createTreeInProjectA(boolean withLocation) throws Exception {
        long treeId = readId(mockMvc.perform(post("/api/projects/{projectId}/trees", projectIdA)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"1\"}"))
                .andExpect(status().isCreated()));
        if (withLocation) {
            mockMvc.perform(put("/api/projects/{projectId}/trees/{treeId}/location", projectIdA, treeId)
                            .header("Authorization", bearerTokenOf(userAccountA))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52.1,\"longitude\":21.1}"))
                    .andExpect(status().isOk());
        }
        return treeId;
    }

    private void addPhotosToTree(long treeId, int photoCount) {
        for (int photoNumber = 0; photoNumber < photoCount; photoNumber++) {
            treePhotoRepository.save(TreePhoto.createForTree(treeRepository.findById(treeId).orElseThrow(), "photo.jpg",
                    UUID.randomUUID() + ".jpg", "image/jpeg", 100, Instant.now(), null));
        }
    }

    private void addWorksToTree(long treeId, int workCount) throws Exception {
        for (int workNumber = 0; workNumber < workCount; workNumber++) {
            mockMvc.perform(post("/api/projects/{projectId}/trees/{treeId}/works", projectIdA, treeId)
                            .header("Authorization", bearerTokenOf(userAccountA))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Praca " + workNumber + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    private ResultActions getSummaryAs(UserAccount userAccount, long projectId) throws Exception {
        return mockMvc.perform(get("/api/projects/{projectId}/summary", projectId)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    private JsonNode summaryOfProjectA() throws Exception {
        return objectMapper.readTree(getSummaryAs(userAccountA, projectIdA)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static void assertSummary(JsonNode summary, long trees, long withLocation, long withoutLocation,
                                      long photos, long works) {
        assertThat(summary.get("treeCount").asLong()).as("treeCount").isEqualTo(trees);
        assertThat(summary.get("treeWithLocationCount").asLong()).as("treeWithLocationCount").isEqualTo(withLocation);
        assertThat(summary.get("treeWithoutLocationCount").asLong()).as("treeWithoutLocationCount").isEqualTo(withoutLocation);
        assertThat(summary.get("photoCount").asLong()).as("photoCount").isEqualTo(photos);
        assertThat(summary.get("workCount").asLong()).as("workCount").isEqualTo(works);
        assertThat(summary.get("treeWithLocationCount").asLong() + summary.get("treeWithoutLocationCount").asLong())
                .as("z lokalizacja + bez lokalizacji == wszystkie drzewa").isEqualTo(summary.get("treeCount").asLong());
    }

    // --- liczby ---

    @Test
    void emptyProjectReturnsAllZero() throws Exception {
        assertSummary(summaryOfProjectA(), 0, 0, 0, 0, 0);
    }

    @Test
    void summaryMatchesMilestoneExample() throws Exception {
        // Drzewo A: lokalizacja, 2 zdjecia, 2 prace; B: lokalizacja, 1 zdjecie; C: bez lokalizacji, 1 praca.
        long treeIdA = createTreeInProjectA(true);
        long treeIdB = createTreeInProjectA(true);
        long treeIdC = createTreeInProjectA(false);
        addPhotosToTree(treeIdA, 2);
        addPhotosToTree(treeIdB, 1);
        addWorksToTree(treeIdA, 2);
        addWorksToTree(treeIdC, 1);

        assertSummary(summaryOfProjectA(), 3, 2, 1, 3, 3);
    }

    @Test
    void treeWithoutPhotosOrWorksStillCountsAsTreeAndLocationCountsAddUp() throws Exception {
        createTreeInProjectA(false);
        createTreeInProjectA(false);
        assertSummary(summaryOfProjectA(), 2, 0, 2, 0, 0);
        long locatedTreeId = createTreeInProjectA(true);
        assertSummary(summaryOfProjectA(), 3, 1, 2, 0, 0);
        // Usuniecie lokalizacji przesuwa drzewo z powrotem do "bez lokalizacji".
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/projects/{projectId}/trees/{treeId}/location", projectIdA, locatedTreeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk());
        assertSummary(summaryOfProjectA(), 3, 0, 3, 0, 0);
    }

    @Test
    void multiplePhotosAndWorksOfOneTreeAreCountedIndividually() throws Exception {
        long treeId = createTreeInProjectA(true);
        addPhotosToTree(treeId, 4);
        addWorksToTree(treeId, 3);
        assertSummary(summaryOfProjectA(), 1, 1, 0, 4, 3);
    }

    @Test
    void dataOfOtherProjectsIsNotCounted() throws Exception {
        long treeIdA = createTreeInProjectA(true);
        addPhotosToTree(treeIdA, 1);
        // Projekt B uzytkownika B ma wlasne drzewo - nie moze wplywac na liczby projektu A i odwrotnie.
        mockMvc.perform(post("/api/projects/{projectId}/trees", projectIdB).header("Authorization", bearerTokenOf(userAccountB))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"B-1\"}"))
                .andExpect(status().isCreated());

        assertSummary(summaryOfProjectA(), 1, 1, 0, 1, 0);
        JsonNode summaryOfProjectB = objectMapper.readTree(getSummaryAs(userAccountB, projectIdB)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertSummary(summaryOfProjectB, 1, 0, 1, 0, 0);
    }

    @Test
    void numberOfSqlStatementsDoesNotGrowWithNumberOfTreesPhotosAndWorks() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long treeId = createTreeInProjectA(true);
        addPhotosToTree(treeId, 1);
        statistics.clear();
        summaryOfProjectA();
        long statementsForSmallProject = statistics.getPrepareStatementCount();
        long entityLoadsForSmallProject = statistics.getEntityLoadCount();

        for (int treeNumber = 0; treeNumber < 5; treeNumber++) {
            long anotherTreeId = createTreeInProjectA(treeNumber % 2 == 0);
            addPhotosToTree(anotherTreeId, 2);
            addWorksToTree(anotherTreeId, 1);
        }
        statistics.clear();
        summaryOfProjectA();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(statementsForSmallProject);
        // Laduja sie tylko konto (filtr JWT + wlasnosc) i projekt - nigdy drzewa, zdjecia ani prace.
        assertThat(statistics.getEntityLoadCount()).as("podsumowanie nie laduje drzew, zdjec ani prac")
                .isEqualTo(entityLoadsForSmallProject);
    }

    // --- wlasnosc ---

    @Test
    void userCannotGetSummaryOfForeignProject() throws Exception {
        createTreeInProjectA(true);
        getSummaryAs(userAccountB, projectIdA)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    @Test
    void nonExistentProjectIsHandledLikeForeignOne() throws Exception {
        getSummaryAs(userAccountA, NON_EXISTENT_ID)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/summary", projectIdA)).andExpect(status().isUnauthorized());
    }
}
