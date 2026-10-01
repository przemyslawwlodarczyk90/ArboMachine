package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeLocationSource;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAP 25-26: lokalizacja z danymi pomiaru GNSS (wysokosc, dokladnosc, zrodlo) przez istniejacy
 * PUT .../trees/{treeId}/location oraz sekwencja operacji wysylana przez synchronizacje offline
 * (utworzenie drzewa -> od razu lokalizacja -> edycja -> praca). Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treelocationgnssapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-location-gnss-api-test-secret"
})
@AutoConfigureMockMvc
class TreeLocationGnssApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeIdA;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("gnss.user.a@example.com");
        userAccountB = saveConfirmedArborist("gnss.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"GNSS\"}")));
        treeIdA = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"G-1\"}")));
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

    private ResultActions putLocation(UserAccount userAccount, long treeId, String body) throws Exception {
        return performAs(userAccount, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Tree storedTree(long treeId) {
        return treeRepository.findById(treeId).orElseThrow();
    }

    @Test
    void gnssMeasurementIsStoredRoundedAndReturned() throws Exception {
        putLocation(userAccountA, treeIdA, """
                {"latitude":52.229675612,"longitude":21.012228711,"altitudeM":104.4567,"accuracyM":3.987,"source":"DEVICE_GNSS"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.latitude").value(52.2296756))
                .andExpect(jsonPath("$.location.altitudeM").value(104.46))
                .andExpect(jsonPath("$.location.accuracyM").value(3.99))
                .andExpect(jsonPath("$.location.source").value("DEVICE_GNSS"));

        Tree tree = storedTree(treeIdA);
        assertThat(tree.getLocation().getAltitudeM()).isEqualByComparingTo("104.46");
        assertThat(tree.getLocation().getAccuracyM()).isEqualByComparingTo("3.99");
        assertThat(tree.getLocation().getSource()).isEqualTo(TreeLocationSource.DEVICE_GNSS);
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}", projectIdA, treeIdA))
                .andExpect(jsonPath("$.location.source").value("DEVICE_GNSS"))
                .andExpect(jsonPath("$.location.accuracyM").value(3.99));
    }

    @Test
    void manualAndMapSourcesAreAcceptedAndMeasurementIsOptional() throws Exception {
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"source\":\"MAP\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.source").value("MAP"))
                .andExpect(jsonPath("$.location.accuracyM").doesNotExist());
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"source\":\"MANUAL\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.source").value("MANUAL"));
        putLocation(userAccountA, treeIdA, "{\"latitude\":52.1,\"longitude\":21.1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.source").doesNotExist());
        assertThat(storedTree(treeIdA).getLocation().getSource()).as("stare klienty - zrodlo nieznane").isNull();
    }

    @Test
    void changedMeasurementOfTheSamePointIsAChange() throws Exception {
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"accuracyM\":12,\"source\":\"DEVICE_GNSS\"}")
                .andExpect(status().isOk());
        Instant firstUpdate = storedTree(treeIdA).getUpdatedAt();
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"accuracyM\":12,\"source\":\"DEVICE_GNSS\"}")
                .andExpect(status().isOk());
        assertThat(storedTree(treeIdA).getUpdatedAt()).as("ten sam pomiar - bez zmiany").isEqualTo(firstUpdate);
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"accuracyM\":3,\"source\":\"DEVICE_GNSS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.accuracyM").value(3));
        assertThat(storedTree(treeIdA).getUpdatedAt()).isAfter(firstUpdate);
    }

    @Test
    void invalidMeasurementIsRejectedWithFieldMessages() throws Exception {
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"accuracyM\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accuracyM").value("Accuracy must be between 0 and 10000 m"));
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"altitudeM\":99999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.altitudeM").value("Altitude must be between -500 and 9000 m"));
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"source\":\"IMPORT\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.source").value("Unsupported location source"));
        putLocation(userAccountA, treeIdA, "{\"latitude\":52,\"longitude\":21,\"source\":\"<script>\"}")
                .andExpect(status().isBadRequest());
        putLocation(userAccountA, treeIdA, "{\"latitude\":\"pięćdziesiąt\",\"longitude\":21}")
                .andExpect(status().isBadRequest());
        assertThat(storedTree(treeIdA).getLocation()).isNull();
    }

    @Test
    void otherUserAndAnonymousCannotSetGnssLocation() throws Exception {
        putLocation(userAccountB, treeIdA, "{\"latitude\":52,\"longitude\":21,\"source\":\"DEVICE_GNSS\"}")
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52,\"longitude\":21}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
        assertThat(storedTree(treeIdA).getLocation()).isNull();
    }

    /** Kolejnosc wysylana przez synchronizacje offline: nowe drzewo, od razu jego lokalizacja, edycja i praca. */
    @Test
    void offlineSyncSequenceCreateLocateEditAndAddWorkWorks() throws Exception {
        long createdTreeId = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"OFF-1\",\"speciesPolish\":\"lipa\"}")));
        putLocation(userAccountA, createdTreeId, "{\"latitude\":51.75,\"longitude\":19.45,\"accuracyM\":6,\"source\":\"DEVICE_GNSS\"}")
                .andExpect(status().isOk());
        performAs(userAccountA, patch("/api/projects/{p}/trees/{t}", projectIdA, createdTreeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"OFF-1\",\"speciesPolish\":\"lipa drobnolistna\",\"heightM\":14.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.accuracyM").value(6));
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, createdTreeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kontrola za 12 miesięcy\"}"))
                .andExpect(status().isCreated());

        Tree tree = storedTree(createdTreeId);
        assertThat(tree.getSpeciesPolish()).isEqualTo("lipa drobnolistna");
        assertThat(tree.getHeightM()).isEqualByComparingTo("14.5");
        assertThat(tree.getLocation().getSource()).isEqualTo(TreeLocationSource.DEVICE_GNSS);
        assertThat(treeWorkRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(createdTreeId)).hasSize(1);
    }
}
