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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lokalizacja drzewa (ETAP 10): PUT/DELETE /api/projects/{projectId}/trees/{treeId}/location.
 * Pelna sciezka API: prawdziwy SecurityConfig, filtr JWT, baza H2. Uzytkownik A ma projekty A1 i A2, B ma B1.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treelocationapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-location-api-test-secret"
})
@AutoConfigureMockMvc
class TreeLocationApiIntegrationTest {

    private static final long NON_EXISTENT_ID = 999_999L;
    private static final String WARSAW_LOCATION_JSON = "{\"latitude\":52.2296756,\"longitude\":21.0122287}";

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
    private long treeIdOfUserA;
    private long treeIdOfUserB;
    private long projectIdB1;

    @BeforeEach
    void createUsersProjectsAndTrees() throws Exception {
        userAccountA = saveConfirmedArborist("location.user.a@example.com");
        userAccountB = saveConfirmedArborist("location.user.b@example.com");
        projectIdA1 = createProjectAs(userAccountA, "Projekt A1");
        projectIdA2 = createProjectAs(userAccountA, "Projekt A2");
        projectIdB1 = createProjectAs(userAccountB, "Projekt B1");
        treeIdOfUserA = createTreeAs(userAccountA, projectIdA1);
        treeIdOfUserB = createTreeAs(userAccountB, projectIdB1);
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

    private long createTreeAs(UserAccount userAccount, long projectId) throws Exception {
        return readId(mockMvc.perform(post("/api/projects/{projectId}/trees", projectId)
                        .header("Authorization", bearerTokenOf(userAccount))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"1\"}"))
                .andExpect(status().isCreated()));
    }

    private ResultActions putLocationAs(UserAccount userAccount, long projectId, long treeId, String requestJson) throws Exception {
        return mockMvc.perform(put("/api/projects/{projectId}/trees/{treeId}/location", projectId, treeId)
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
    }

    private ResultActions deleteLocationAs(UserAccount userAccount, long projectId, long treeId) throws Exception {
        return mockMvc.perform(delete("/api/projects/{projectId}/trees/{treeId}/location", projectId, treeId)
                .header("Authorization", bearerTokenOf(userAccount)));
    }

    private Tree storedTree(long treeId) {
        return treeRepository.findById(treeId).orElseThrow();
    }

    // --- stan poczatkowy ---

    @Test
    void newTreeHasNoLocation() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeIdOfUserA)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").doesNotExist());
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNull();
    }

    // --- ustawienie, zmiana, usuniecie ---

    @Test
    void ownerCanSetValidLocationAndItIsReturnedByDetailsAndList() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(treeIdOfUserA))
                .andExpect(jsonPath("$.location.latitude").value(52.2296756))
                .andExpect(jsonPath("$.location.longitude").value(21.0122287));

        Tree tree = storedTree(treeIdOfUserA);
        assertThat(tree.getLocation().getLatitude()).isEqualByComparingTo("52.2296756");
        assertThat(tree.getLocation().getLongitude()).isEqualByComparingTo("21.0122287");

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeIdOfUserA)
                        .header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(jsonPath("$.location.latitude").value(52.2296756));
        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA1).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(jsonPath("$[0].location.longitude").value(21.0122287));
    }

    @Test
    void ownerCanChangeLocation() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":-33.8688,\"longitude\":-151.2093}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.latitude").value(-33.8688))
                .andExpect(jsonPath("$.location.longitude").value(-151.2093));

        assertThat(storedTree(treeIdOfUserA).getLocation().getLatitude()).isEqualByComparingTo("-33.8688");
    }

    @Test
    void ownerCanClearLocationAndClearingIsIdempotent() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        deleteLocationAs(userAccountA, projectIdA1, treeIdOfUserA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(treeIdOfUserA))
                .andExpect(jsonPath("$.location").doesNotExist());
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNull();

        deleteLocationAs(userAccountA, projectIdA1, treeIdOfUserA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").doesNotExist());
    }

    @Test
    void boundaryCoordinatesAreAccepted() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":90,\"longitude\":-180}")
                .andExpect(status().isOk());
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":-90,\"longitude\":180}")
                .andExpect(status().isOk());
        // 0,0 to poprawny punkt (Zatoka Gwinejska), a NIE oznaczenie braku lokalizacji
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":0,\"longitude\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.latitude").value(0));
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNotNull();
    }

    @Test
    void moreThanSevenDecimalPlacesAreRoundedToColumnScale() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":52.123456789,\"longitude\":21.000000049}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.latitude").value(52.1234568))
                .andExpect(jsonPath("$.location.longitude").value(21.0));
    }

    @Test
    void updatedAtMovesOnlyWhenLocationReallyChanges() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());
        Tree treeWithLocation = storedTree(treeIdOfUserA);

        Thread.sleep(5);
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":52.22967560,\"longitude\":21.0122287}")
                .andExpect(status().isOk());
        assertThat(storedTree(treeIdOfUserA).getUpdatedAt()).isEqualTo(treeWithLocation.getUpdatedAt());

        Thread.sleep(5);
        deleteLocationAs(userAccountA, projectIdA1, treeIdOfUserA).andExpect(status().isOk());
        Tree treeWithoutLocation = storedTree(treeIdOfUserA);
        assertThat(treeWithoutLocation.getUpdatedAt()).isAfter(treeWithLocation.getUpdatedAt());
        assertThat(treeWithoutLocation.getCreatedAt()).isEqualTo(treeWithLocation.getCreatedAt());
    }

    @Test
    void editingTreeFieldsDoesNotTouchLocation() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        mockMvc.perform(patch("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeIdOfUserA)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryNumber\":\"2\",\"latitude\":1,\"location\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryNumber").value("2"))
                .andExpect(jsonPath("$.location.latitude").value(52.2296756));
    }

    // --- walidacja ---

    @Test
    void latitudeOutsideRangeIsRejected() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":90.0000001,\"longitude\":21}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.latitude").value("Latitude must be between -90 and 90"));
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":-90.5,\"longitude\":21}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.latitude").value("Latitude must be between -90 and 90"));
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNull();
    }

    @Test
    void longitudeOutsideRangeIsRejected() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":52,\"longitude\":180.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.longitude").value("Longitude must be between -180 and 180"));
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":52,\"longitude\":-181}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.longitude").value("Longitude must be between -180 and 180"));
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNull();
    }

    @Test
    void partialLocationIsRejectedAndExistingLocationStaysIntact() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":50}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.longitude").value("Longitude is mandatory"));
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"longitude\":20}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.latitude").value("Latitude is mandatory"));
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":null,\"longitude\":null}")
                .andExpect(status().isBadRequest());
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, "{\"latitude\":\"north\",\"longitude\":20}")
                .andExpect(status().isBadRequest());

        assertThat(storedTree(treeIdOfUserA).getLocation().getLatitude()).isEqualByComparingTo("52.2296756");
    }

    // --- wlasnosc ---

    @Test
    void userBCannotSetOrClearLocationOfTreeOfUserA() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        // przez cudzy projekt
        putLocationAs(userAccountB, projectIdA1, treeIdOfUserA, "{\"latitude\":1,\"longitude\":1}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
        deleteLocationAs(userAccountB, projectIdA1, treeIdOfUserA)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
        // przez wlasny projekt z podstawionym cudzym treeId
        putLocationAs(userAccountB, projectIdB1, treeIdOfUserA, "{\"latitude\":1,\"longitude\":1}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
        deleteLocationAs(userAccountB, projectIdB1, treeIdOfUserA)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));

        assertThat(storedTree(treeIdOfUserA).getLocation().getLatitude()).isEqualByComparingTo("52.2296756");
    }

    @Test
    void userBCannotReadTreeOrLocationOfUserA() throws Exception {
        putLocationAs(userAccountA, projectIdA1, treeIdOfUserA, WARSAW_LOCATION_JSON).andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdA1, treeIdOfUserA)
                        .header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("52.2296756"))));
        mockMvc.perform(get("/api/projects/{projectId}/trees", projectIdA1).header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("52.2296756"))));
        mockMvc.perform(get("/api/projects/{projectId}/trees/{treeId}", projectIdB1, treeIdOfUserA)
                        .header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("52.2296756"))));
    }

    @Test
    void treeOfAnotherOwnProjectCannotBeLocatedThroughWrongProjectId() throws Exception {
        long treeIdInA2 = createTreeAs(userAccountA, projectIdA2);

        putLocationAs(userAccountA, projectIdA1, treeIdInA2, WARSAW_LOCATION_JSON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
        assertThat(storedTree(treeIdInA2).getLocation()).isNull();
    }

    @Test
    void nonExistentTreeOrProjectIsNotFound() throws Exception {
        putLocationAs(userAccountA, projectIdA1, NON_EXISTENT_ID, WARSAW_LOCATION_JSON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Tree not found"));
        deleteLocationAs(userAccountA, NON_EXISTENT_ID, treeIdOfUserA)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    @Test
    void ownerOfTreeBIsStillAbleToLocateOwnTree() throws Exception {
        putLocationAs(userAccountB, projectIdB1, treeIdOfUserB, WARSAW_LOCATION_JSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectIdB1));
    }

    // --- brak zalogowania ---

    @Test
    void anonymousUserCannotSetOrClearLocation() throws Exception {
        mockMvc.perform(put("/api/projects/{projectId}/trees/{treeId}/location", projectIdA1, treeIdOfUserA)
                        .contentType(MediaType.APPLICATION_JSON).content(WARSAW_LOCATION_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/projects/{projectId}/trees/{treeId}/location", projectIdA1, treeIdOfUserA))
                .andExpect(status().isUnauthorized());
        assertThat(storedTree(treeIdOfUserA).getLocation()).isNull();
    }
}
