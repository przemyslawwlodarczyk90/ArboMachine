package com.example.arbomachine.infrastructure.api.dataexchange;

import com.example.arbomachine.domain.tree.entity.Tree;
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
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import i eksport drzew (ETAP 23-24): eksport CSV/XLSX/GeoJSON (odczytywany z powrotem bibliotekami), import
 * CSV/XLSX/GeoJSON z walidacja, "wszystko albo nic", wlasnosc projektu, ignorowanie id z pliku, round-trip.
 * Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dataexchangeapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=data-exchange-api-test-secret"
})
@AutoConfigureMockMvc
class DataExchangeApiIntegrationTest {

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
    private long projectIdB;
    private long treeIdOfB;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("exchange.user.a@example.com");
        userAccountB = saveConfirmedArborist("exchange.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        projectIdB = readId(performAs(userAccountB, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park B\"}")));
        treeIdOfB = readId(performAs(userAccountB, post("/api/projects/{p}/trees", projectIdB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"B-1\",\"speciesPolish\":\"dąb\"}")));
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

    /** Trzy drzewa A: pelne dane + lokalizacja + 2 prace; tylko numer; numer + tekst z ";" i cudzyslowem + lokalizacja. */
    private void createTreesOfA() throws Exception {
        long fullTreeId = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"inventoryNumber":"L-1","speciesPolish":"lipa drobnolistna","speciesLatin":"Tilia cordata",
                         "trunkCircumferenceCm":215,"heightM":18.5,"crownDiameterM":9,"notes":"Żółć; \\"dziupla\\"\\nna 3 m",
                         "conditionNotes":"dobry","hazardNotes":"konar nad chodnikiem"}""")));
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, fullTreeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":51.7592,\"longitude\":19.456,\"accuracyM\":4.5,\"altitudeM\":201.3,\"source\":\"DEVICE_GNSS\"}"))
                .andExpect(status().isOk());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, fullTreeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cięcia redukcyjne\",\"description\":\"od jezdni\"}")).andExpect(status().isCreated());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, fullTreeId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kontrola za 12 miesięcy\"}")).andExpect(status().isCreated());
        readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"L-2\"}")));
        long thirdTreeId = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"L-3\",\"speciesLatin\":\"Acer 'Crimson King'\",\"heightM\":7.25}")));
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, thirdTreeId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":-33.8688,\"longitude\":-151.2093}")).andExpect(status().isOk());
    }

    private byte[] exportOfA(String extension) throws Exception {
        return performAs(userAccountA, get("/api/projects/{p}/exports/trees." + extension, projectIdA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"arbomachine-projekt-" + projectIdA + "-drzewa." + extension + "\""))
                .andReturn().getResponse().getContentAsByteArray();
    }

    private JsonNode validate(String fileName, byte[] content) throws Exception {
        return objectMapper.readTree(performAs(userAccountA, multipart("/api/projects/{p}/imports/trees/validate", projectIdA)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private ResultActions importFile(UserAccount userAccount, long projectId, String fileName, byte[] content) throws Exception {
        return performAs(userAccount, multipart("/api/projects/{p}/imports/trees", projectId)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content)));
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> codesOf(JsonNode issues) {
        return StreamSupport.stream(issues.spliterator(), false).map(issue -> issue.get("code").asText()).toList();
    }

    private List<Tree> treesOfA() {
        return treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(projectIdA);
    }

    // ================= EKSPORT =================

    @Test
    void csvExportHasPolishHeadersSemicolonsCommaDecimalsEmptyNullsAndEscaping() throws Exception {
        createTreesOfA();
        byte[] csvBytes = exportOfA("csv");
        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿");

        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(new StringReader(csv.substring(1)), CSVFormat.DEFAULT.builder().setDelimiter(';').get())) {
            records = parser.getRecords();
        }
        assertThat(records.getFirst().toList()).containsExactly("ID drzewa (ArboMachine)", "Numer inwentaryzacyjny", "Gatunek polski",
                "Gatunek łaciński", "Obwód pnia [cm]", "Wysokość [m]", "Średnica korony [m]", "Notatki", "Stan drzewa",
                "Zagrożenia", "Szerokość geograficzna", "Długość geograficzna", "Dokładność lokalizacji [m]", "Wysokość n.p.m. [m]",
                "Źródło lokalizacji", "Zalecenia i prace", "Waloryzacja - ocena ogólna", "Wartość wiekowa",
                "Wartość przyrodnicza", "Wartość ekologiczna", "Wartość kulturowa", "Wartość krajobrazowa", "Uzasadnienie waloryzacji",
                "Prace do wykonania (liczba)", "Najbliższy termin pracy");
        assertThat(records).hasSize(4);
        CSVRecord full = records.get(1);
        assertThat(full.get(1)).isEqualTo("L-1");
        assertThat(full.get(2)).isEqualTo("lipa drobnolistna");
        assertThat(full.get(4)).isEqualTo("215");
        assertThat(full.get(5)).isEqualTo("18,5");
        assertThat(full.get(6)).isEqualTo("9");
        assertThat(full.get(7)).isEqualTo("Żółć; \"dziupla\"\nna 3 m");
        assertThat(full.get(10)).isEqualTo("51,7592");
        assertThat(full.get(11)).isEqualTo("19,456");
        assertThat(full.get(12)).isEqualTo("4,5");
        assertThat(full.get(13)).isEqualTo("201,3");
        assertThat(full.get(14)).isEqualTo("DEVICE_GNSS");
        assertThat(full.get(15)).isEqualTo("Cięcia redukcyjne; Kontrola za 12 miesięcy");
        CSVRecord onlyNumber = records.get(2);
        assertThat(onlyNumber.get(1)).isEqualTo("L-2");
        assertThat(onlyNumber.toList().subList(2, 23)).allMatch(String::isEmpty);
        assertThat(onlyNumber.get(23)).as("brak prac = 0 prac do wykonania").isEqualTo("0");
        assertThat(csv).doesNotContain("null").contains("\r\n");
    }

    @Test
    void geoJsonExportHasPointsInLongitudeLatitudeOrderStableProperties_AndSkipsTreesWithoutLocation() throws Exception {
        createTreesOfA();
        byte[] geoJsonBytes = performAs(userAccountA, get("/api/projects/{p}/exports/trees.geojson", projectIdA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/geo+json"))
                .andReturn().getResponse().getContentAsByteArray();
        JsonNode root = objectMapper.readTree(geoJsonBytes);

        assertThat(root.get("type").asText()).isEqualTo("FeatureCollection");
        assertThat(root.has("crs")).as("RFC 7946: bez czlonu crs, domyslnie WGS 84").isFalse();
        assertThat(root.at("/arboMachine/schemaVersion").asText()).isEqualTo("1");
        assertThat(root.at("/arboMachine/crs").asText()).isEqualTo("EPSG:4326");
        assertThat(root.at("/arboMachine/treeCount").asInt()).isEqualTo(3);
        assertThat(root.at("/arboMachine/treesWithoutLocationCount").asInt()).isEqualTo(1);
        JsonNode features = root.get("features");
        assertThat(features).hasSize(2);

        JsonNode first = features.get(0);
        assertThat(first.get("type").asText()).isEqualTo("Feature");
        assertThat(first.at("/geometry/type").asText()).isEqualTo("Point");
        assertThat(first.at("/geometry/coordinates/0").decimalValue()).isEqualByComparingTo("19.456");
        assertThat(first.at("/geometry/coordinates/1").decimalValue()).isEqualByComparingTo("51.7592");
        JsonNode properties = first.get("properties");
        assertThat(properties.get("inventoryNumber").asText()).isEqualTo("L-1");
        assertThat(properties.get("speciesPolish").asText()).isEqualTo("lipa drobnolistna");
        assertThat(properties.get("speciesLatin").asText()).isEqualTo("Tilia cordata");
        assertThat(properties.get("trunkCircumferenceCm").asInt()).isEqualTo(215);
        assertThat(properties.get("heightM").decimalValue()).isEqualByComparingTo("18.5");
        assertThat(properties.get("crownDiameterM").decimalValue()).isEqualByComparingTo("9");
        assertThat(properties.get("notes").asText()).isEqualTo("Żółć; \"dziupla\"\nna 3 m");
        assertThat(properties.get("treeId").asLong()).isEqualTo(first.get("id").asLong());
        assertThat(properties.get("workNames").asText()).isEqualTo("Cięcia redukcyjne; Kontrola za 12 miesięcy");
        assertThat(properties.get("works")).hasSize(2);
        assertThat(properties.has("latitude")).as("wspolrzedne tylko w geometrii").isFalse();

        JsonNode southWest = features.get(1);
        assertThat(southWest.at("/geometry/coordinates/0").decimalValue()).isEqualByComparingTo("-151.2093");
        assertThat(southWest.at("/geometry/coordinates/1").decimalValue()).isEqualByComparingTo("-33.8688");
        assertThat(southWest.at("/properties/speciesPolish").isNull()).isTrue();
        assertThat(southWest.at("/properties/works")).isEmpty();
    }

    @Test
    void xlsxExportHasPolishHeadersNumericCellsAndProjectSheet() throws Exception {
        createTreesOfA();
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(exportOfA("xlsx")))) {
            List<Row> rows = workbook.findSheet("Drzewa").orElseThrow().read();
            assertThat(rows.getFirst().getCellText(1)).isEqualTo("Numer inwentaryzacyjny");
            assertThat(rows.getFirst().getCellText(3)).isEqualTo("Gatunek łaciński");
            assertThat(rows).hasSize(4);
            Row full = rows.get(1);
            assertThat(full.getCellText(2)).isEqualTo("lipa drobnolistna");
            assertThat(full.getCell(4).getType()).isEqualTo(CellType.NUMBER);
            assertThat(full.getCell(4).asNumber()).isEqualByComparingTo("215");
            assertThat(full.getCell(5).getType()).isEqualTo(CellType.NUMBER);
            assertThat(full.getCell(5).asNumber()).isEqualByComparingTo("18.5");
            assertThat(full.getCell(10).asNumber()).isEqualByComparingTo("51.7592");
            assertThat(full.getCellText(7)).isEqualTo("Żółć; \"dziupla\"\nna 3 m");
            Row onlyNumber = rows.get(2);
            assertThat(onlyNumber.getOptionalCell(4).map(cell -> cell.getType() == CellType.EMPTY).orElse(true)).isTrue();

            List<Row> projectRows = workbook.findSheet("Projekt").orElseThrow().read();
            assertThat(projectRows.getFirst().getCellText(1)).isEqualTo("Park A");
            assertThat(projectRows.stream().map(row -> row.getCellText(0))).contains("Układ współrzędnych", "Drzewa bez lokalizacji");
        }
    }

    @Test
    void geoPackageExportHasTreesPointLayerInWgs84WithShortGisFieldNames() throws Exception {
        createTreesOfA();
        byte[] geoPackageBytes = performAs(userAccountA, get("/api/projects/{p}/exports/trees.gpkg", projectIdA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/geopackage+sqlite3"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"arbomachine-projekt-" + projectIdA + "-drzewa.gpkg\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(geoPackageBytes, 0, 15, StandardCharsets.US_ASCII)).isEqualTo("SQLite format 3");

        Path geoPackageFile = Files.createTempFile("export-test-", ".gpkg");
        try {
            Files.write(geoPackageFile, geoPackageBytes);
            try (GeoPackage geoPackage = GeoPackageManager.open(geoPackageFile.toFile(), false)) {
                assertThat(geoPackage.getFeatureTables()).containsExactly("trees");
                FeatureDao trees = geoPackage.getFeatureDao("trees");
                assertThat(trees.getGeometryType()).isEqualTo(GeometryType.POINT);
                assertThat(trees.getSrs().getOrganization()).isEqualToIgnoringCase("EPSG");
                assertThat(trees.getSrs().getOrganizationCoordsysId()).isEqualTo(4326);
                assertThat(trees.getTable().getColumnNames()).contains("tree_id", "inventory_no", "species_pl", "species_la",
                        "circum_cm", "height_m", "crown_m", "notes", "condition", "hazards", "map_symbol", "work_names");
                assertThat(trees.count()).as("drzewo bez lokalizacji pominiete").isEqualTo(2);
                List<FeatureRow> rows = new ArrayList<>();
                FeatureResultSet resultSet = trees.queryForAll();
                try {
                    while (resultSet.moveToNext()) rows.add(resultSet.getRow());
                } finally {
                    resultSet.close();
                }
                FeatureRow full = rows.stream().filter(row -> "L-1".equals(row.getValue("inventory_no"))).findFirst().orElseThrow();
                Point point = (Point) full.getGeometry().getGeometry();
                assertThat(point.getX()).isEqualTo(19.456);
                assertThat(point.getY()).isEqualTo(51.7592);
                assertThat(full.getValue("species_pl")).isEqualTo("lipa drobnolistna");
                assertThat(((Number) full.getValue("circum_cm")).intValue()).isEqualTo(215);
                assertThat(((Number) full.getValue("height_m")).doubleValue()).isEqualTo(18.5);
                assertThat(full.getValue("notes")).isEqualTo("Żółć; \"dziupla\"\nna 3 m");
                assertThat(full.getValue("work_names")).isEqualTo("Cięcia redukcyjne; Kontrola za 12 miesięcy");
                FeatureRow sparse = rows.stream().filter(row -> "L-3".equals(row.getValue("inventory_no"))).findFirst().orElseThrow();
                assertThat(sparse.getValue("species_pl")).isNull();
                assertThat(((Point) sparse.getGeometry().getGeometry()).getX()).isEqualTo(-151.2093);
            }
        } finally {
            Files.deleteIfExists(geoPackageFile);
        }
    }

    @Test
    void emptyProjectExportsHeadersOnlyAndEmptyFeatureCollection() throws Exception {
        String csv = new String(exportOfA("csv"), StandardCharsets.UTF_8);
        assertThat(csv.strip().lines()).hasSize(1);
        assertThat(objectMapper.readTree(exportOfA("geojson")).get("features")).isEmpty();
    }

    // ================= IMPORT =================

    @Test
    void validCsvIsValidatedThenImportedAsNewTreesWithLocation() throws Exception {
        byte[] csv = utf8("""
                Numer inwentaryzacyjny;Gatunek polski;Gatunek łaciński;Obwód pnia [cm];Wysokość [m];Średnica korony [m];Notatki;Szerokość geograficzna;Długość geograficzna;Nieznana kolumna
                K-1;klon zwyczajny;Acer platanoides;120;12,5;6;"Pień; rozwidlony";52,2297;21,0122;x
                K-2;;;;;;;;;
                K-3;brzoza;Betula pendula;80.0;9.75;;;;;
                """);
        JsonNode validation = validate("drzewa.csv", csv);
        assertThat(validation.get("readyToImport").asBoolean()).isTrue();
        assertThat(validation.get("format").asText()).isEqualTo("CSV");
        assertThat(validation.get("totalRowCount").asInt()).isEqualTo(3);
        assertThat(validation.get("validRowCount").asInt()).isEqualTo(3);
        assertThat(validation.get("errors")).isEmpty();
        assertThat(validation.get("ignoredColumns").get(0).asText()).isEqualTo("Nieznana kolumna");
        assertThat(validation.at("/preview/0/hasLocation").asBoolean()).isTrue();
        assertThat(treesOfA()).as("sprawdzenie niczego nie zapisuje").isEmpty();

        JsonNode result = objectMapper.readTree(importFile(userAccountA, projectIdA, "drzewa.csv", csv)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(result.get("importedTreeCount").asInt()).isEqualTo(3);
        List<Tree> trees = treesOfA();
        assertThat(trees).extracting(Tree::getInventoryNumber).containsExactly("K-1", "K-2", "K-3");
        Tree first = trees.getFirst();
        assertThat(first.getSpeciesPolish()).isEqualTo("klon zwyczajny");
        assertThat(first.getTrunkCircumferenceCm()).isEqualTo(120);
        assertThat(first.getHeightM()).isEqualByComparingTo("12.5");
        assertThat(first.getNotes()).isEqualTo("Pień; rozwidlony");
        assertThat(first.getLocation().getLatitude()).isEqualByComparingTo("52.2297");
        assertThat(first.getLocation().getLongitude()).isEqualByComparingTo("21.0122");
        assertThat(trees.get(1).getSpeciesPolish()).isNull();
        assertThat(trees.get(1).getLocation()).isNull();
        assertThat(trees.get(2).getTrunkCircumferenceCm()).isEqualTo(80);
        assertThat(trees.get(2).getHeightM()).isEqualByComparingTo("9.75");
    }

    @Test
    void manyRowErrorsAreReportedWithRowNumbersAndNothingIsImported() throws Exception {
        byte[] csv = utf8("""
                inventoryNumber,heightM,trunkCircumferenceCm,crownDiameterM,latitude,longitude
                ,10,,,,
                E-2,dużo,,,,
                E-3,10,12.5,,,
                E-4,151,,,,
                E-5,10.123,,,,
                E-6,10,,0,,
                E-7,10,,,52.1,
                E-8,10,,,95,21
                E-9,10,,,52.1,21.5
                """);
        JsonNode validation = validate("bledy.csv", csv);
        assertThat(validation.get("readyToImport").asBoolean()).isFalse();
        assertThat(validation.get("totalRowCount").asInt()).isEqualTo(9);
        assertThat(validation.get("validRowCount").asInt()).isEqualTo(1);
        assertThat(validation.get("invalidRowCount").asInt()).isEqualTo(8);
        JsonNode errors = validation.get("errors");
        assertThat(codesOf(errors)).containsExactly("REQUIRED_VALUE_MISSING", "INVALID_NUMBER", "NOT_A_WHOLE_NUMBER",
                "VALUE_OUT_OF_RANGE", "TOO_MANY_DECIMAL_PLACES", "VALUE_OUT_OF_RANGE", "INCOMPLETE_LOCATION", "VALUE_OUT_OF_RANGE");
        assertThat(errors.get(0).get("rowNumber").asInt()).as("naglowek = wiersz 1").isEqualTo(2);
        assertThat(errors.get(1).get("rowNumber").asInt()).isEqualTo(3);
        assertThat(errors.get(1).get("column").asText()).isEqualTo("heightM");
        assertThat(errors.get(1).get("field").asText()).isEqualTo("heightM");
        assertThat(errors.get(1).get("value").asText()).isEqualTo("dużo");
        assertThat(errors.get(7).get("column").asText()).isEqualTo("latitude");

        JsonNode rejected = objectMapper.readTree(importFile(userAccountA, projectIdA, "bledy.csv", csv)
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(rejected.get("errors")).hasSize(8);
        assertThat(treesOfA()).as("wszystko albo nic - poprawny wiersz E-9 tez nie zostal zapisany").isEmpty();
    }

    @Test
    void fileLevelProblemsAreReportedAsFileErrors() throws Exception {
        assertThat(codesOf(validate("drzewa.txt", utf8("a;b")).get("fileErrors"))).containsExactly("UNSUPPORTED_FILE_FORMAT");
        assertThat(codesOf(validate("pusty.csv", new byte[0]).get("fileErrors"))).containsExactly("EMPTY_FILE");
        assertThat(codesOf(validate("same-spacje.csv", utf8("  \n ")).get("fileErrors"))).containsExactly("EMPTY_FILE");
        JsonNode missingColumn = validate("bez-numeru.csv", utf8("Gatunek polski;Wysokość [m]\nlipa;10\n"));
        assertThat(codesOf(missingColumn.get("fileErrors"))).containsExactly("MISSING_REQUIRED_COLUMN");
        assertThat(missingColumn.at("/fileErrors/0/column").asText()).isEqualTo("Numer inwentaryzacyjny");
        assertThat(codesOf(validate("naglowek.csv", utf8("Numer inwentaryzacyjny\n")).get("fileErrors"))).containsExactly("NO_DATA_ROWS");
        byte[] windows1250 = "Numer inwentaryzacyjny;Gatunek polski\nX-1;żółw\n".getBytes(java.nio.charset.Charset.forName("windows-1250"));
        assertThat(codesOf(validate("ansi.csv", windows1250).get("fileErrors"))).containsExactly("INVALID_ENCODING");
        assertThat(codesOf(validate("zly.xlsx", utf8("to nie jest arkusz")).get("fileErrors"))).containsExactly("UNREADABLE_FILE");
        assertThat(codesOf(validate("zly.geojson", utf8("{\"type\":")).get("fileErrors"))).containsExactly("INVALID_GEOJSON");
        assertThat(validate("pusty.csv", new byte[0]).get("readyToImport").asBoolean()).isFalse();
    }

    @Test
    void tooManyRowsAreRejected() throws Exception {
        StringBuilder csv = new StringBuilder("Numer inwentaryzacyjny\n");
        for (int row = 1; row <= 2001; row++) csv.append("T-").append(row).append('\n');
        assertThat(codesOf(validate("duzo.csv", utf8(csv.toString())).get("fileErrors"))).containsExactly("TOO_MANY_ROWS");
    }

    @Test
    void duplicateNumbersAreOnlyWarningsAndCreateNewTrees() throws Exception {
        readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"D-1\"}")));
        byte[] csv = utf8("Numer inwentaryzacyjny\nD-1\nD-2\nD-2\n");
        JsonNode validation = validate("duplikaty.csv", csv);
        assertThat(validation.get("readyToImport").asBoolean()).isTrue();
        assertThat(codesOf(validation.get("warnings"))).containsExactly("INVENTORY_NUMBER_EXISTS_IN_PROJECT", "DUPLICATE_INVENTORY_NUMBER_IN_FILE");
        importFile(userAccountA, projectIdA, "duplikaty.csv", csv).andExpect(status().isCreated());
        assertThat(treesOfA()).extracting(Tree::getInventoryNumber).containsExactly("D-1", "D-1", "D-2", "D-2");
    }

    @Test
    void xlsxWithNumericCellsIsImported() throws Exception {
        ByteArrayOutputStream xlsxBytes = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(xlsxBytes, "Test", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("Arkusz1");
            String[] headers = {"Numer inwentaryzacyjny", "Gatunek polski", "Wysokość [m]", "Obwód pnia [cm]", "Szerokość geograficzna", "Długość geograficzna"};
            for (int column = 0; column < headers.length; column++) sheet.value(0, column, headers[column]);
            sheet.value(1, 0, 101);
            sheet.value(1, 1, "jesion wyniosły");
            sheet.value(1, 2, new BigDecimal("14.25"));
            sheet.value(1, 3, 230);
            sheet.value(1, 4, new BigDecimal("50.0614"));
            sheet.value(1, 5, new BigDecimal("19.9366"));
        }
        JsonNode validation = validate("drzewa.xlsx", xlsxBytes.toByteArray());
        assertThat(validation.get("readyToImport").asBoolean()).isTrue();
        importFile(userAccountA, projectIdA, "drzewa.xlsx", xlsxBytes.toByteArray()).andExpect(status().isCreated());
        Tree tree = treesOfA().getFirst();
        assertThat(tree.getInventoryNumber()).isEqualTo("101");
        assertThat(tree.getSpeciesPolish()).isEqualTo("jesion wyniosły");
        assertThat(tree.getHeightM()).isEqualByComparingTo("14.25");
        assertThat(tree.getTrunkCircumferenceCm()).isEqualTo(230);
        assertThat(tree.getLocation().getLatitude()).isEqualByComparingTo("50.0614");
    }

    @Test
    void geoJsonImportUsesPointGeometryAndIgnoresTreeIdFromFile() throws Exception {
        byte[] geoJson = utf8("""
                {"type":"FeatureCollection","crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}},
                 "features":[
                  {"type":"Feature","id":%d,"geometry":{"type":"Point","coordinates":[21.0122,52.2297]},
                   "properties":{"treeId":%d,"inventoryNumber":"G-1","speciesPolish":"Przejęty?","heightM":11.5}},
                  {"type":"Feature","geometry":null,"properties":{"inventory_no":"G-2","species_la":"Quercus robur"}}
                 ]}""".formatted(treeIdOfB, treeIdOfB));
        JsonNode validation = validate("drzewa.geojson", geoJson);
        assertThat(validation.get("readyToImport").asBoolean()).isTrue();
        assertThat(validation.get("ignoredColumns").toString()).contains("treeId");
        importFile(userAccountA, projectIdA, "drzewa.geojson", geoJson).andExpect(status().isCreated());

        List<Tree> trees = treesOfA();
        assertThat(trees).hasSize(2);
        assertThat(trees.getFirst().getId()).isNotEqualTo(treeIdOfB);
        assertThat(trees.getFirst().getLocation().getLatitude()).isEqualByComparingTo("52.2297");
        assertThat(trees.getFirst().getLocation().getLongitude()).isEqualByComparingTo("21.0122");
        assertThat(trees.get(1).getSpeciesLatin()).isEqualTo("Quercus robur");
        Tree treeOfB = treeRepository.findById(treeIdOfB).orElseThrow();
        assertThat(treeOfB.getInventoryNumber()).as("drzewo B nietkniete mimo jego id w pliku").isEqualTo("B-1");
        assertThat(treeOfB.getProject().getId()).isEqualTo(projectIdB);
        assertThat(treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(projectIdB)).hasSize(1);
    }

    @Test
    void geoJsonWithOtherGeometryOrCrsIsRejected() throws Exception {
        JsonNode polygon = validate("poligon.geojson", utf8("""
                {"type":"FeatureCollection","features":[{"type":"Feature",
                 "geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]},"properties":{"inventoryNumber":"P-1"}}]}"""));
        assertThat(codesOf(polygon.get("errors"))).containsExactly("UNSUPPORTED_GEOMETRY");
        JsonNode pl1992 = validate("pl1992.geojson", utf8("""
                {"type":"FeatureCollection","crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:EPSG::2180"}},
                 "features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[637000,486000]},"properties":{"inventoryNumber":"P-1"}}]}"""));
        assertThat(codesOf(pl1992.get("fileErrors"))).containsExactly("UNSUPPORTED_CRS");
        JsonNode outOfRange = validate("zakres.geojson", utf8("""
                {"type":"Feature","geometry":{"type":"Point","coordinates":[637000,486000]},"properties":{"inventoryNumber":"P-1"}}"""));
        assertThat(codesOf(outOfRange.get("errors"))).containsExactly("VALUE_OUT_OF_RANGE", "VALUE_OUT_OF_RANGE");
    }

    @Test
    void exportedFilesCanBeImportedBackAsNewTrees() throws Exception {
        createTreesOfA();
        for (String extension : List.of("csv", "xlsx", "geojson")) {
            JsonNode validation = validate("eksport." + extension, exportOfA(extension));
            assertThat(validation.get("readyToImport").asBoolean()).as(extension).isTrue();
            assertThat(validation.get("validRowCount").asInt()).as(extension).isEqualTo(extension.equals("geojson") ? 2 : 3);
            assertThat(codesOf(validation.get("warnings"))).as(extension).allMatch("INVENTORY_NUMBER_EXISTS_IN_PROJECT"::equals);
        }
        importFile(userAccountA, projectIdA, "eksport.csv", exportOfA("csv")).andExpect(status().isCreated());
        List<Tree> trees = treesOfA();
        assertThat(trees).hasSize(6);
        Tree original = trees.getFirst();
        Tree copy = trees.get(3);
        assertThat(copy.getId()).isNotEqualTo(original.getId());
        assertThat(copy.editableAttributes()).isEqualTo(original.editableAttributes());
        assertThat(copy.getLocation().getLatitude()).isEqualByComparingTo(original.getLocation().getLatitude());
        assertThat(copy.getLocation().getLongitude()).isEqualByComparingTo(original.getLocation().getLongitude());
        assertThat(copy.getLocation().getSource()).as("punkt z importu").isEqualTo(com.example.arbomachine.domain.tree.entity.TreeLocationSource.IMPORT);
    }

    // ================= WLASNOSC =================

    @Test
    void otherUserCannotExportValidateOrImportIntoForeignProject() throws Exception {
        for (String extension : List.of("csv", "xlsx", "geojson", "gpkg")) {
            performAs(userAccountB, get("/api/projects/{p}/exports/trees." + extension, projectIdA)).andExpect(status().isNotFound());
        }
        byte[] csv = utf8("Numer inwentaryzacyjny\nX-1\n");
        performAs(userAccountB, multipart("/api/projects/{p}/imports/trees/validate", projectIdA)
                .file(new MockMultipartFile("file", "a.csv", "text/csv", csv))).andExpect(status().isNotFound());
        importFile(userAccountB, projectIdA, "a.csv", csv).andExpect(status().isNotFound());
        assertThat(treesOfA()).isEmpty();
        mockMvc.perform(get("/api/projects/{p}/exports/trees.csv", projectIdA))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
