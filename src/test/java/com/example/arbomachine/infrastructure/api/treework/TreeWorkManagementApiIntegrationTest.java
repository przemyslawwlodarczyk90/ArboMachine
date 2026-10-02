package com.example.arbomachine.infrastructure.api.treework;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gospodarka drzewostanem (ETAP 28) na TreeWork: status, priorytet, terminy, filtry listy projektu, wlasnosc
 * Project -> Tree -> TreeWork, eksport (arkusz "Prace", CSV prac, GeoJSON) i dokument PDF. Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treeworkmanagementapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-work-management-api-test-secret"
})
@AutoConfigureMockMvc
class TreeWorkManagementApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
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
        userAccountA = saveConfirmedArborist("management.user.a@example.com");
        userAccountB = saveConfirmedArborist("management.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        treeIdA1 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"G-1\",\"speciesPolish\":\"lipa drobnolistna\"}")));
        treeIdA2 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"G-2\"}")));
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park B\"}")));
        treeIdB1 = readId(performAs(userAccountB, post("/api/projects/{p}/trees", projectIdB).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"B-1\"}")));
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

    private long createWork(long treeId, String body) throws Exception {
        return readId(performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeId)
                .contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    private JsonNode projectWorks(String query) throws Exception {
        return objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/works" + query, projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static List<String> namesOf(JsonNode works) {
        return StreamSupport.stream(works.spliterator(), false).map(work -> work.get("name").asText()).toList();
    }

    @Test
    void workIsCreatedWithStatusPriorityAndDatesAndDefaultsToRecommended() throws Exception {
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cięcia sanitarne\",\"description\":\"Posusz nad chodnikiem\",\"status\":\"PLANNED\","
                        + "\"priority\":\"URGENT\",\"plannedDate\":\"2026-11-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.plannedDate").value("2026-11-15"))
                .andExpect(jsonPath("$.completedDate").doesNotExist());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kontrola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECOMMENDED"))
                .andExpect(jsonPath("$.priority").doesNotExist());
        assertThat(projectWorks("")).as("wiele prac na jednym drzewie").hasSize(2);
    }

    @Test
    void workFromBeforeStage28WithoutStatusIsReadAsRecommended() throws Exception {
        long workId = createWork(treeIdA1, "{\"name\":\"Stara praca\"}");
        jdbcTemplate.update("update " + tableName() + " set status = null where id = ?", workId);
        performAs(userAccountA, get("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA1, workId))
                .andExpect(jsonPath("$.status").value("RECOMMENDED"));
    }

    private String tableName() {
        return jdbcTemplate.queryForObject(
                "select table_schema || '.' || table_name from information_schema.tables where upper(table_name) = 'TREE_WORK'", String.class);
    }

    @Test
    void anyStatusTransitionAndHistoricalDatesAreAllowed() throws Exception {
        long workId = createWork(treeIdA1, "{\"name\":\"Wycinka posuszu\",\"status\":\"PLANNED\",\"plannedDate\":\"2019-05-01\"}");
        String path = "/api/projects/{p}/trees/{t}/works/{w}";
        performAs(userAccountA, patch(path, projectIdA, treeIdA1, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Wycinka posuszu\",\"status\":\"COMPLETED\",\"plannedDate\":\"2019-05-01\",\"completedDate\":\"2019-05-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedDate").value("2019-05-20"));
        performAs(userAccountA, patch(path, projectIdA, treeIdA1, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Wycinka posuszu\",\"status\":\"PLANNED\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.plannedDate").doesNotExist());
        performAs(userAccountA, patch(path, projectIdA, treeIdA1, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Wycinka posuszu\",\"status\":\"CANCELLED\"}"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void invalidStatusPriorityAndDateAreRejected() throws Exception {
        String path = "/api/projects/{p}/trees/{t}/works";
        performAs(userAccountA, post(path, projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("Unsupported work status"));
        performAs(userAccountA, post(path, projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"priority\":\"CRITICAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.priority").value("Unsupported work priority"));
        performAs(userAccountA, post(path, projectIdA, treeIdA1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"plannedDate\":\"15.11.2026\"}"))
                .andExpect(status().isBadRequest());
        assertThat(treeWorkRepository.findAll()).isEmpty();
    }

    @Test
    void projectWorksCanBeFilteredByStatusAndPriority() throws Exception {
        createWork(treeIdA1, "{\"name\":\"Zalecana\",\"status\":\"RECOMMENDED\"}");
        createWork(treeIdA1, "{\"name\":\"Zaplanowana pilna\",\"status\":\"PLANNED\",\"priority\":\"URGENT\"}");
        createWork(treeIdA2, "{\"name\":\"Wykonana\",\"status\":\"COMPLETED\",\"priority\":\"URGENT\"}");
        createWork(treeIdA2, "{\"name\":\"Anulowana\",\"status\":\"CANCELLED\"}");
        assertThat(namesOf(projectWorks(""))).hasSize(4);
        assertThat(namesOf(projectWorks("?status=PLANNED&status=RECOMMENDED"))).containsExactly("Zalecana", "Zaplanowana pilna");
        assertThat(namesOf(projectWorks("?priority=URGENT"))).containsExactly("Zaplanowana pilna", "Wykonana");
        assertThat(namesOf(projectWorks("?status=COMPLETED&priority=URGENT"))).containsExactly("Wykonana");
        performAs(userAccountA, get("/api/projects/{p}/works?status=DONE", projectIdA)).andExpect(status().isBadRequest());
    }

    @Test
    void workCanBeDeleted() throws Exception {
        long workId = createWork(treeIdA1, "{\"name\":\"Do usunięcia\",\"status\":\"PLANNED\"}");
        performAs(userAccountA, delete("/api/projects/{p}/trees/{t}/works/{w}", projectIdA, treeIdA1, workId)).andExpect(status().isNoContent());
        assertThat(projectWorks("")).isEmpty();
    }

    @Test
    void otherUsersAndWrongTreeCannotReachWork() throws Exception {
        long workId = createWork(treeIdA1, "{\"name\":\"Praca A\",\"status\":\"PLANNED\"}");
        String path = "/api/projects/{p}/trees/{t}/works/{w}";
        performAs(userAccountB, get(path, projectIdA, treeIdA1, workId)).andExpect(status().isNotFound());
        performAs(userAccountB, patch(path, projectIdA, treeIdA1, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Przejęta\",\"status\":\"CANCELLED\"}")).andExpect(status().isNotFound());
        performAs(userAccountB, delete(path, projectIdA, treeIdA1, workId)).andExpect(status().isNotFound());
        performAs(userAccountB, get("/api/projects/{p}/works", projectIdA)).andExpect(status().isNotFound());
        // praca drzewa A1 przez URL drzewa A2 i przez projekt/drzewo B
        performAs(userAccountA, patch(path, projectIdA, treeIdA2, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Zła ścieżka\"}")).andExpect(status().isNotFound());
        performAs(userAccountB, patch(path, projectIdB, treeIdB1, workId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Zła ścieżka\"}")).andExpect(status().isNotFound());
        JsonNode work = objectMapper.readTree(performAs(userAccountA, get(path, projectIdA, treeIdA1, workId))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(work.get("name").asText()).isEqualTo("Praca A");
        assertThat(work.get("status").asText()).isEqualTo("PLANNED");
    }

    @Test
    void calendarExportContainsOpenWorksWithDatesOfOwnProjectOnly() throws Exception {
        createWork(treeIdA1, "{\"name\":\"Cięcia sanitarne\",\"status\":\"PLANNED\",\"plannedDate\":\"2026-11-15\"}");
        createWork(treeIdA2, "{\"name\":\"Bez terminu\",\"status\":\"PLANNED\"}");
        var response = performAs(userAccountA, get("/api/projects/{p}/exports/calendar.ics", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentType()).startsWith("text/calendar");
        assertThat(response.getHeader("Content-Disposition")).contains(".ics");
        String ics = response.getContentAsString(StandardCharsets.UTF_8).replace("\r\n ", "");
        assertThat(ics).contains("BEGIN:VCALENDAR", "DTSTART;VALUE=DATE:20261115", "SUMMARY:Cięcia sanitarne – drzewo G-1")
                .doesNotContain("Bez terminu");
        performAs(userAccountB, get("/api/projects/{p}/exports/calendar.ics", projectIdA)).andExpect(status().isNotFound());
    }

    @Test
    void managementDataAppearsInExportsAndProjectDocument() throws Exception {
        createWork(treeIdA1, "{\"name\":\"Cięcia sanitarne\",\"description\":\"Posusz\",\"status\":\"PLANNED\",\"priority\":\"URGENT\",\"plannedDate\":\"2026-11-15\"}");
        createWork(treeIdA1, "{\"name\":\"Kontrola\",\"status\":\"RECOMMENDED\",\"plannedDate\":\"2027-04-01\"}");
        createWork(treeIdA2, "{\"name\":\"Wiązanie korony\",\"status\":\"COMPLETED\",\"completedDate\":\"2026-09-30\"}");
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeIdA1)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52,\"longitude\":21}")).andExpect(status().isOk());

        String worksCsv = performAs(userAccountA, get("/api/projects/{p}/exports/works.csv", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(new StringReader(worksCsv.substring(1)), CSVFormat.DEFAULT.builder().setDelimiter(';').get())) {
            records = parser.getRecords();
        }
        assertThat(records.getFirst().toList()).containsExactly("ID drzewa (ArboMachine)", "Numer inwentaryzacyjny", "Gatunek polski",
                "Gatunek łaciński", "ID pracy (ArboMachine)", "Praca", "Opis / uzasadnienie", "Status", "Priorytet", "Termin",
                "Data wykonania", "Symbol na mapie");
        assertThat(records).hasSize(4);
        assertThat(records.get(1).toList()).contains("G-1", "lipa drobnolistna", "Cięcia sanitarne", "PLANNED", "URGENT", "2026-11-15");
        assertThat(records.get(3).toList()).contains("G-2", "Wiązanie korony", "COMPLETED", "2026-09-30");

        byte[] xlsx = performAs(userAccountA, get("/api/projects/{p}/exports/trees.xlsx", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            List<Row> workRows = workbook.findSheet("Prace").orElseThrow().read();
            assertThat(workRows).hasSize(4);
            assertThat(workRows.get(1).getCellText(5)).isEqualTo("Cięcia sanitarne");
            assertThat(workRows.get(1).getCellText(7)).isEqualTo("PLANNED");
        }

        JsonNode properties = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andReturn().getResponse().getContentAsByteArray()).at("/features/0/properties");
        assertThat(properties.get("openWorkCount").asInt()).isEqualTo(2);
        assertThat(properties.get("nextPlannedWorkDate").asText()).isEqualTo("2026-11-15");
        assertThat(properties.at("/works/0/status").asText()).isEqualTo("PLANNED");
        assertThat(properties.at("/works/0/priority").asText()).isEqualTo("URGENT");

        byte[] pdf = performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        }
        assertThat(text).contains("Gospodarka drzewostanem", "Zalecana praca", "Cięcia sanitarne", "Zaplanowana", "Pilny",
                "15.11.2026", "Wykonana", "30.09.2026", "Zalecenie");
        assertThat(text.replaceAll("\\s+", " ")).contains("Prace do wykonania (zalecone lub zaplanowane) 2", "Prace wykonane 1");
    }
}
