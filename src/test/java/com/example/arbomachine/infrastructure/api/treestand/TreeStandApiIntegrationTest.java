package com.example.arbomachine.infrastructure.api.treestand;

import com.example.arbomachine.domain.dataexchange.gis.GisFeature;
import com.example.arbomachine.domain.dataexchange.gis.GisLayer;
import com.example.arbomachine.domain.dataexchange.gis.ShapefileZipReader;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.sf.GeometryType;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Grupy drzew (ETAP 29): CRUD przez Project, geometria (walidacja, pole), przypisanie drzew (tylko z projektu, "w obszarze"),
 * IDOR, eksport (GeoJSON, GeoPackage "stands", Shapefile ZIP, XLSX), dokument PDF i dane zlecenia projektu.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treestandapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-stand-api-test-secret",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class TreeStandApiIntegrationTest {

    /** Kwadrat ok. 70 x 111 m wokol drzew S-1 i S-2 (S-3 poza nim). */
    private static final String SQUARE = "{\"type\":\"Polygon\",\"coordinates\":[[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.409],[21.149,50.408]]]}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeStandRepository treeStandRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long tree1;
    private long tree2;
    private long tree3;
    private long projectIdB;
    private long treeB1;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("stand.user.a@example.com");
        userAccountB = saveConfirmedArborist("stand.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Park A\",\"clientName\":\"Gmina Lubnice\",\"siteAddress\":\"Park przy szkole\",\"orderDate\":\"2026-09-01\"}")));
        tree1 = createTree(userAccountA, projectIdA, "{\"inventoryNumber\":\"S-1\",\"speciesLatin\":\"Pinus sylvestris\"}", 50.4085, 21.1495);
        tree2 = createTree(userAccountA, projectIdA, "{\"inventoryNumber\":\"S-2\",\"speciesPolish\":\"lipa drobnolistna\"}", 50.4082, 21.1492);
        tree3 = createTree(userAccountA, projectIdA, "{\"inventoryNumber\":\"S-3\"}", 50.4200, 21.1600);
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park B\"}")));
        treeB1 = createTree(userAccountB, projectIdB, "{\"inventoryNumber\":\"B-1\"}", 50.4085, 21.1495);
    }

    @AfterEach
    void deleteAllData() {
        treeStandRepository.deleteAll();
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
        return mockMvc.perform(request.header("Authorization", "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail())));
    }

    private long readId(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTree(UserAccount owner, long projectId, String body, double latitude, double longitude) throws Exception {
        long treeId = readId(performAs(owner, post("/api/projects/{p}/trees", projectId).contentType(MediaType.APPLICATION_JSON).content(body)));
        performAs(owner, put("/api/projects/{p}/trees/{t}/location", projectId, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}")).andExpect(status().isOk());
        return treeId;
    }

    private String standBody(String name, String geometry, String treeIds, boolean assignInside) {
        return "{\"name\":\"" + name + "\",\"notes\":\"  Skupisko przy alei  \",\"geometry\":" + geometry + ",\"treeIds\":" + treeIds
                + ",\"assignTreesInside\":" + assignInside + "}";
    }

    private JsonNode json(ResultActions resultActions) throws Exception {
        return objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long createStand(String body) throws Exception {
        return readId(performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    @Test
    void projectStoresClientFieldsAndClearsThemWithEmptyText() throws Exception {
        performAs(userAccountA, get("/api/projects/{p}", projectIdA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Gmina Lubnice"))
                .andExpect(jsonPath("$.siteAddress").value("Park przy szkole"))
                .andExpect(jsonPath("$.orderDate").value("2026-09-01"));
        performAs(userAccountA, patch("/api/projects/{p}", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientName\":\"\",\"orderDate\":\"\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").doesNotExist())
                .andExpect(jsonPath("$.orderDate").doesNotExist())
                .andExpect(jsonPath("$.siteAddress").value("Park przy szkole"));
        performAs(userAccountA, patch("/api/projects/{p}", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderDate\":\"01.09.2026\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void standIsCreatedWithAreaExplicitTreesAndTreesInside() throws Exception {
        JsonNode created = json(performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content(standBody("Aleja lipowa", SQUARE, "[" + tree3 + "]", true))).andExpect(status().isCreated()));
        assertThat(created.get("name").asText()).isEqualTo("Aleja lipowa");
        assertThat(created.get("notes").asText()).isEqualTo("Skupisko przy alei");
        assertThat(created.get("source").asText()).isEqualTo("MANUAL");
        assertThat(created.get("areaSquareMeters").asDouble()).isBetween(7800.0, 8000.0);
        assertThat(StreamSupport.stream(created.get("treeIds").spliterator(), false).map(JsonNode::asLong).toList())
                .containsExactlyInAnyOrder(tree1, tree2, tree3);
        assertThat(created.at("/geometry/type").asText()).isEqualTo("Polygon");

        JsonNode list = json(performAs(userAccountA, get("/api/projects/{p}/stands", projectIdA)).andExpect(status().isOk()));
        assertThat(list).hasSize(1);
    }

    @Test
    void standCanBeUpdatedAssignedAndDeletedWithoutDeletingTrees() throws Exception {
        long standId = createStand(standBody("Grupa", SQUARE, "[]", false));
        String path = "/api/projects/" + projectIdA + "/stands/" + standId;
        performAs(userAccountA, put(path).contentType(MediaType.APPLICATION_JSON).content(standBody("Szpaler", SQUARE, "[" + tree3 + "]", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Szpaler"))
                .andExpect(jsonPath("$.treeIds.length()").value(1));
        performAs(userAccountA, post(path + "/assign-trees-inside")).andExpect(status().isOk())
                .andExpect(jsonPath("$.treeIds.length()").value(3));
        performAs(userAccountA, delete(path)).andExpect(status().isNoContent());
        performAs(userAccountA, get(path)).andExpect(status().isNotFound());
        assertThat(treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(projectIdA)).hasSize(3);
    }

    @Test
    void invalidGeometryAndForeignTreesAreRejected() throws Exception {
        for (String geometry : List.of("{\"type\":\"Point\",\"coordinates\":[21,50]}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[7510595,5585827],[7510600,5585827],[7510600,5585830],[7510595,5585827]]]}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[21,50],[21.1,50],[21,50]]]}")) {
            performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                    .content(standBody("Zla", geometry, "[]", false))).andExpect(status().isBadRequest());
        }
        performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content(standBody("Cudze drzewo", SQUARE, "[" + treeB1 + "]", false))).andExpect(status().isBadRequest());
        performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"geometry\":" + SQUARE + "}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Stand name is required"));
        assertThat(treeStandRepository.findAll()).isEmpty();
    }

    @Test
    void otherUsersCannotReachStands() throws Exception {
        long standId = createStand(standBody("Grupa A", SQUARE, "[]", true));
        String path = "/api/projects/" + projectIdA + "/stands/" + standId;
        performAs(userAccountB, get("/api/projects/{p}/stands", projectIdA)).andExpect(status().isNotFound());
        performAs(userAccountB, get(path)).andExpect(status().isNotFound());
        performAs(userAccountB, put(path).contentType(MediaType.APPLICATION_JSON).content(standBody("Przejeta", SQUARE, "[]", false)))
                .andExpect(status().isNotFound());
        performAs(userAccountB, delete(path)).andExpect(status().isNotFound());
        performAs(userAccountB, post(path + "/assign-trees-inside")).andExpect(status().isNotFound());
        performAs(userAccountB, get("/api/projects/{p}/stands/{s}", projectIdB, standId)).andExpect(status().isNotFound());
        performAs(userAccountB, get("/api/projects/{p}/exports/shapefile.zip", projectIdA)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{p}/stands", projectIdA)).andExpect(status().isUnauthorized());
        assertThat(json(performAs(userAccountA, get(path))).get("name").asText()).isEqualTo("Grupa A");
    }

    @Test
    void standsAndSpeciesGroupAppearInGisExportsSpreadsheetAndDocument() throws Exception {
        createStand(standBody("Aleja lipowa", SQUARE, "[]", true));

        JsonNode standsGeoJson = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/stands.geojson", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertThat(standsGeoJson.at("/features/0/geometry/type").asText()).isEqualTo("Polygon");
        assertThat(standsGeoJson.at("/features/0/properties/name").asText()).isEqualTo("Aleja lipowa");
        assertThat(standsGeoJson.at("/features/0/properties/treeCount").asInt()).isEqualTo(2);

        JsonNode treesGeoJson = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andReturn().getResponse().getContentAsByteArray());
        JsonNode s1 = StreamSupport.stream(treesGeoJson.get("features").spliterator(), false)
                .filter(feature -> feature.at("/properties/inventoryNumber").asText().equals("S-1")).findFirst().orElseThrow();
        assertThat(s1.at("/properties/speciesGroup").asText()).as("Pinus = iglaste").isEqualTo("CONIFER");
        assertThat(s1.at("/properties/standNames").asText()).isEqualTo("Aleja lipowa");

        byte[] shapefileZip = performAs(userAccountA, get("/api/projects/{p}/exports/shapefile.zip", projectIdA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        List<GisLayer> layers = ShapefileZipReader.read(shapefileZip);
        GisLayer trees = layers.stream().filter(layer -> layer.name().equals("trees")).findFirst().orElseThrow();
        GisLayer stands = layers.stream().filter(layer -> layer.name().equals("stands")).findFirst().orElseThrow();
        assertThat(trees.features()).hasSize(3);
        assertThat(trees.crs()).isPresent();
        assertThat(trees.features().getFirst().attributes()).containsEntry("inv_no", "S-1").containsEntry("sp_group", "CONIFER");
        GisFeature.Point point = (GisFeature.Point) trees.features().getFirst().geometry();
        assertThat(point.x()).isEqualTo(21.1495);
        assertThat(point.y()).isEqualTo(50.4085);
        assertThat(stands.features()).hasSize(1);
        assertThat(stands.features().getFirst().geometry()).isInstanceOf(GisFeature.Polygon.class);
        assertThat(stands.features().getFirst().attributes()).containsEntry("name", "Aleja lipowa").containsEntry("tree_count", "2");

        byte[] geoPackageBytes = performAs(userAccountA, get("/api/projects/{p}/exports/trees.gpkg", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        Path geoPackageFile = Files.createTempFile("stands-test-", ".gpkg");
        try {
            Files.write(geoPackageFile, geoPackageBytes);
            try (GeoPackage geoPackage = GeoPackageManager.open(geoPackageFile.toFile(), false)) {
                FeatureDao standsDao = geoPackage.getFeatureDao("stands");
                assertThat(standsDao.getGeometryType()).isEqualTo(GeometryType.POLYGON);
                assertThat(standsDao.count()).isEqualTo(1);
            }
        } finally {
            Files.deleteIfExists(geoPackageFile);
        }

        byte[] xlsx = performAs(userAccountA, get("/api/projects/{p}/exports/trees.xlsx", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            List<Row> standRows = workbook.findSheet("Grupy drzew").orElseThrow().read();
            assertThat(standRows).hasSize(2);
            assertThat(standRows.get(1).getCellText(1)).isEqualTo("Aleja lipowa");
            assertThat(standRows.get(1).getCellText(4)).isEqualTo("S-1, S-2");
            List<Row> projectRows = workbook.findSheet("Projekt").orElseThrow().read();
            assertThat(projectRows.stream().map(row -> row.getCellText(1))).contains("Gmina Lubnice", "Park przy szkole", "2026-09-01");
        }

        byte[] pdf = performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
        assertThat(text).contains("Grupy drzew", "Aleja lipowa", "S-1, S-2", "Gmina Lubnice", "Park przy szkole", "01.09.2026");
    }
}
