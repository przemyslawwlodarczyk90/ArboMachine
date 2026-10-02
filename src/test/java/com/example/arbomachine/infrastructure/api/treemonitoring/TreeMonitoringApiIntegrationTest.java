package com.example.arbomachine.infrastructure.api.treemonitoring;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Monitoring stanu drzew (ETAP 29): historia kontroli, walidacja, wlasnosc Project -> Tree -> wpis (IDOR), ostatnia
 * kontrola projektu, eksport (CSV, arkusz "Monitoring", GeoJSON) i dokument PDF. Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treemonitoringapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-monitoring-api-test-secret",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class TreeMonitoringApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    @Autowired private UserNotificationRepository userNotificationRepository;
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
        userAccountA = saveConfirmedArborist("monitoring.user.a@example.com");
        userAccountB = saveConfirmedArborist("monitoring.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        treeIdA1 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"M-1\",\"speciesPolish\":\"dąb szypułkowy\"}")));
        treeIdA2 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"M-2\"}")));
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park B\"}")));
        treeIdB1 = readId(performAs(userAccountB, post("/api/projects/{p}/trees", projectIdB).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"B-1\"}")));
    }

    @AfterEach
    void deleteAllData() {
        userNotificationRepository.deleteAll();
        treeMonitoringRecordRepository.deleteAll();
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

    private String monitoringPath(long projectId, long treeId) {
        return "/api/projects/" + projectId + "/trees/" + treeId + "/monitoring";
    }

    private long createRecord(long treeId, String body) throws Exception {
        return readId(performAs(userAccountA, post(monitoringPath(projectIdA, treeId)).contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    private JsonNode json(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void treeWithoutRecordsHasEmptyHistory() throws Exception {
        performAs(userAccountA, get(monitoringPath(projectIdA, treeIdA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recordsAreCreatedTrimmedAndListedNewestFirst() throws Exception {
        performAs(userAccountA, post(monitoringPath(projectIdA, treeIdA1)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inspectionDate\":\"2026-05-10\",\"healthStatus\":\"GOOD\",\"notes\":\"  Bez zmian  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treeId").value(treeIdA1))
                .andExpect(jsonPath("$.inspectionDate").value("2026-05-10"))
                .andExpect(jsonPath("$.healthStatus").value("GOOD"))
                .andExpect(jsonPath("$.notes").value("Bez zmian"));
        createRecord(treeIdA1, "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"FAIR\"}");
        createRecord(treeIdA1, "{\"inspectionDate\":\"2025-04-01\",\"notes\":\"Kontrola historyczna bez oceny\"}");

        JsonNode history = json(performAs(userAccountA, get(monitoringPath(projectIdA, treeIdA1))).andExpect(status().isOk()));
        assertThat(StreamSupport.stream(history.spliterator(), false).map(record -> record.get("inspectionDate").asText()))
                .containsExactly("2026-09-20", "2026-05-10", "2025-04-01");
        assertThat(history.get(2).get("healthStatus").isNull()).as("brak oceny = nie okreslono").isTrue();
    }

    @Test
    void recordCanBeUpdatedAndDeleted() throws Exception {
        long recordId = createRecord(treeIdA1, "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"FAIR\",\"notes\":\"Posusz\"}");
        String path = monitoringPath(projectIdA, treeIdA1) + "/" + recordId;
        performAs(userAccountA, put(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inspectionDate\":\"2026-09-21\",\"healthStatus\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionDate").value("2026-09-21"))
                .andExpect(jsonPath("$.healthStatus").value("GOOD"))
                .andExpect(jsonPath("$.notes").doesNotExist());
        performAs(userAccountA, get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.healthStatus").value("GOOD"));
        performAs(userAccountA, delete(path)).andExpect(status().isNoContent());
        performAs(userAccountA, get(path)).andExpect(status().isNotFound());
    }

    @Test
    void invalidRecordsAreRejected() throws Exception {
        String path = monitoringPath(projectIdA, treeIdA1);
        performAs(userAccountA, post(path).contentType(MediaType.APPLICATION_JSON).content("{\"healthStatus\":\"GOOD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.inspectionDate").value("Inspection date is required"));
        performAs(userAccountA, post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"DANGEROUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.healthStatus").value("Unsupported health status"));
        performAs(userAccountA, post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inspectionDate\":\"2026-09-20\",\"notes\":\"" + "x".repeat(5001) + "\"}"))
                .andExpect(status().isBadRequest());
        performAs(userAccountA, post(path).contentType(MediaType.APPLICATION_JSON).content("{\"inspectionDate\":\"20.09.2026\"}"))
                .andExpect(status().isBadRequest());
        assertThat(treeMonitoringRecordRepository.findAll()).isEmpty();
    }

    @Test
    void otherUsersWrongTreeAndAnonymousCannotReachRecords() throws Exception {
        long recordId = createRecord(treeIdA1, "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"GOOD\"}");
        String path = monitoringPath(projectIdA, treeIdA1) + "/" + recordId;
        String body = "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"POOR\"}";
        performAs(userAccountB, get(monitoringPath(projectIdA, treeIdA1))).andExpect(status().isNotFound());
        performAs(userAccountB, get(path)).andExpect(status().isNotFound());
        performAs(userAccountB, put(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        performAs(userAccountB, delete(path)).andExpect(status().isNotFound());
        performAs(userAccountB, post(monitoringPath(projectIdA, treeIdA1)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        performAs(userAccountB, get("/api/projects/{p}/monitoring/latest", projectIdA)).andExpect(status().isNotFound());
        // wpis drzewa A1 przez URL drzewa A2 i przez projekt/drzewo B
        performAs(userAccountA, get(monitoringPath(projectIdA, treeIdA2) + "/" + recordId)).andExpect(status().isNotFound());
        performAs(userAccountB, put(monitoringPath(projectIdB, treeIdB1) + "/" + recordId).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isNotFound());
        performAs(userAccountA, get(monitoringPath(projectIdA, treeIdB1))).andExpect(status().isNotFound());
        performAs(userAccountA, get(monitoringPath(999_999, treeIdA1))).andExpect(status().isNotFound());
        mockMvc.perform(get(monitoringPath(projectIdA, treeIdA1))).andExpect(status().isUnauthorized());

        assertThat(json(performAs(userAccountA, get(path))).get("healthStatus").asText()).isEqualTo("GOOD");
        assertThat(userNotificationRepository.findAll()).as("proby B nie wywolaly alertu").isEmpty();
    }

    @Test
    void latestRecordOfEachTreeIsListedForProject() throws Exception {
        createRecord(treeIdA1, "{\"inspectionDate\":\"2026-05-10\",\"healthStatus\":\"GOOD\"}");
        createRecord(treeIdA1, "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"POOR\"}");
        JsonNode latest = json(performAs(userAccountA, get("/api/projects/{p}/monitoring/latest", projectIdA)).andExpect(status().isOk()));
        assertThat(latest).hasSize(1);
        assertThat(latest.get(0).get("treeId").asLong()).isEqualTo(treeIdA1);
        assertThat(latest.get(0).get("healthStatus").asText()).isEqualTo("POOR");
    }

    @Test
    void monitoringAppearsInExportsAndProjectDocument() throws Exception {
        createRecord(treeIdA1, "{\"inspectionDate\":\"2026-05-10\",\"healthStatus\":\"GOOD\",\"notes\":\"Pierwsza kontrola\"}");
        createRecord(treeIdA1, "{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"POOR\",\"notes\":\"Pęknięcie konaru\"}");
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA1)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52,\"longitude\":21}")).andExpect(status().isOk());

        String csv = performAs(userAccountA, get("/api/projects/{p}/exports/trees.csv", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(new StringReader(csv.substring(1)), CSVFormat.DEFAULT.builder().setDelimiter(';').get())) {
            records = parser.getRecords();
        }
        List<String> headers = records.getFirst().toList();
        CSVRecord treeM1 = records.get(1);
        assertThat(treeM1.get(headers.indexOf("Data ostatniej kontroli"))).isEqualTo("2026-09-20");
        assertThat(treeM1.get(headers.indexOf("Ocena stanu (ostatnia kontrola)"))).isEqualTo("POOR");
        assertThat(treeM1.get(headers.indexOf("Uwagi z ostatniej kontroli"))).isEqualTo("Pęknięcie konaru");
        assertThat(records.get(2).get(headers.indexOf("Data ostatniej kontroli"))).as("drzewo bez kontroli").isEmpty();

        byte[] xlsx = performAs(userAccountA, get("/api/projects/{p}/exports/trees.xlsx", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            List<Row> rows = workbook.findSheet("Monitoring").orElseThrow().read();
            assertThat(rows).hasSize(3);
            assertThat(rows.get(0).getCellText(4)).isEqualTo("Data kontroli");
            assertThat(rows.get(1).getCellText(1)).isEqualTo("M-1");
            assertThat(rows.get(1).getCellText(4)).isEqualTo("2026-09-20");
            assertThat(rows.get(1).getCellText(5)).isEqualTo("POOR");
        }

        JsonNode properties = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andReturn().getResponse().getContentAsByteArray()).at("/features/0/properties");
        assertThat(properties.get("lastMonitoringDate").asText()).isEqualTo("2026-09-20");
        assertThat(properties.get("lastHealthStatus").asText()).isEqualTo("POOR");
        assertThat(properties.has("lastMonitoringNotes")).as("dlugie uwagi nie trafiaja do GIS").isFalse();

        byte[] pdf = performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        }
        String flatText = text.replaceAll("\\s+", " ");
        assertThat(flatText).contains("Monitoring stanu", "20.09.2026", "ocena stanu: Zły", "Pęknięcie konaru", "ocena stanu: Dobry",
                "Brak przeprowadzonego monitoringu.", "Drzewa z kontrolą stanu (monitoring) 1",
                "Drzewa w złym stanie (wg ostatniej kontroli) 1");
    }
}
