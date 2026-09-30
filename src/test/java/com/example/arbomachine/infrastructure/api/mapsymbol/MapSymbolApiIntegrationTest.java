package com.example.arbomachine.infrastructure.api.mapsymbol;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Symbole mapowe (ETAP 19): katalog GET /api/map-symbols, symbol drzewa i pracy (PUT/DELETE .../map-symbol) oraz
 * lista prac projektu GET /api/projects/{projectId}/works. Prawdziwy SecurityConfig, JWT, H2.
 * Uzytkownik A: projekt A z drzewami A1 i A2 (praca na A1); uzytkownik B: projekt B z drzewem B1 i praca.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mapsymbolapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=map-symbol-api-test-secret"
})
@AutoConfigureMockMvc
class MapSymbolApiIntegrationTest {

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
    private long projectIdA;
    private long treeIdA1;
    private long treeIdA2;
    private long workIdA1;
    private long projectIdB;
    private long treeIdB1;
    private long workIdB1;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("symbol.user.a@example.com");
        userAccountB = saveConfirmedArborist("symbol.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\"}")));
        treeIdA1 = createTree(userAccountA, projectIdA, "A1");
        treeIdA2 = createTree(userAccountA, projectIdA, "A2");
        workIdA1 = createWork(userAccountA, projectIdA, treeIdA1, "Cięcia redukcyjne");
        projectIdB = readId(performAs(userAccountB, post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B\"}")));
        treeIdB1 = createTree(userAccountB, projectIdB, "B1");
        workIdB1 = createWork(userAccountB, projectIdB, treeIdB1, "Praca B");
    }

    @AfterEach
    void deleteAllData() {
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

    private ResultActions performAs(UserAccount userAccount, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization",
                "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail())));
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTree(UserAccount userAccount, long projectId, String inventoryNumber) throws Exception {
        return readId(performAs(userAccount, post("/api/projects/{p}/trees", projectId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"" + inventoryNumber + "\"}")));
    }

    private long createWork(UserAccount userAccount, long projectId, long treeId, String name) throws Exception {
        return readId(performAs(userAccount, post("/api/projects/{p}/trees/{t}/works", projectId, treeId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}")));
    }

    private static MockHttpServletRequestBuilder putSymbol(String url, String symbolCode, Object... uriVariables) {
        return put(url, uriVariables).contentType(MediaType.APPLICATION_JSON).content("{\"symbolCode\":\"" + symbolCode + "\"}");
    }

    private static final String TREE_SYMBOL_URL = "/api/projects/{p}/trees/{t}/map-symbol";
    private static final String WORK_SYMBOL_URL = "/api/projects/{p}/trees/{t}/works/{w}/map-symbol";

    // --- katalog ---

    @Test
    void catalogReturnsStableCodesWithPolishNamesShapesAndColors() throws Exception {
        performAs(userAccountA, get("/api/map-symbols"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains(
                        "GREEN_CIRCLE", "BLUE_SQUARE", "ORANGE_TRIANGLE", "RED_CROSS", "PURPLE_DIAMOND", "BLACK_X")))
                .andExpect(jsonPath("$[*].shape").value(containsInAnyOrder(
                        "CIRCLE", "SQUARE", "TRIANGLE", "CROSS", "DIAMOND", "X")))
                .andExpect(jsonPath("$[0].displayName").value("Zielone koło"))
                .andExpect(jsonPath("$[0].fillColor").value("#7CB342"))
                .andExpect(jsonPath("$[0].strokeColor").value("#FFFFFF"));
    }

    @Test
    void catalogRequiresSignedInUser() throws Exception {
        mockMvc.perform(get("/api/map-symbols")).andExpect(status().isUnauthorized());
    }

    // --- symbol drzewa ---

    @Test
    void newTreeHasNoSymbolAndSymbolCanBeSetChangedAndCleared() throws Exception {
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}", projectIdA, treeIdA1))
                .andExpect(jsonPath("$.mapSymbolCode").doesNotExist());

        performAs(userAccountA, putSymbol(TREE_SYMBOL_URL, " GREEN_CIRCLE ", projectIdA, treeIdA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(treeIdA1))
                .andExpect(jsonPath("$.mapSymbolCode").value("GREEN_CIRCLE"));
        assertThat(treeRepository.findById(treeIdA1).orElseThrow().getMapSymbolCode()).isEqualTo("GREEN_CIRCLE");

        performAs(userAccountA, putSymbol(TREE_SYMBOL_URL, "BLUE_SQUARE", projectIdA, treeIdA1))
                .andExpect(jsonPath("$.mapSymbolCode").value("BLUE_SQUARE"));
        performAs(userAccountA, get("/api/projects/{p}/trees", projectIdA))
                .andExpect(jsonPath("$[0].mapSymbolCode").value("BLUE_SQUARE"));

        performAs(userAccountA, delete(TREE_SYMBOL_URL, projectIdA, treeIdA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mapSymbolCode").doesNotExist());
        assertThat(treeRepository.findById(treeIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    @Test
    void invalidTreeSymbolIsRejected() throws Exception {
        for (String invalidCode : new String[]{"PINK_STAR", "red_cross", "<svg onload=alert(1)>", "../icons/x.svg",
                "https://example.com/x.png", "color: red"}) {
            performAs(userAccountA, putSymbol(TREE_SYMBOL_URL, invalidCode, projectIdA, treeIdA1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Unsupported map symbol"));
        }
        performAs(userAccountA, put(TREE_SYMBOL_URL, projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbolCode\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.symbolCode").value("Map symbol code is mandatory"));
        performAs(userAccountA, putSymbol(TREE_SYMBOL_URL, "A".repeat(65), projectIdA, treeIdA1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.symbolCode").value("Map symbol code must have at most 64 characters"));
        assertThat(treeRepository.findById(treeIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    @Test
    void treeSymbolIsNotPartOfTreeEditForm() throws Exception {
        performAs(userAccountA, putSymbol(TREE_SYMBOL_URL, "GREEN_CIRCLE", projectIdA, treeIdA1)).andExpect(status().isOk());
        // PATCH danych drzewa (komplet pol formularza) nie kasuje symbolu, a symbol z body jest ignorowany.
        performAs(userAccountA, patch("/api/projects/{p}/trees/{t}", projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryNumber\":\"A1\",\"mapSymbolCode\":\"BLACK_X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mapSymbolCode").value("GREEN_CIRCLE"));
    }

    @Test
    void userBCannotChangeTreeSymbolOfUserA() throws Exception {
        performAs(userAccountB, putSymbol(TREE_SYMBOL_URL, "RED_CROSS", projectIdA, treeIdA1))
                .andExpect(status().isNotFound()).andExpect(content().string("Project not found"));
        performAs(userAccountB, putSymbol(TREE_SYMBOL_URL, "RED_CROSS", projectIdB, treeIdA1))
                .andExpect(status().isNotFound()).andExpect(content().string("Tree not found"));
        performAs(userAccountB, delete(TREE_SYMBOL_URL, projectIdA, treeIdA1)).andExpect(status().isNotFound());
        // Nieznany kod dla cudzego drzewa: najpierw wlasnosc (404), a nie walidacja (400) - nie zdradzamy istnienia.
        performAs(userAccountB, putSymbol(TREE_SYMBOL_URL, "PINK_STAR", projectIdA, treeIdA1)).andExpect(status().isNotFound());
        assertThat(treeRepository.findById(treeIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    // --- symbol pracy ---

    @Test
    void workSymbolCanBeSetChangedAndClearedWithoutEditingNameAndDescription() throws Exception {
        performAs(userAccountA, putSymbol(WORK_SYMBOL_URL, "RED_CROSS", projectIdA, treeIdA1, workIdA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workIdA1))
                .andExpect(jsonPath("$.name").value("Cięcia redukcyjne"))
                .andExpect(jsonPath("$.mapSymbolCode").value("RED_CROSS"));
        performAs(userAccountA, putSymbol(WORK_SYMBOL_URL, "ORANGE_TRIANGLE", projectIdA, treeIdA1, workIdA1))
                .andExpect(jsonPath("$.mapSymbolCode").value("ORANGE_TRIANGLE"));
        // Edycja nazwy (PATCH z kompletem pol) nie kasuje symbolu.
        performAs(userAccountA, patch("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA1, workIdA1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Cięcia sanitarne\"}"))
                .andExpect(jsonPath("$.mapSymbolCode").value("ORANGE_TRIANGLE"));
        performAs(userAccountA, delete(WORK_SYMBOL_URL, projectIdA, treeIdA1, workIdA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mapSymbolCode").doesNotExist())
                .andExpect(jsonPath("$.name").value("Cięcia sanitarne"));
        assertThat(treeWorkRepository.findById(workIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    @Test
    void invalidWorkSymbolIsRejected() throws Exception {
        performAs(userAccountA, putSymbol(WORK_SYMBOL_URL, "PINK_STAR", projectIdA, treeIdA1, workIdA1))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Unsupported map symbol"));
        assertThat(treeWorkRepository.findById(workIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    @Test
    void userBCannotChangeWorkSymbolOfUserA() throws Exception {
        performAs(userAccountB, putSymbol(WORK_SYMBOL_URL, "RED_CROSS", projectIdA, treeIdA1, workIdA1))
                .andExpect(status().isNotFound());
        performAs(userAccountB, putSymbol(WORK_SYMBOL_URL, "RED_CROSS", projectIdB, treeIdB1, workIdA1))
                .andExpect(status().isNotFound()).andExpect(content().string("Tree work not found"));
        performAs(userAccountB, delete(WORK_SYMBOL_URL, projectIdA, treeIdA1, workIdA1)).andExpect(status().isNotFound());
        assertThat(treeWorkRepository.findById(workIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    @Test
    void workOfOneTreeCannotBeUpdatedThroughUrlOfAnotherTree() throws Exception {
        performAs(userAccountA, putSymbol(WORK_SYMBOL_URL, "RED_CROSS", projectIdA, treeIdA2, workIdA1))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tree work not found"));
        performAs(userAccountA, delete(WORK_SYMBOL_URL, projectIdA, treeIdA2, workIdA1)).andExpect(status().isNotFound());
        assertThat(treeWorkRepository.findById(workIdA1).orElseThrow().getMapSymbolCode()).isNull();
    }

    // --- prace projektu ---

    @Test
    void projectWorksReturnOnlyWorksOfOwnedProjectGroupedByTree() throws Exception {
        long secondWorkOfA2 = createWork(userAccountA, projectIdA, treeIdA2, "Kontrola");
        performAs(userAccountA, putSymbol(WORK_SYMBOL_URL, "RED_CROSS", projectIdA, treeIdA2, secondWorkOfA2)).andExpect(status().isOk());

        performAs(userAccountA, get("/api/projects/{p}/works", projectIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].treeId").value(contains((int) treeIdA1, (int) treeIdA2)))
                .andExpect(jsonPath("$[1].mapSymbolCode").value("RED_CROSS"))
                .andExpect(jsonPath("$[*].id").value(contains((int) workIdA1, (int) secondWorkOfA2)));

        performAs(userAccountB, get("/api/projects/{p}/works", projectIdA))
                .andExpect(status().isNotFound()).andExpect(content().string("Project not found"));
        performAs(userAccountB, get("/api/projects/{p}/works", projectIdB))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(workIdB1));
        mockMvc.perform(get("/api/projects/{p}/works", projectIdA)).andExpect(status().isUnauthorized());
    }
}
