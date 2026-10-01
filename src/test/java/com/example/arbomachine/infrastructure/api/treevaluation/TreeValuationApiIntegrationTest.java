package com.example.arbomachine.infrastructure.api.treevaluation;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Waloryzacja drzew (ETAP 27): zapis oceny arborysty (bez algorytmu i kwot), wlasnosc Project -> Tree -> Valuation,
 * lista projektu, eksport i dokument PDF. Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treevaluationapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-valuation-api-test-secret"
})
@AutoConfigureMockMvc
class TreeValuationApiIntegrationTest {

    private static final String FULL_VALUATION = """
            {"overallCategory":"HIGH","ageValue":"  Drzewo ok. 150-letnie  ","naturalValue":"Okazały egzemplarz rodzimego gatunku",
             "ecologicalValue":"Dziuple - siedlisko ptaków","culturalValue":"","landscapeValue":"Dominanta widoczna z alei",
             "justification":"Wiek i rola w krajobrazie","notes":"Do potwierdzenia w dokumentach parku"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeValuationRepository treeValuationRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeIdA1;
    private long treeIdA2;
    private long projectIdB;
    private long treeIdB1;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("valuation.user.a@example.com");
        userAccountB = saveConfirmedArborist("valuation.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        treeIdA1 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"A-1\",\"speciesPolish\":\"dąb szypułkowy\"}")));
        treeIdA2 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"A-2\"}")));
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park B\"}")));
        treeIdB1 = readId(performAs(userAccountB, post("/api/projects/{p}/trees", projectIdB).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"B-1\"}")));
    }

    @AfterEach
    void deleteAllData() {
        treeValuationRepository.deleteAll();
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

    private ResultActions putValuation(UserAccount userAccount, long projectId, long treeId, String body) throws Exception {
        return performAs(userAccount, put("/api/projects/{p}/trees/{t}/valuation", projectId, treeId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void treeWithoutValuationReturnsEmptyValuation() throws Exception {
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treeId").value(treeIdA1))
                .andExpect(jsonPath("$.hasValuation").value(false))
                .andExpect(jsonPath("$.overallCategory").doesNotExist());
    }

    @Test
    void valuationIsSavedTrimmedReadAndReplacedWithoutAnyCalculatedValue() throws Exception {
        String saved = putValuation(userAccountA, projectIdA, treeIdA1, FULL_VALUATION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasValuation").value(true))
                .andExpect(jsonPath("$.overallCategory").value("HIGH"))
                .andExpect(jsonPath("$.ageValue").value("Drzewo ok. 150-letnie"))
                .andExpect(jsonPath("$.culturalValue").doesNotExist())
                .andExpect(jsonPath("$.landscapeValue").value("Dominanta widoczna z alei"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(saved).doesNotContainIgnoringCase("pln").doesNotContain("calculatedValue").doesNotContain("monetary");

        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1))
                .andExpect(jsonPath("$.naturalValue").value("Okazały egzemplarz rodzimego gatunku"))
                .andExpect(jsonPath("$.justification").value("Wiek i rola w krajobrazie"));

        Instant firstUpdate = treeValuationRepository.findByTreeId(treeIdA1).orElseThrow().getUpdatedAt();
        putValuation(userAccountA, projectIdA, treeIdA1, FULL_VALUATION).andExpect(status().isOk());
        assertThat(treeValuationRepository.findByTreeId(treeIdA1).orElseThrow().getUpdatedAt()).as("bez zmian").isEqualTo(firstUpdate);

        putValuation(userAccountA, projectIdA, treeIdA1, "{\"landscapeValue\":\"Element szpaleru\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallCategory").doesNotExist())
                .andExpect(jsonPath("$.ageValue").doesNotExist())
                .andExpect(jsonPath("$.landscapeValue").value("Element szpaleru"));
        assertThat(treeValuationRepository.findAll()).as("nadal jedna waloryzacja drzewa (1:1)").hasSize(1);
    }

    @Test
    void invalidOrEmptyValuationIsRejected() throws Exception {
        putValuation(userAccountA, projectIdA, treeIdA1, "{\"overallCategory\":\"EXCELLENT\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.overallCategory").value("Unsupported valuation category"));
        putValuation(userAccountA, projectIdA, treeIdA1, "{\"notes\":\"" + "x".repeat(5001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.notes").value("Valuation text must have at most 5000 characters"));
        String emptyResponse = putValuation(userAccountA, projectIdA, treeIdA1, "{\"ageValue\":\"   \"}")
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(emptyResponse).isEqualTo("Valuation must contain at least one value");
        assertThat(treeValuationRepository.findAll()).isEmpty();
    }

    @Test
    void valuationCanBeDeletedIdempotently() throws Exception {
        putValuation(userAccountA, projectIdA, treeIdA1, FULL_VALUATION).andExpect(status().isOk());
        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1)).andExpect(status().isNoContent());
        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1)).andExpect(status().isNoContent());
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1))
                .andExpect(jsonPath("$.hasValuation").value(false));
    }

    @Test
    void projectValuationsListOnlyValuedTreesOfOwnProject() throws Exception {
        putValuation(userAccountA, projectIdA, treeIdA1, "{\"overallCategory\":\"VERY_HIGH\"}").andExpect(status().isOk());
        putValuation(userAccountB, projectIdB, treeIdB1, "{\"overallCategory\":\"LOW\"}").andExpect(status().isOk());
        JsonNode valuations = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/valuations", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(valuations).hasSize(1);
        assertThat(valuations.get(0).get("treeId").asLong()).isEqualTo(treeIdA1);
        assertThat(valuations.get(0).get("overallCategory").asText()).isEqualTo("VERY_HIGH");
    }

    @Test
    void otherUsersForeignTreesAndMissingProjectsAreNotReachable() throws Exception {
        putValuation(userAccountA, projectIdA, treeIdA1, FULL_VALUATION).andExpect(status().isOk());
        performAs(userAccountB, get("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1)).andExpect(status().isNotFound());
        putValuation(userAccountB, projectIdA, treeIdA1, "{\"overallCategory\":\"LOW\"}").andExpect(status().isNotFound());
        performAs(userAccountB, delete("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1)).andExpect(status().isNotFound());
        performAs(userAccountB, get("/api/projects/{p}/valuations", projectIdA)).andExpect(status().isNotFound());
        // drzewo B podane w URL projektu A (IDOR przez podmiane id drzewa)
        putValuation(userAccountA, projectIdA, treeIdB1, "{\"overallCategory\":\"LOW\"}").andExpect(status().isNotFound());
        performAs(userAccountA, get("/api/projects/{p}/valuations", projectIdA + 1000)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{p}/trees/{t}/valuation", projectIdA, treeIdA1))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
        assertThat(treeValuationRepository.findByTreeId(treeIdA1).orElseThrow().getOverallCategory().name()).isEqualTo("HIGH");
        assertThat(treeValuationRepository.findByTreeId(treeIdB1)).isEmpty();
    }

    @Test
    void valuationAppearsInExportsAndProjectDocument() throws Exception {
        putValuation(userAccountA, projectIdA, treeIdA1, FULL_VALUATION).andExpect(status().isOk());

        String csv = performAs(userAccountA, get("/api/projects/{p}/exports/trees.csv", projectIdA))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Waloryzacja - ocena ogólna", "Wartość przyrodnicza", "Uzasadnienie waloryzacji",
                "HIGH", "Okazały egzemplarz rodzimego gatunku");
        JsonNode geoJson = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(geoJson.get("features")).isEmpty(); // drzewa bez lokalizacji
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA1)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52,\"longitude\":21}")).andExpect(status().isOk());
        JsonNode properties = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andReturn().getResponse().getContentAsByteArray()).at("/features/0/properties");
        assertThat(properties.get("valuationCategory").asText()).isEqualTo("HIGH");
        assertThat(properties.has("valuationNaturalValue")).as("dlugie opisy nie trafiaja do GIS").isFalse();

        byte[] pdf = performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        }
        assertThat(text).contains("Waloryzacja", "Ocena ogólna", "Wysoka", "Wartość przyrodnicza", "Okazały egzemplarz rodzimego gatunku",
                "Nie przeprowadzono waloryzacji.");
        assertThat(text.replaceAll("\\s+", " ")).contains("Drzewa z waloryzacją 1");
    }
}
