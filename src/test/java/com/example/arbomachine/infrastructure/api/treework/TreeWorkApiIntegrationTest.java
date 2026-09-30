package com.example.arbomachine.infrastructure.api.treework;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Zalecenia/prace przy drzewie (ETAP 14): /api/projects/{projectId}/trees/{treeId}/works/**.
 * Pelna sciezka API: prawdziwy SecurityConfig, filtr JWT, baza H2. Uzytkownik A ma projekt A1 z drzewami A1-1
 * i A1-2, uzytkownik B - projekt B1 z drzewem B1-1.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treeworkapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-work-api-test-secret"
})
@AutoConfigureMockMvc
class TreeWorkApiIntegrationTest {

    private static final long NON_EXISTENT_ID = 999_999L;
    private static final String WORK_JSON = "{\"name\":\"  Cięcia redukcyjne  \",\"description\":\"  Redukcja korony o 20%  \"}";

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
    private TreeWorkRepository treeWorkRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA1;
    private long treeIdA1First;
    private long treeIdA1Second;
    private long projectIdB1;
    private long treeIdB1;

    @BeforeEach
    void createUsersProjectsAndTrees() throws Exception {
        userAccountA = saveConfirmedArborist("work.user.a@example.com");
        userAccountB = saveConfirmedArborist("work.user.b@example.com");
        projectIdA1 = createProjectAs(userAccountA);
        treeIdA1First = createTreeAs(userAccountA, projectIdA1);
        treeIdA1Second = createTreeAs(userAccountA, projectIdA1);
        projectIdB1 = createProjectAs(userAccountB);
        treeIdB1 = createTreeAs(userAccountB, projectIdB1);
    }

    @AfterEach
    void deleteAllData() {
        // Kolejnosc wynika z kluczy obcych: tree_work -> tree -> project -> user_account.
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
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Projekt\"}"))
                .andExpect(status().isCreated()));
    }

    private long createTreeAs(UserAccount userAccount, long projectId) throws Exception {
        return readId(mockMvc.perform(post("/api/projects/{projectId}/trees", projectId)
                        .header("Authorization", bearerTokenOf(userAccount))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"1\"}"))
                .andExpect(status().isCreated()));
    }

    private ResultActions createWorkAs(UserAccount userAccount, long projectId, long treeId, String requestJson)
            throws Exception {
        return mockMvc.perform(post("/api/projects/{projectId}/trees/{treeId}/works", projectId, treeId)
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON).content(requestJson));
    }

    private ResultActions getWorkAs(UserAccount userAccount, long projectId, long treeId, long workId) throws Exception {
        return mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/works/{workId}", projectId, treeId, workId)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    private ResultActions updateWorkAs(UserAccount userAccount, long projectId, long treeId, long workId,
                                       String requestJson) throws Exception {
        return mockMvc.perform(patch("/api/projects/{projectId}/trees/{treeId}/works/{workId}", projectId, treeId, workId)
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON).content(requestJson));
    }

    private ResultActions deleteWorkAs(UserAccount userAccount, long projectId, long treeId, long workId) throws Exception {
        return mockMvc.perform(delete("/api/projects/{projectId}/trees/{treeId}/works/{workId}", projectId, treeId, workId)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    private long createWorkOfUserA() throws Exception {
        return readId(createWorkAs(userAccountA, projectIdA1, treeIdA1First, WORK_JSON).andExpect(status().isCreated()));
    }

    // --- CRUD wlasciciela ---

    @Test
    void ownerCanCreateWorkForOwnTreeWithTrimmedFields() throws Exception {
        long workId = readId(createWorkAs(userAccountA, projectIdA1, treeIdA1First, WORK_JSON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treeId").value(treeIdA1First))
                .andExpect(jsonPath("$.name").value("Cięcia redukcyjne"))
                .andExpect(jsonPath("$.description").value("Redukcja korony o 20%"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists()));

        TreeWork storedWork = treeWorkRepository.findById(workId).orElseThrow();
        assertThat(storedWork.getName()).isEqualTo("Cięcia redukcyjne");
        assertThat(storedWork.getTree().getId()).isEqualTo(treeIdA1First);
    }

    @Test
    void workWithoutDescriptionIsStoredWithNullDescription() throws Exception {
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"Wycinka\",\"description\":\"   \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void ownerCanListManyWorksOfTreeInCreationOrderAndOnlyThatTree() throws Exception {
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"Pierwsza\"}").andExpect(status().isCreated());
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"Druga\"}").andExpect(status().isCreated());
        createWorkAs(userAccountA, projectIdA1, treeIdA1Second, "{\"name\":\"Inne drzewo\"}").andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/works", projectIdA1, treeIdA1First)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Pierwsza"))
                .andExpect(jsonPath("$[1].name").value("Druga"));
    }

    @Test
    void ownerCanGetWork() throws Exception {
        long workId = createWorkOfUserA();
        getWorkAs(userAccountA, projectIdA1, treeIdA1First, workId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workId))
                .andExpect(jsonPath("$.name").value("Cięcia redukcyjne"));
    }

    @Test
    void ownerCanUpdateWorkAndEmptyDescriptionClearsIt() throws Exception {
        long workId = createWorkOfUserA();
        updateWorkAs(userAccountA, projectIdA1, treeIdA1First, workId,
                "{\"name\":\"Cięcia sanitarne\",\"description\":\"Usunąć posusz\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cięcia sanitarne"))
                .andExpect(jsonPath("$.description").value("Usunąć posusz"));

        updateWorkAs(userAccountA, projectIdA1, treeIdA1First, workId, "{\"name\":\"Cięcia sanitarne\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());
        assertThat(treeWorkRepository.findById(workId).orElseThrow().getDescription()).isNull();
    }

    @Test
    void updateWithoutRealChangeDoesNotMoveUpdatedAt() throws Exception {
        long workId = createWorkOfUserA();
        var updatedAtBefore = treeWorkRepository.findById(workId).orElseThrow().getUpdatedAt();
        updateWorkAs(userAccountA, projectIdA1, treeIdA1First, workId,
                "{\"name\":\"Cięcia redukcyjne \",\"description\":\" Redukcja korony o 20%\"}")
                .andExpect(status().isOk());
        assertThat(treeWorkRepository.findById(workId).orElseThrow().getUpdatedAt()).isEqualTo(updatedAtBefore);
    }

    @Test
    void ownerCanDeleteWork() throws Exception {
        long workId = createWorkOfUserA();
        deleteWorkAs(userAccountA, projectIdA1, treeIdA1First, workId).andExpect(status().isNoContent());
        assertThat(treeWorkRepository.findById(workId)).isEmpty();
        getWorkAs(userAccountA, projectIdA1, treeIdA1First, workId).andExpect(status().isNotFound());
    }

    // --- walidacja ---

    @Test
    void blankNameIsRejected() throws Exception {
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Work name is mandatory"));
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"description\":\"bez nazwy\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Work name is mandatory"));
        assertThat(treeWorkRepository.count()).isZero();
    }

    @Test
    void tooLongNameAndDescriptionAreRejected() throws Exception {
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Work name must have at most 255 characters"));
        createWorkAs(userAccountA, projectIdA1, treeIdA1First,
                "{\"name\":\"Praca\",\"description\":\"" + "a".repeat(5001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").value("Work description must have at most 5000 characters"));
        createWorkAs(userAccountA, projectIdA1, treeIdA1First, "{\"name\":\"" + "a".repeat(255) + "\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void blankNameIsRejectedOnUpdate() throws Exception {
        long workId = createWorkOfUserA();
        updateWorkAs(userAccountA, projectIdA1, treeIdA1First, workId, "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Work name is mandatory"));
        assertThat(treeWorkRepository.findById(workId).orElseThrow().getName()).isEqualTo("Cięcia redukcyjne");
    }

    @Test
    void identifiersInRequestBodyAreIgnored() throws Exception {
        createWorkAs(userAccountA, projectIdA1, treeIdA1First,
                "{\"name\":\"Praca\",\"treeId\":" + treeIdB1 + ",\"projectId\":" + projectIdB1 + ",\"ownerId\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treeId").value(treeIdA1First));
    }

    // --- wlasnosc i hierarchia ---

    @Test
    void userBCannotCreateWorkForTreeOfUserA() throws Exception {
        createWorkAs(userAccountB, projectIdA1, treeIdA1First, WORK_JSON)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
        // Wlasny projekt B, ale drzewo A w URL.
        createWorkAs(userAccountB, projectIdB1, treeIdA1First, WORK_JSON)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree not found"));
        assertThat(treeWorkRepository.count()).isZero();
    }

    @Test
    void userBCannotReadWorksOfUserA() throws Exception {
        long workId = createWorkOfUserA();
        getWorkAs(userAccountB, projectIdA1, treeIdA1First, workId).andExpect(status().isNotFound());
        getWorkAs(userAccountB, projectIdB1, treeIdB1, workId)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree work not found"));
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/works", projectIdA1, treeIdA1First)
                        .header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userBCannotUpdateWorkOfUserA() throws Exception {
        long workId = createWorkOfUserA();
        updateWorkAs(userAccountB, projectIdA1, treeIdA1First, workId, "{\"name\":\"Przejęte\"}")
                .andExpect(status().isNotFound());
        updateWorkAs(userAccountB, projectIdB1, treeIdB1, workId, "{\"name\":\"Przejęte\"}")
                .andExpect(status().isNotFound());
        assertThat(treeWorkRepository.findById(workId).orElseThrow().getName()).isEqualTo("Cięcia redukcyjne");
    }

    @Test
    void userBCannotDeleteWorkOfUserA() throws Exception {
        long workId = createWorkOfUserA();
        deleteWorkAs(userAccountB, projectIdA1, treeIdA1First, workId).andExpect(status().isNotFound());
        deleteWorkAs(userAccountB, projectIdB1, treeIdB1, workId).andExpect(status().isNotFound());
        assertThat(treeWorkRepository.findById(workId)).isPresent();
    }

    @Test
    void workOfOneTreeIsNotAccessibleThroughUrlOfAnotherTreeOfSameOwner() throws Exception {
        long workIdOfFirstTree = createWorkOfUserA();

        getWorkAs(userAccountA, projectIdA1, treeIdA1Second, workIdOfFirstTree)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree work not found"));
        updateWorkAs(userAccountA, projectIdA1, treeIdA1Second, workIdOfFirstTree, "{\"name\":\"Zmiana\"}")
                .andExpect(status().isNotFound());
        deleteWorkAs(userAccountA, projectIdA1, treeIdA1Second, workIdOfFirstTree).andExpect(status().isNotFound());

        TreeWork untouchedWork = treeWorkRepository.findById(workIdOfFirstTree).orElseThrow();
        assertThat(untouchedWork.getName()).isEqualTo("Cięcia redukcyjne");
    }

    @Test
    void nonExistentProjectTreeAndWorkAreHandledConsistentlyAs404() throws Exception {
        long workId = createWorkOfUserA();
        getWorkAs(userAccountA, NON_EXISTENT_ID, treeIdA1First, workId)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
        getWorkAs(userAccountA, projectIdA1, NON_EXISTENT_ID, workId)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree not found"));
        getWorkAs(userAccountA, projectIdA1, treeIdA1First, NON_EXISTENT_ID)
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree work not found"));
        createWorkAs(userAccountA, NON_EXISTENT_ID, treeIdA1First, WORK_JSON).andExpect(status().isNotFound());
        createWorkAs(userAccountA, projectIdA1, NON_EXISTENT_ID, WORK_JSON).andExpect(status().isNotFound());
        updateWorkAs(userAccountA, projectIdA1, treeIdA1First, NON_EXISTENT_ID, "{\"name\":\"X\"}")
                .andExpect(status().isNotFound());
        deleteWorkAs(userAccountA, projectIdA1, treeIdA1First, NON_EXISTENT_ID).andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}/works", projectIdA1, treeIdA1First))
                .andExpect(status().isUnauthorized());
    }
}
