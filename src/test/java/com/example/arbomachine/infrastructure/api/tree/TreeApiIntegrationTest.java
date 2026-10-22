package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pelna sciezka API drzew: prawdziwy SecurityConfig, filtr JWT i tokeny z JwtTokenService, baza H2.
 * Uzytkownik A ma dwa projekty (A1, A2), uzytkownik B ma projekt B1. Wlasnosc drzewa wynika z projektu.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treeapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-api-test-secret"
})
@AutoConfigureMockMvc
class TreeApiIntegrationTest {

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
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA1;
    private long projectIdA2;
    private long projectIdB1;

    @BeforeEach
    void createUsersAndProjects() throws Exception {
        userAccountA = saveConfirmedArborist("tree.user.a@example.com");
        userAccountB = saveConfirmedArborist("tree.user.b@example.com");
        projectIdA1 = createProjectAs(userAccountA, "Projekt A1");
        projectIdA2 = createProjectAs(userAccountA, "Projekt A2");
        projectIdB1 = createProjectAs(userAccountB, "Projekt B1");
    }

    @AfterEach
    void deleteAllTreesProjectsAndAccounts() {
        // Kolejnosc wynika z kluczy obcych: tree -> project -> user_account.
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

    private long createProjectAs(UserAccount owner, String projectName) throws Exception {
        return readId(mockMvc.perform(post("/api/projects").header("Authorization", bearerTokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + projectName + "\"}"))
                .andExpect(status().isCreated()));
    }

    private ResultActions postTreeAs(UserAccount userAccount, long projectId, String requestJson) throws Exception {
        return mockMvc.perform(post("/api/projects/{projectId}/trees", projectId)
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
    }

    private long createTreeAs(UserAccount userAccount, long projectId, String inventoryNumber) throws Exception {
        return readId(postTreeAs(userAccount, projectId, "{\"inventoryNumber\":\"" + inventoryNumber + "\"}")
                .andExpect(status().isCreated()));
    }

    // --- tworzenie ---

    @Test
    void createTreeInOwnProjectSucceeds() throws Exception {
        postTreeAs(userAccountA, projectIdA1,
                "{\"inventoryNumber\":\"A-12\",\"speciesLatin\":\"Quercus robur\",\"notes\":\"Example field note.\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/projects/" + projectIdA1 + "/trees/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.projectId").value(projectIdA1))
                .andExpect(jsonPath("$.inventoryNumber").value("A-12"))
                .andExpect(jsonPath("$.speciesLatin").value("Quercus robur"))
                .andExpect(jsonPath("$.notes").value("Example field note."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.project").doesNotExist())
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void speciesAndNotesAreOptionalAndTextIsNormalized() throws Exception {
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"  7  \",\"speciesLatin\":\"   \",\"notes\":\"\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventoryNumber").value("7"))
                .andExpect(jsonPath("$.speciesLatin").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void projectIdFromUrlWinsOverProjectIdOrOwnerInBody() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1,
                "{\"inventoryNumber\":\"1\",\"projectId\":" + projectIdB1 + ",\"ownerId\":" + userAccountB.getId() + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectIdA1)));

        assertThat(treeRepository.findByIdAndProjectId(treeId, projectIdA1)).isPresent();
        assertThat(treeRepository.findByIdAndProjectId(treeId, projectIdB1)).isEmpty();
    }

    @Test
    void createTreeInAnotherUsersProjectIsRejectedAsNotFound() throws Exception {
        postTreeAs(userAccountA, projectIdB1, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
        assertThat(treeRepository.count()).isZero();
    }

    @Test
    void createTreeInNonExistentProjectIsNotFound() throws Exception {
        postTreeAs(userAccountA, NON_EXISTENT_ID, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    @Test
    void blankInventoryNumberIsRejected() throws Exception {
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.inventoryNumber").value("Inventory number is mandatory"));
        postTreeAs(userAccountA, projectIdA1, "{\"speciesLatin\":\"Quercus robur\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.inventoryNumber").value("Inventory number is mandatory"));
        assertThat(treeRepository.count()).isZero();
    }

    @Test
    void textsLongerThanColumnsAreRejected() throws Exception {
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"" + "9".repeat(Tree.MAXIMUM_INVENTORY_NUMBER_LENGTH + 1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.inventoryNumber").value("Inventory number must have at most 100 characters"));
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"speciesLatin\":\""
                + "q".repeat(Tree.MAXIMUM_SPECIES_LATIN_LENGTH + 1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.speciesLatin").value("Latin species name must have at most 255 characters"));
    }

    // --- lista ---

    @Test
    void listReturnsOnlyTreesOfSelectedProjectInCreationOrder() throws Exception {
        createTreeAs(userAccountA, projectIdA1, "1");
        createTreeAs(userAccountA, projectIdA1, "2");
        createTreeAs(userAccountA, projectIdA2, "other-project-tree");
        createTreeAs(userAccountB, projectIdB1, "foreign-tree");

        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA1).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].inventoryNumber", contains("1", "2")));
    }

    @Test
    void listOfEmptyOwnProjectIsEmpty() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA2).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listOfAnotherUsersProjectIsRejectedAsNotFound() throws Exception {
        createTreeAs(userAccountB, projectIdB1, "foreign-tree");

        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdB1).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    // --- pojedyncze drzewo ---

    @Test
    void getOwnTreeSucceeds() throws Exception {
        long treeId = createTreeAs(userAccountA, projectIdA1, "15");

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(treeId))
                .andExpect(jsonPath("$.projectId").value(projectIdA1))
                .andExpect(jsonPath("$.inventoryNumber").value("15"));
    }

    @Test
    void treeOfAnotherOwnProjectIsNotReachableThroughWrongProjectId() throws Exception {
        long treeIdInA2 = createTreeAs(userAccountA, projectIdA2, "in-A2");

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeIdInA2)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
    }

    @Test
    void treeOfAnotherUserIsNotReachable() throws Exception {
        long foreignTreeId = createTreeAs(userAccountB, projectIdB1, "foreign-tree");

        // przez cudzy projekt
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdB1, foreignTreeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
        // przez wlasny projekt z podstawionym cudzym treeId
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, foreignTreeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
    }

    @Test
    void nonExistentTreeIsNotFound() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, NON_EXISTENT_ID)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
    }

    // --- edycja (ETAP 7) ---

    private ResultActions patchTreeAs(UserAccount userAccount, long projectId, long treeId, String requestJson) throws Exception {
        return mockMvc.perform(patch("/api/projects/{projectId}/trees/{treeId}", projectId, treeId)
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
    }

    private long createFullTreeAs(UserAccount userAccount, long projectId) throws Exception {
        return readId(postTreeAs(userAccount, projectId,
                "{\"inventoryNumber\":\"1\",\"speciesLatin\":\"Quercus robur\",\"notes\":\"Old note\"}")
                .andExpect(status().isCreated()));
    }

    @Test
    void updateOwnTreeChangesAllEditableFields() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId,
                "{\"inventoryNumber\":\" A-7 \",\"speciesLatin\":\"Tilia cordata\",\"notes\":\"New note\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(treeId))
                .andExpect(jsonPath("$.projectId").value(projectIdA1))
                .andExpect(jsonPath("$.inventoryNumber").value("A-7"))
                .andExpect(jsonPath("$.speciesLatin").value("Tilia cordata"))
                .andExpect(jsonPath("$.notes").value("New note"));

        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getInventoryNumber()).isEqualTo("A-7");
        assertThat(storedTree.getSpeciesLatin()).isEqualTo("Tilia cordata");
        assertThat(storedTree.getNotes()).isEqualTo("New note");
    }

    @Test
    void optionalFieldsCanBeCleared() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"speciesLatin\":\"\",\"notes\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesLatin").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());

        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getSpeciesLatin()).isNull();
        assertThat(storedTree.getNotes()).isNull();
    }

    @Test
    void blankOrMissingInventoryNumberIsRejectedAndTreeStaysIntact() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"   \",\"speciesLatin\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.inventoryNumber").value("Inventory number is mandatory"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"\"}")
                .andExpect(status().isBadRequest());
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"speciesLatin\":\"X\"}")
                .andExpect(status().isBadRequest());

        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getInventoryNumber()).isEqualTo("1");
        assertThat(storedTree.getSpeciesLatin()).isEqualTo("Quercus robur");
    }

    @Test
    void updatedAtMovesOnlyOnRealChange() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);
        Tree treeBefore = treeRepository.findById(treeId).orElseThrow();

        // identyczne dane -> brak zmiany
        patchTreeAs(userAccountA, projectIdA1, treeId,
                "{\"inventoryNumber\":\"1\",\"speciesLatin\":\"Quercus robur\",\"notes\":\"Old note\"}")
                .andExpect(status().isOk());
        assertThat(treeRepository.findById(treeId).orElseThrow().getUpdatedAt()).isEqualTo(treeBefore.getUpdatedAt());

        Thread.sleep(5);
        patchTreeAs(userAccountA, projectIdA1, treeId,
                "{\"inventoryNumber\":\"1\",\"speciesLatin\":\"Quercus robur\",\"notes\":\"Changed\"}")
                .andExpect(status().isOk());
        Tree treeAfter = treeRepository.findById(treeId).orElseThrow();
        assertThat(treeAfter.getUpdatedAt()).isAfter(treeBefore.getUpdatedAt());
        assertThat(treeAfter.getCreatedAt()).isEqualTo(treeBefore.getCreatedAt());
    }

    @Test
    void updateCannotMoveTreeToAnotherProject() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId,
                "{\"inventoryNumber\":\"1\",\"projectId\":" + projectIdA2 + ",\"ownerId\":" + userAccountB.getId() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectIdA1));
        assertThat(treeRepository.findByIdAndProjectId(treeId, projectIdA1)).isPresent();
    }

    @Test
    void treeOfAnotherOwnProjectCannotBeUpdatedThroughWrongProjectId() throws Exception {
        long treeIdInA2 = createFullTreeAs(userAccountA, projectIdA2);

        patchTreeAs(userAccountA, projectIdA1, treeIdInA2, "{\"inventoryNumber\":\"hijacked\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
        assertThat(treeRepository.findById(treeIdInA2).orElseThrow().getInventoryNumber()).isEqualTo("1");
    }

    @Test
    void treeInAnotherUsersProjectCannotBeUpdated() throws Exception {
        long foreignTreeId = createFullTreeAs(userAccountB, projectIdB1);

        patchTreeAs(userAccountA, projectIdB1, foreignTreeId, "{\"inventoryNumber\":\"hijacked\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
        assertThat(treeRepository.findById(foreignTreeId).orElseThrow().getInventoryNumber()).isEqualTo("1");
    }

    @Test
    void updateOfNonExistentTreeOrProjectIsNotFound() throws Exception {
        patchTreeAs(userAccountA, projectIdA1, NON_EXISTENT_ID, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
        patchTreeAs(userAccountA, NON_EXISTENT_ID, NON_EXISTENT_ID, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    @Test
    void anonymousUserCannotUpdateTree() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        mockMvc.perform(patch("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"2\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(treeRepository.findById(treeId).orElseThrow().getInventoryNumber()).isEqualTo("1");
    }

    // --- pola Tree V1 (ETAP 8) ---

    @Test
    void speciesPolishIsStoredOnCreateAndReturned() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1,
                "{\"inventoryNumber\":\"1\",\"speciesPolish\":\"  Dąb szypułkowy \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.speciesPolish").value("Dąb szypułkowy")));

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(jsonPath("$.speciesPolish").value("Dąb szypułkowy"));
    }

    @Test
    void speciesPolishCanBeChangedAndCleared() throws Exception {
        long treeId = createFullTreeAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"speciesPolish\":\"Lipa drobnolistna\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesPolish").value("Lipa drobnolistna"));
        assertThat(treeRepository.findById(treeId).orElseThrow().getSpeciesPolish()).isEqualTo("Lipa drobnolistna");

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"speciesPolish\":\"  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesPolish").doesNotExist());
        assertThat(treeRepository.findById(treeId).orElseThrow().getSpeciesPolish()).isNull();
    }

    @Test
    void conditionAndHazardNotesAreStoredTrimmedChangedAndCleared() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1,
                "{\"inventoryNumber\":\"1\",\"conditionNotes\":\"  Posusz w koronie \",\"hazardNotes\":\"Konar nad chodnikiem\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conditionNotes").value("Posusz w koronie"))
                .andExpect(jsonPath("$.hazardNotes").value("Konar nad chodnikiem")));

        patchTreeAs(userAccountA, projectIdA1, treeId,
                "{\"inventoryNumber\":\"1\",\"conditionNotes\":\"Dobry\",\"hazardNotes\":\"  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionNotes").value("Dobry"))
                .andExpect(jsonPath("$.hazardNotes").doesNotExist());
        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getConditionNotes()).isEqualTo("Dobry");
        assertThat(storedTree.getHazardNotes()).isNull();

        // Formularz wysyla komplet pol - pominiete pole opisowe czysci wartosc (jak notatka).
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionNotes").doesNotExist());
    }

    @Test
    void tooLongSpeciesPolishIsRejected() throws Exception {
        String tooLongName = "d".repeat(Tree.MAXIMUM_SPECIES_POLISH_LENGTH + 1);
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"speciesPolish\":\"" + tooLongName + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.speciesPolish").value("Polish species name must have at most 255 characters"));
        long treeId = createFullTreeAs(userAccountA, projectIdA1);
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"speciesPolish\":\"" + tooLongName + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.speciesPolish").value("Polish species name must have at most 255 characters"));
    }

    @Test
    void trunkCircumferenceIsStoredAsWholeCentimetersAndCanBeChangedAndCleared() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":314}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trunkCircumferenceCm").value(314)));

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":5000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trunkCircumferenceCm").value(5000));
        assertThat(treeRepository.findById(treeId).orElseThrow().getTrunkCircumferenceCm()).isEqualTo(5000);

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trunkCircumferenceCm").doesNotExist());
        assertThat(treeRepository.findById(treeId).orElseThrow().getTrunkCircumferenceCm()).isNull();
    }

    @Test
    void invalidTrunkCircumferenceIsRejectedAndTreeStaysIntact() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":120}")
                .andExpect(status().isCreated()));

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":123.7}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trunkCircumferenceCm").value("Trunk circumference must be a whole number of centimeters"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trunkCircumferenceCm").value("Trunk circumference must be at least 1 cm"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":-5}")
                .andExpect(status().isBadRequest());
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":5001}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trunkCircumferenceCm").value("Trunk circumference must be at most 5000 cm"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"trunkCircumferenceCm\":\"abc\"}")
                .andExpect(status().isBadRequest());

        assertThat(treeRepository.findById(treeId).orElseThrow().getTrunkCircumferenceCm()).isEqualTo(120);
    }

    @Test
    void heightIsStoredInMetersAndCanBeChangedAndCleared() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"heightM\":18.5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.heightM").value(18.5)));
        assertThat(treeRepository.findById(treeId).orElseThrow().getHeightM()).isEqualByComparingTo("18.5");

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":150}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heightM").value(150));

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heightM").doesNotExist());
        assertThat(treeRepository.findById(treeId).orElseThrow().getHeightM()).isNull();
    }

    @Test
    void sameHeightWrittenDifferentlyIsNotAChange() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"heightM\":18.5}")
                .andExpect(status().isCreated()));
        Tree treeBefore = treeRepository.findById(treeId).orElseThrow();

        Thread.sleep(5);
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":18.50}")
                .andExpect(status().isOk());

        assertThat(treeRepository.findById(treeId).orElseThrow().getUpdatedAt()).isEqualTo(treeBefore.getUpdatedAt());
    }

    @Test
    void invalidHeightIsRejectedAndTreeStaysIntact() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"heightM\":12}")
                .andExpect(status().isCreated()));

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":12.345}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.heightM").value("Height can have at most 2 decimal places"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.heightM").value("Height must be greater than 0 m"));
        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"heightM\":150.01}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.heightM").value("Height must be at most 150 m"));

        assertThat(treeRepository.findById(treeId).orElseThrow().getHeightM()).isEqualByComparingTo("12");
    }

    @Test
    void crownDiameterIsStoredInMetersAndCanBeChangedAndCleared() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"crownDiameterM\":12.25}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.crownDiameterM").value(12.25)));

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\",\"crownDiameterM\":200}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crownDiameterM").value(200));
        assertThat(treeRepository.findById(treeId).orElseThrow().getCrownDiameterM()).isEqualByComparingTo("200");

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crownDiameterM").doesNotExist());
        assertThat(treeRepository.findById(treeId).orElseThrow().getCrownDiameterM()).isNull();
    }

    @Test
    void invalidCrownDiameterIsRejected() throws Exception {
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"crownDiameterM\":1.005}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.crownDiameterM").value("Crown diameter can have at most 2 decimal places"));
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"crownDiameterM\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.crownDiameterM").value("Crown diameter must be greater than 0 m"));
        postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"1\",\"crownDiameterM\":200.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.crownDiameterM").value("Crown diameter must be at most 200 m"));
        assertThat(treeRepository.count()).isZero();
    }

    @Test
    void allTreeV1FieldsAreReturnedTogetherByListAndDetails() throws Exception {
        long treeId = readId(postTreeAs(userAccountA, projectIdA1, "{\"inventoryNumber\":\"7\",\"speciesLatin\":\"Tilia cordata\","
                + "\"speciesPolish\":\"Lipa drobnolistna\",\"trunkCircumferenceCm\":251,\"heightM\":21.4,\"crownDiameterM\":14,"
                + "\"notes\":\"Pien z ubytkiem\"}")
                .andExpect(status().isCreated()));

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(jsonPath("$.speciesLatin").value("Tilia cordata"))
                .andExpect(jsonPath("$.speciesPolish").value("Lipa drobnolistna"))
                .andExpect(jsonPath("$.trunkCircumferenceCm").value(251))
                .andExpect(jsonPath("$.heightM").value(21.4))
                .andExpect(jsonPath("$.crownDiameterM").value(14))
                .andExpect(jsonPath("$.notes").value("Pien z ubytkiem"));
        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA1).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(jsonPath("$[0].speciesPolish").value("Lipa drobnolistna"))
                .andExpect(jsonPath("$[0].heightM").value(21.4));
    }

    // --- etap 32: ochrona danych przy zapisie z panelu terenowego ---

    private static final String FULL_TREE_JSON = "{\"inventoryNumber\":\"15\",\"speciesLatin\":\"Quercus robur\","
            + "\"speciesPolish\":\"Dab szypulkowy\",\"trunkCircumferenceCm\":314,\"heightM\":18.5,\"crownDiameterM\":12.25,"
            + "\"notes\":\"Przy lawce\",\"conditionNotes\":\"Posusz w koronie\",\"hazardNotes\":\"Konar nad chodnikiem\"}";

    private long createFullyFilledTreeWithLocationAs(UserAccount userAccount, long projectId) throws Exception {
        long treeId = readId(postTreeAs(userAccount, projectId, FULL_TREE_JSON).andExpect(status().isCreated()));
        mockMvc.perform(put("/api/projects/{projectId}/trees/{treeId}/location", projectId, treeId)
                        .header("Authorization", bearerTokenOf(userAccount)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":52.1,\"longitude\":21.0,\"accuracyM\":3.5,\"source\":\"DEVICE_GNSS\"}"))
                .andExpect(status().isOk());
        return treeId;
    }

    @Test
    void patchWithAllFieldsAndOneChangedFieldKeepsOtherColumnsAndLocation() throws Exception {
        long treeId = createFullyFilledTreeWithLocationAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId,
                FULL_TREE_JSON.replace("Posusz w koronie", "Posusz i ubytek"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionNotes").value("Posusz i ubytek"));

        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getConditionNotes()).isEqualTo("Posusz i ubytek");
        assertThat(storedTree.getInventoryNumber()).isEqualTo("15");
        assertThat(storedTree.getSpeciesLatin()).isEqualTo("Quercus robur");
        assertThat(storedTree.getSpeciesPolish()).isEqualTo("Dab szypulkowy");
        assertThat(storedTree.getTrunkCircumferenceCm()).isEqualTo(314);
        assertThat(storedTree.getHeightM()).isEqualByComparingTo("18.5");
        assertThat(storedTree.getCrownDiameterM()).isEqualByComparingTo("12.25");
        assertThat(storedTree.getNotes()).isEqualTo("Przy lawce");
        assertThat(storedTree.getHazardNotes()).isEqualTo("Konar nad chodnikiem");
        assertThat(storedTree.getLocation()).isNotNull();
        assertThat(storedTree.getLocation().getLatitude()).isEqualByComparingTo("52.1");
        assertThat(storedTree.getLocation().getAccuracyM()).isEqualByComparingTo("3.5");
    }

    /** Dokumentuje obecna zasade PATCH (formularze zawsze wysylaja komplet pol) - zmiana tej zasady wymaga decyzji. */
    @Test
    void patchClearsOptionalFieldsOmittedFromRequestButKeepsLocation() throws Exception {
        long treeId = createFullyFilledTreeWithLocationAs(userAccountA, projectIdA1);

        patchTreeAs(userAccountA, projectIdA1, treeId, "{\"inventoryNumber\":\"15\",\"conditionNotes\":\"Posusz w koronie\"}")
                .andExpect(status().isOk());

        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getConditionNotes()).isEqualTo("Posusz w koronie");
        assertThat(storedTree.getSpeciesLatin()).isNull();
        assertThat(storedTree.getSpeciesPolish()).isNull();
        assertThat(storedTree.getTrunkCircumferenceCm()).isNull();
        assertThat(storedTree.getHeightM()).isNull();
        assertThat(storedTree.getCrownDiameterM()).isNull();
        assertThat(storedTree.getNotes()).isNull();
        assertThat(storedTree.getHazardNotes()).isNull();
        assertThat(storedTree.getLocation()).isNotNull();
    }

    // --- etap 37: konflikt edycji (wersja danych drzewa) ---

    private long dataVersionOf(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("dataVersion").asLong();
    }

    @Test
    void dataVersionGrowsOnFormChangeButNotOnLocationOrUnchangedSave() throws Exception {
        long treeId = createFullyFilledTreeWithLocationAs(userAccountA, projectIdA1);
        long afterCreate = dataVersionOf(mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)
                .header("Authorization", bearerTokenOf(userAccountA))));
        assertThat(afterCreate).as("lokalizacja nie zmienia wersji danych").isZero();

        long afterEdit = dataVersionOf(patchTreeAs(userAccountA, projectIdA1, treeId,
                FULL_TREE_JSON.replace("}", ",\"expectedDataVersion\":0}").replace("Przy lawce", "Przy fontannie"))
                .andExpect(status().isOk()));
        assertThat(afterEdit).isEqualTo(1);
        long afterSameSave = dataVersionOf(patchTreeAs(userAccountA, projectIdA1, treeId,
                FULL_TREE_JSON.replace("Przy lawce", "Przy fontannie")).andExpect(status().isOk()));
        assertThat(afterSameSave).as("zapis bez zmian").isEqualTo(1);
    }

    @Test
    void staleDataVersionIsRejectedWith409AndKeepsOtherDevicesChanges() throws Exception {
        long treeId = createFullyFilledTreeWithLocationAs(userAccountA, projectIdA1);
        // Urzadzenie 1 zapisuje zmiane (wersja 0 -> 1).
        patchTreeAs(userAccountA, projectIdA1, treeId,
                FULL_TREE_JSON.replace("}", ",\"expectedDataVersion\":0}").replace("Posusz w koronie", "Posusz i ubytek"))
                .andExpect(status().isOk());
        // Urzadzenie 2 widzialo wersje 0 i zmienia inne pole - konflikt, bez nadpisania.
        patchTreeAs(userAccountA, projectIdA1, treeId,
                FULL_TREE_JSON.replace("}", ",\"expectedDataVersion\":0}").replace("Przy lawce", "Przy fontannie"))
                .andExpect(status().isConflict());
        Tree storedTree = treeRepository.findById(treeId).orElseThrow();
        assertThat(storedTree.getConditionNotes()).isEqualTo("Posusz i ubytek");
        assertThat(storedTree.getNotes()).isEqualTo("Przy lawce");

        // "Zapisz mimo to" (bez wersji) nadpisuje swiadomie - jak przed etapem 37.
        patchTreeAs(userAccountA, projectIdA1, treeId, FULL_TREE_JSON.replace("Przy lawce", "Przy fontannie"))
                .andExpect(status().isOk());
        assertThat(treeRepository.findById(treeId).orElseThrow().getNotes()).isEqualTo("Przy fontannie");
    }

    // --- brak zalogowania ---

    @Test
    void anonymousUserHasNoAccessToTreeApi() throws Exception {
        long treeId = createTreeAs(userAccountA, projectIdA1, "1");

        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA1)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/{projectId}/trees", projectIdA1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"2\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(treeRepository.count()).isEqualTo(1);
    }
}
