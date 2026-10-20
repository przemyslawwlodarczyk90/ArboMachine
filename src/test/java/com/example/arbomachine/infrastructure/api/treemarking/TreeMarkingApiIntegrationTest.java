package com.example.arbomachine.infrastructure.api.treemarking;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Oznaczenia drzew (ETAP 31): katalog, komplet oznaczen drzewa (PUT), lista projektu, walidacja kodow, IDOR. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treemarkingapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-marking-api-test-secret",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class TreeMarkingApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeMarkingRepository treeMarkingRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long tree1;
    private long tree2;
    private long projectIdB;
    private long treeB1;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("marking.user.a@example.com");
        userAccountB = saveConfirmedArborist("marking.user.b@example.com");
        projectIdA = createProject(userAccountA, "Park A");
        tree1 = createTree(userAccountA, projectIdA, "M-1");
        tree2 = createTree(userAccountA, projectIdA, "M-2");
        projectIdB = createProject(userAccountB, "Park B");
        treeB1 = createTree(userAccountB, projectIdB, "B-1");
    }

    @AfterEach
    void deleteAllData() {
        treeMarkingRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void catalogListsMarkingsWithCategoryShortLabelAndColor() throws Exception {
        performAs(userAccountA, get("/api/tree-markings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VALUABLE"))
                .andExpect(jsonPath("$[0].displayName").value("Drzewo wartościowe"))
                .andExpect(jsonPath("$[0].category").value("VALUE"))
                .andExpect(jsonPath("$[0].categoryDisplayName").value("Wartość"))
                .andExpect(jsonPath("$[0].shortLabel").value("W"))
                .andExpect(jsonPath("$[0].color").value("#2E7D32"));
        mockMvc.perform(get("/api/tree-markings")).andExpect(status().isUnauthorized());
    }

    @Test
    void treeGetsManyMarkingsInCatalogOrderAndPutReplacesTheWholeSet() throws Exception {
        setMarkings(userAccountA, projectIdA, tree1, "[\" SNAG \",\"TO_REMOVE\",\"VALUABLE\",\"TO_REMOVE\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treeId").value(tree1))
                .andExpect(jsonPath("$.codes", contains("VALUABLE", "TO_REMOVE", "SNAG")));
        assertThat(treeMarkingRepository.findAllByTreeId(tree1)).hasSize(3);

        setMarkings(userAccountA, projectIdA, tree1, "[\"SNAG\",\"HOLLOW\"]")
                .andExpect(jsonPath("$.codes", contains("SNAG", "HOLLOW")));
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/markings", projectIdA, tree1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codes", contains("SNAG", "HOLLOW")));

        setMarkings(userAccountA, projectIdA, tree1, "[]").andExpect(jsonPath("$.codes", hasSize(0)));
        assertThat(treeMarkingRepository.findAllByTreeId(tree1)).isEmpty();
    }

    @Test
    void projectListContainsOnlyTreesWithMarkings() throws Exception {
        setMarkings(userAccountA, projectIdA, tree2, "[\"HAZARD\"]").andExpect(status().isOk());
        setMarkings(userAccountB, projectIdB, treeB1, "[\"NEST\"]").andExpect(status().isOk());
        performAs(userAccountA, get("/api/projects/{p}/tree-markings", projectIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].treeId").value(tree2))
                .andExpect(jsonPath("$[0].codes", contains("HAZARD")));
    }

    @Test
    void unknownCodeIsRejectedAndNothingChanges() throws Exception {
        setMarkings(userAccountA, projectIdA, tree1, "[\"VALUABLE\"]").andExpect(status().isOk());
        setMarkings(userAccountA, projectIdA, tree1, "[\"TO_REMOVE\",\"<script>\"]").andExpect(status().isBadRequest());
        setMarkings(userAccountA, projectIdA, tree1, "null").andExpect(status().isBadRequest());
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/markings", projectIdA, tree1))
                .andExpect(jsonPath("$.codes", contains("VALUABLE")));
    }

    @Test
    void otherUsersProjectAndTreeAreNotFound() throws Exception {
        setMarkings(userAccountA, projectIdB, treeB1, "[\"VALUABLE\"]").andExpect(status().isNotFound());
        setMarkings(userAccountA, projectIdA, treeB1, "[\"VALUABLE\"]").andExpect(status().isNotFound());
        performAs(userAccountA, get("/api/projects/{p}/tree-markings", projectIdB)).andExpect(status().isNotFound());
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/markings", projectIdB, treeB1)).andExpect(status().isNotFound());
        assertThat(treeMarkingRepository.findAllByTreeId(treeB1)).isEmpty();
    }

    private ResultActions setMarkings(UserAccount userAccount, long projectId, long treeId, String codesJson) throws Exception {
        return performAs(userAccount, put("/api/projects/{p}/trees/{t}/markings", projectId, treeId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"codes\":" + codesJson + "}"));
    }

    private UserAccount saveConfirmedArborist(String email) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Test").lastName("User").email(email).passwordHash("unused")
                .role(UserRole.ARBORIST).emailConfirmed(true)
                .build());
    }

    private ResultActions performAs(UserAccount userAccount, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail())));
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long createProject(UserAccount owner, String name) throws Exception {
        return readId(performAs(owner, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}")));
    }

    private long createTree(UserAccount owner, long projectId, String inventoryNumber) throws Exception {
        return readId(performAs(owner, post("/api/projects/{p}/trees", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"" + inventoryNumber + "\"}")));
    }
}
