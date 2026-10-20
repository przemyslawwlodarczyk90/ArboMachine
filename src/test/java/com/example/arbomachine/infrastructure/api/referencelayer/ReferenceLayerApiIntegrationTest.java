package com.example.arbomachine.infrastructure.api.referencelayer;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectReferenceLayerRepository;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Warstwy referencyjne projektu (ETAP 31): pliki GIS/CAD -> GeoJSON WGS 84, odrzucenia z kodem, IDOR, usuwanie. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:referencelayerapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=reference-layer-api-test-secret",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class ReferenceLayerApiIntegrationTest {

    /** Granica i aleja w PL-2000 strefa 7 (punkt wzorcowy PROJ: 52.2297 N, 21.0122 E). */
    private static final String GEOJSON_PL2000 = """
            {"type":"FeatureCollection","crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:EPSG::2178"}},"features":[
              {"type":"Feature","properties":{"nazwa":"Aleja lipowa","rodzaj":"droga"},
               "geometry":{"type":"LineString","coordinates":[[7500833.512,5788456.487],[7500900,5788500]]}},
              {"type":"Feature","properties":{"nazwa":"Granica parku"},
               "geometry":{"type":"Polygon","coordinates":[[[7500800,5788400],[7500900,5788400],[7500900,5788500],[7500800,5788400]]]}},
              {"type":"Feature","properties":{},"geometry":null}
            ]}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectReferenceLayerRepository projectReferenceLayerRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long projectIdB;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("layer.user.a@example.com");
        userAccountB = saveConfirmedArborist("layer.user.b@example.com");
        projectIdA = createProject(userAccountA, "Park A");
        projectIdB = createProject(userAccountB, "Park B");
    }

    @AfterEach
    void deleteAllData() {
        projectReferenceLayerRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void geoJsonInPolishGridIsStoredAsWgs84WithAttributesAndListedWithoutGeometry() throws Exception {
        long layerId = readId(upload(userAccountA, projectIdA, "granice parku.geojson", GEOJSON_PL2000, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("granice parku"))
                .andExpect(jsonPath("$.sourceFormat").value("GEOJSON"))
                .andExpect(jsonPath("$.sourceCrs").value("EPSG:2178"))
                .andExpect(jsonPath("$.featureCount").value(2)));

        performAs(userAccountA, get("/api/projects/{p}/reference-layers", projectIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].geoJson").doesNotExist());

        JsonNode geoJson = objectMapper.readTree(performAs(userAccountA, get("/api/projects/{p}/reference-layers/{l}/geojson", projectIdA, layerId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode line = geoJson.path("features").get(0);
        assertThat(line.path("geometry").path("type").asText()).isEqualTo("LineString");
        assertThat(line.path("geometry").path("coordinates").get(0).get(0).asDouble()).isCloseTo(21.0122, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(line.path("geometry").path("coordinates").get(0).get(1).asDouble()).isCloseTo(52.2297, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(line.path("properties").path("nazwa").asText()).isEqualTo("Aleja lipowa");
        assertThat(line.path("properties").path("warstwa").asText()).isEqualTo("geojson");
        assertThat(geoJson.path("features").get(1).path("geometry").path("type").asText()).isEqualTo("Polygon");
    }

    @Test
    void dxfDrawingBecomesLayerWithLinesAndRequestedName() throws Exception {
        String dxf = String.join("\n", "0", "SECTION", "2", "ENTITIES",
                "0", "LINE", "8", "Ogrodzenie", "10", "7500000", "20", "5788000", "11", "7500010", "21", "5788000",
                "0", "CIRCLE", "8", "Slupy", "10", "7500005", "20", "5788005", "40", "0.3",
                "0", "ENDSEC", "0", "EOF", "");
        upload(userAccountA, projectIdA, "mapa.dxf", dxf, "Mapa do celow projektowych")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mapa do celow projektowych"))
                .andExpect(jsonPath("$.sourceFormat").value("DXF"))
                .andExpect(jsonPath("$.sourceCrs").value("EPSG:2178"))
                .andExpect(jsonPath("$.featureCount").value(2));
    }

    @Test
    void rejectedFilesReturnCodeAndAreNotStored() throws Exception {
        upload(userAccountA, projectIdA, "dane.xlsx", "x", null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_FORMAT"));
        upload(userAccountA, projectIdA, "pusty.geojson", "{\"type\":\"FeatureCollection\",\"features\":[]}", null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NO_FEATURES"));
        upload(userAccountA, projectIdA, "zly.geojson", "to nie jest json", null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNREADABLE_FILE"));
        upload(userAccountA, projectIdA, "obcy.geojson",
                "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[3000000,3000000]}}]}", null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        assertThat(projectReferenceLayerRepository.count()).isZero();
    }

    @Test
    void otherUsersProjectAndLayerAreNotFoundAndDeleteRemovesOnlyTheLayer() throws Exception {
        long layerIdB = readId(upload(userAccountB, projectIdB, "b.geojson", GEOJSON_PL2000, null));
        upload(userAccountA, projectIdB, "a.geojson", GEOJSON_PL2000, null).andExpect(status().isNotFound());
        performAs(userAccountA, get("/api/projects/{p}/reference-layers/{l}/geojson", projectIdA, layerIdB)).andExpect(status().isNotFound());
        performAs(userAccountA, delete("/api/projects/{p}/reference-layers/{l}", projectIdA, layerIdB)).andExpect(status().isNotFound());

        performAs(userAccountB, delete("/api/projects/{p}/reference-layers/{l}", projectIdB, layerIdB)).andExpect(status().isNoContent());
        assertThat(projectReferenceLayerRepository.count()).isZero();
        assertThat(projectRepository.existsById(projectIdB)).isTrue();
    }

    private ResultActions upload(UserAccount userAccount, long projectId, String fileName, String content, String name) throws Exception {
        MockHttpServletRequestBuilder request = multipart("/api/projects/{p}/reference-layers", projectId)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content.getBytes(StandardCharsets.UTF_8)));
        if (name != null) request = request.param("name", name);
        return performAs(userAccount, request);
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
}
