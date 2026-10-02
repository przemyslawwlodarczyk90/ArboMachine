package com.example.arbomachine.infrastructure.api.dataexchange;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import com.example.arbomachine.domain.dataexchange.gis.ProjectionFileReader;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import GIS (ETAP 29): DXF w PL-2000 (numery z opisow, obszary), arkusz z aplikacji terenowej (aliasy, karta oceny,
 * obszary pominiete, obwody wielopniowe), Shapefile i GeoPackage (petla eksport -> import), grupy drzew z GeoJSON,
 * pliki bledne i wlasnosc projektu.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gisimportapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=gis-import-api-test-secret",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class GisImportApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeStandRepository treeStandRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long projectIdA2;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("gis.user.a@example.com");
        userAccountB = saveConfirmedArborist("gis.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Lubnice\"}")));
        projectIdA2 = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Kopia\"}")));
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

    private JsonNode upload(UserAccount userAccount, long projectId, String path, String fileName, byte[] content, int expectedStatus) throws Exception {
        String body = performAs(userAccount, multipart("/api/projects/{p}/imports/" + path, projectId)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return body.isBlank() || expectedStatus == 404 ? null : objectMapper.readTree(body);
    }

    private static List<String> codesOf(JsonNode issues) {
        return StreamSupport.stream(issues.spliterator(), false).map(issue -> issue.get("code").asText()).toList();
    }

    private List<Tree> treesOf(long projectId) {
        return treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(projectId);
    }

    /** Rysunek w PL-2000 strefa 7: dwa drzewa (jedno z odnosnikiem do opisu), krzew (pomijany), obszar wokol drzewa 1. */
    private static byte[] dxfDrawing() {
        String[] pairs = {
                "0", "SECTION", "2", "HEADER", "9", "$ACADVER", "1", "AC1021", "0", "ENDSEC",
                "0", "SECTION", "2", "ENTITIES",
                "0", "CIRCLE", "8", "Inw-Deciduous-Point", "10", "7510595.0306", "20", "5585827.1674", "30", "0.0", "40", "0.1894",
                "0", "CIRCLE", "8", "Inw-Deciduous", "10", "7510595.0306", "20", "5585827.1674", "30", "0.0", "40", "3.0",
                "0", "LWPOLYLINE", "8", "Inw-Label", "90", "3", "10", "7510595.0306", "20", "5585827.1674",
                "10", "7510598.0306", "20", "5585830.1674", "10", "7510598.5306", "20", "5585830.1674",
                "0", "TEXT", "8", "Inw-Label", "10", "7510598.5306", "20", "5585829.6674", "40", "1.0", "1", "1",
                "0", "CIRCLE", "8", "Inw-Coniferous-Point", "10", "7510640.0", "20", "5585840.0", "30", "0.0", "40", "0.1",
                "0", "TEXT", "8", "Inw-Label", "10", "7510643.0", "20", "5585842.0", "40", "1.0", "1", "2a",
                "0", "CIRCLE", "8", "Inw-Shrub", "10", "7510700.0", "20", "5585900.0", "30", "0.0", "40", "0.5",
                "0", "LWPOLYLINE", "8", "Inw-Area", "90", "4", "70", "1",
                "10", "7510585.0", "20", "5585817.0", "10", "7510605.0", "20", "5585817.0",
                "10", "7510605.0", "20", "5585837.0", "10", "7510585.0", "20", "5585837.0",
                "0", "ENDSEC", "0", "EOF"};
        return String.join("\r\n", pairs).concat("\r\n").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void dxfInPolishGridImportsTreesWithLabelsAndAreasAsStands() throws Exception {
        JsonNode validation = upload(userAccountA, projectIdA, "trees/validate", "lubnice.dxf", dxfDrawing(), 200);
        assertThat(validation.get("validRowCount").asInt()).isEqualTo(2);
        assertThat(codesOf(validation.get("warnings"))).contains("COORDINATES_CONVERTED");
        assertThat(validation.get("warnings").get(0).get("column").asText()).isEqualTo("EPSG:2178");
        upload(userAccountA, projectIdA, "trees", "lubnice.dxf", dxfDrawing(), 201);

        List<Tree> trees = treesOf(projectIdA);
        assertThat(trees).extracting(Tree::getInventoryNumber).containsExactly("1", "2a");
        Tree first = trees.getFirst();
        // Wzorzec PROJ: EPSG:2178 (7510595.0306, 5585827.1674) -> 50.40814910 N, 21.14905476 E
        assertThat(first.getLocation().getLatitude().doubleValue()).isCloseTo(50.4081491, within(1e-7));
        assertThat(first.getLocation().getLongitude().doubleValue()).isCloseTo(21.1490548, within(1e-7));
        assertThat(first.getTrunkCircumferenceCm()).as("okregi z rysunku nie sa przeliczane na obwod").isNull();

        JsonNode standValidation = upload(userAccountA, projectIdA, "stands/validate", "lubnice.dxf", dxfDrawing(), 200);
        assertThat(standValidation.get("validStandCount").asInt()).isEqualTo(1);
        assertThat(standValidation.at("/preview/0/treeCountInside").asInt()).isEqualTo(1);
        assertThat(standValidation.at("/preview/0/areaSquareMeters").asDouble()).isCloseTo(400.0, within(5.0));
        JsonNode standResult = upload(userAccountA, projectIdA, "stands", "lubnice.dxf", dxfDrawing(), 201);
        assertThat(standResult.get("importedStandCount").asInt()).isEqualTo(1);
        assertThat(standResult.get("assignedTreeCount").asInt()).isEqualTo(1);
        transactionTemplate.executeWithoutResult(status -> {
            TreeStand stand = treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(projectIdA).getFirst();
            assertThat(stand.getName()).isEqualTo("Grupa drzew 1");
            assertThat(stand.getTrees()).extracting(Tree::getInventoryNumber).containsExactly("1");
        });
    }

    private static byte[] fieldAppSpreadsheet() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(bytes, "test", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("Sheet 1");
            String[] headers = {"Etykieta", "Czcionka - nazwa", "Gatunki (lac.)", "Gatunki", "Obwody na 130 cm",
                    "Obwody na 130 cm - suma obliczona według prawa obowiązującego w dniu 2022.03.01", "Średnica korony [m]", "Wysokość [m]",
                    "Pień", "Gałęzie - posusz", "Notatki", "Obszar - powierzchnia", "Pozycja WGS84 - szerokość", "Pozycja WGS84 - długość"};
            for (int column = 0; column < headers.length; column++) sheet.value(0, column, headers[column]);
            Object[][] rows = {
                    {1, "Arial", "Tilia cordata", "lipa drobnolistna", 119, 119, 6, 12, "pochylony", "5%", "zasycha czubek", null, 50.4081491018956, 21.1490547625262},
                    {2, "Arial", "Acer platanoides", "klon zwyczajny", "57+34", 74, 5, 10, null, null, null, null, 50.408125, 21.149038},
                    {210, "Arial", null, "tarnina", null, null, null, null, null, null, null, 63.99, 50.4070, 21.1480}};
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < rows[row].length; column++) {
                    Object value = rows[row][column];
                    if (value instanceof Number number) sheet.value(row + 1, column, number);
                    else if (value != null) sheet.value(row + 1, column, value.toString());
                }
            }
        }
        return bytes.toByteArray();
    }

    @Test
    void fieldInventorySpreadsheetIsImportedWithConditionCardAndWithoutAreasOrComputedCircumferences() throws Exception {
        JsonNode validation = upload(userAccountA, projectIdA, "trees/validate", "inwentaryzacja.xlsx", fieldAppSpreadsheet(), 200);
        assertThat(validation.get("validRowCount").asInt()).isEqualTo(2);
        assertThat(validation.get("readyToImport").asBoolean()).isTrue();
        assertThat(codesOf(validation.get("warnings"))).contains("AREA_ROW_SKIPPED", "MULTI_STEM_CIRCUMFERENCE_IN_NOTES");
        assertThat(StreamSupport.stream(validation.get("ignoredColumns").spliterator(), false).map(JsonNode::asText)).contains("Czcionka - nazwa");
        upload(userAccountA, projectIdA, "trees", "inwentaryzacja.xlsx", fieldAppSpreadsheet(), 201);

        List<Tree> trees = treesOf(projectIdA);
        assertThat(trees).extracting(Tree::getInventoryNumber).containsExactly("1", "2");
        Tree lime = trees.getFirst();
        assertThat(lime.getSpeciesLatin()).isEqualTo("Tilia cordata");
        assertThat(lime.getTrunkCircumferenceCm()).isEqualTo(119);
        assertThat(lime.getHeightM()).isEqualByComparingTo("12");
        assertThat(lime.getConditionNotes()).isEqualTo("Pień: pochylony; Gałęzie - posusz: 5%");
        assertThat(lime.getNotes()).isEqualTo("zasycha czubek");
        assertThat(lime.getLocation().getLatitude().doubleValue()).isCloseTo(50.4081491, within(1e-7));
        Tree maple = trees.get(1);
        assertThat(maple.getTrunkCircumferenceCm()).as("obwod wielopniowy nie jest przeliczany").isNull();
        assertThat(maple.getNotes()).isEqualTo("Obwody pni na 130 cm: 57+34 (suma podana w pliku: 74)");
    }

    @Test
    void shapefileAndGeoPackageExportsCanBeImportedBack() throws Exception {
        long treeId = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"Ż-1\",\"speciesPolish\":\"żywotnik zachodni\",\"heightM\":7.5,\"notes\":\"Przy ogrodzeniu\"}")));
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":50.4085,\"longitude\":21.1495}")).andExpect(status().isOk());
        performAs(userAccountA, post("/api/projects/{p}/stands", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Żywopłot\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.409],[21.149,50.408]]]}}"))
                .andExpect(status().isCreated());

        byte[] shapefile = performAs(userAccountA, get("/api/projects/{p}/exports/shapefile.zip", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        upload(userAccountA, projectIdA2, "trees", "projekt.zip", shapefile, 201);
        JsonNode standResult = upload(userAccountA, projectIdA2, "stands", "projekt.zip", shapefile, 201);
        assertThat(standResult.get("importedStandCount").asInt()).isEqualTo(1);
        assertThat(standResult.get("assignedTreeCount").asInt()).isEqualTo(1);
        Tree copy = treesOf(projectIdA2).getFirst();
        assertThat(copy.getInventoryNumber()).isEqualTo("Ż-1");
        assertThat(copy.getSpeciesPolish()).isEqualTo("żywotnik zachodni");
        assertThat(copy.getHeightM()).isEqualByComparingTo("7.5");
        assertThat(copy.getLocation().getLatitude()).isEqualByComparingTo("50.4085");
        transactionTemplate.executeWithoutResult(status ->
                assertThat(treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(projectIdA2).getFirst().getName()).isEqualTo("Żywopłot"));

        byte[] geoPackage = performAs(userAccountA, get("/api/projects/{p}/exports/trees.gpkg", projectIdA)).andReturn().getResponse().getContentAsByteArray();
        JsonNode gpkgValidation = upload(userAccountA, projectIdA2, "trees/validate", "projekt.gpkg", geoPackage, 200);
        assertThat(gpkgValidation.get("validRowCount").asInt()).isEqualTo(1);
        assertThat(codesOf(gpkgValidation.get("warnings"))).contains("INVENTORY_NUMBER_EXISTS_IN_PROJECT").doesNotContain("COORDINATES_CONVERTED");
        JsonNode gpkgStands = upload(userAccountA, projectIdA2, "stands/validate", "projekt.gpkg", geoPackage, 200);
        assertThat(gpkgStands.get("validStandCount").asInt()).isEqualTo(1);
    }

    @Test
    void standsAreImportedFromGeoJsonWithNamesAndMultiPolygonParts() throws Exception {
        String geoJson = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"properties\":{\"nazwa\":\"Szpaler\",\"uwagi\":\"Wzdłuż drogi\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.408]]]}},"
                + "{\"type\":\"Feature\",\"properties\":{\"name\":\"Zadrzewienie\"},\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":["
                + "[[[21.16,50.41],[21.161,50.41],[21.161,50.411],[21.16,50.41]]],[[[21.17,50.41],[21.171,50.41],[21.171,50.411],[21.17,50.41]]]]}},"
                + "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[21.1,50.4]}}]}";
        JsonNode validation = upload(userAccountA, projectIdA, "stands/validate", "grupy.geojson", geoJson.getBytes(StandardCharsets.UTF_8), 200);
        assertThat(validation.get("validStandCount").asInt()).isEqualTo(3);
        assertThat(StreamSupport.stream(validation.get("preview").spliterator(), false).map(stand -> stand.get("name").asText()))
                .containsExactly("Szpaler", "Zadrzewienie (1)", "Zadrzewienie (2)");
        upload(userAccountA, projectIdA, "stands", "grupy.geojson", geoJson.getBytes(StandardCharsets.UTF_8), 201);
        transactionTemplate.executeWithoutResult(status -> assertThat(treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(projectIdA))
                .extracting(TreeStand::getNotes).contains("Wzdłuż drogi"));
    }

    @Test
    void invalidGisFilesAreRejectedAndNothingIsSaved() throws Exception {
        assertThat(codesOf(upload(userAccountA, projectIdA, "trees/validate", "zly.zip", "not a zip".getBytes(), 200).get("fileErrors")))
                .containsAnyOf("UNREADABLE_FILE", "NO_SHAPEFILE_IN_ZIP");
        assertThat(codesOf(upload(userAccountA, projectIdA, "trees/validate", "zly.dxf", "hello".getBytes(), 200).get("fileErrors")))
                .containsExactly("UNREADABLE_FILE");
        assertThat(codesOf(upload(userAccountA, projectIdA, "trees/validate", "zly.gpkg", "SQLite? no".getBytes(), 200).get("fileErrors")))
                .containsExactly("UNREADABLE_FILE");
        String onlyAreas = new String(dxfDrawing(), StandardCharsets.UTF_8).replace("-Point", "-Other");
        assertThat(codesOf(upload(userAccountA, projectIdA, "trees/validate", "obszary.dxf", onlyAreas.getBytes(StandardCharsets.UTF_8), 200)
                .get("fileErrors"))).containsExactly("NO_POINT_FEATURES");
        String pointsOnly = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[21.1,50.4]}}]}";
        assertThat(codesOf(upload(userAccountA, projectIdA, "stands/validate", "punkty.geojson", pointsOnly.getBytes(StandardCharsets.UTF_8), 200)
                .get("fileErrors"))).containsExactly("NO_POLYGON_FEATURES");
        String utmPolygon = "{\"type\":\"FeatureCollection\",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"urn:ogc:def:crs:EPSG::32634\"}},"
                + "\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[500000,5580000],[500100,5580000],[500100,5580100],[500000,5580000]]]}}]}";
        JsonNode rejected = upload(userAccountA, projectIdA, "stands", "utm.geojson", utmPolygon.getBytes(StandardCharsets.UTF_8), 400);
        assertThat(codesOf(rejected.get("fileErrors"))).containsExactly("UNSUPPORTED_CRS");
        assertThat(codesOf(upload(userAccountA, projectIdA, "stands/validate", "drzewa.csv", "a;b".getBytes(), 200).get("fileErrors")))
                .containsExactly("UNSUPPORTED_FILE_FORMAT");
        assertThat(treesOf(projectIdA)).isEmpty();
        assertThat(treeStandRepository.findAll()).isEmpty();
    }

    @Test
    void importIntoOtherUsersProjectIsNotFound() throws Exception {
        upload(userAccountB, projectIdA, "trees/validate", "lubnice.dxf", dxfDrawing(), 404);
        upload(userAccountB, projectIdA, "stands", "lubnice.dxf", dxfDrawing(), 404);
        assertThat(treesOf(projectIdA)).isEmpty();
    }

    @Test
    void projectionFilesOfPolishGridsAreRecognized() {
        String zone7 = "PROJCS[\"ETRF2000-PL_CS2000_21\",GEOGCS[\"GCS_ETRF2000-PL\",DATUM[\"D_ETRF2000_PL\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],"
                + "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",7500000.0],"
                + "PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",21.0],PARAMETER[\"Scale_Factor\",0.999923],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
        String pl1992 = "PROJCS[\"ETRS_1989_Poland_CS92\",GEOGCS[\"GCS_ETRS_1989\",DATUM[\"D_ETRS_1989\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],"
                + "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],"
                + "PARAMETER[\"False_Northing\",-5300000.0],PARAMETER[\"Central_Meridian\",19.0],PARAMETER[\"Scale_Factor\",0.9993],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
        String utm = "PROJCS[\"WGS_1984_UTM_Zone_34N\",GEOGCS[\"GCS_WGS_1984\"],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],"
                + "PARAMETER[\"Central_Meridian\",21.0],PARAMETER[\"Scale_Factor\",0.9996]]";
        assertThat(ProjectionFileReader.read(zone7).crs()).contains(PolishGridCrs.PL_2000_ZONE_7);
        assertThat(ProjectionFileReader.read(pl1992).crs()).contains(PolishGridCrs.PL_1992);
        assertThat(ProjectionFileReader.read("GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\"]]").crs()).contains(PolishGridCrs.WGS84);
        ProjectionFileReader.ProjectionResult unknown = ProjectionFileReader.read(utm);
        assertThat(unknown.crs()).isEmpty();
        assertThat(unknown.name()).isEqualTo("WGS_1984_UTM_Zone_34N");
    }
}
