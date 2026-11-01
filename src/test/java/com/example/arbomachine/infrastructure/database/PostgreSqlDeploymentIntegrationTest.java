package com.example.arbomachine.infrastructure.database;

import com.example.arbomachine.ArboMachineApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wdrozenie na wspolnym PostgreSQL 18.6 (AWS): czysta baza "arbomachine" zalozona skryptem infrastruktury
 * (deploy/postgres/initdb/10-arbomachine.sh), schemat i tabele WYLACZNIE z migracji Flyway, Hibernate validate,
 * profil prod. Sprawdza: start, rejestracje i logowanie, CRUD projektu i drzewa, zdjecie na dysku, restart aplikacji
 * (dane i zdjecie zostaja, migracje nie wykonuja sie ponownie), /actuator/health oraz izolacje - rola ArboMachine
 * nie moze polaczyc sie z baza innej aplikacji (arnocrm) na tym samym serwerze. Wymaga Dockera (bez niego pomijany).
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlDeploymentIntegrationTest {

    private static final String APPLICATION_DB_PASSWORD = "arbomachine-test-password-" + UUID.randomUUID();
    private static final String LATEST_MIGRATION_VERSION = "10";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine")
            .withEnv("ARBOMACHINE_DB_PASSWORD", APPLICATION_DB_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("deploy/postgres/initdb/10-arbomachine.sh")),
                    "/docker-entrypoint-initdb.d/10-arbomachine.sh")
            .withCopyFileToContainer(MountableFile.forClasspathResource("postgres-test/20-other-application-database.sql"),
                    "/docker-entrypoint-initdb.d/20-other-application-database.sql");

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @TempDir
    private Path photoDirectory;

    @Test
    void shouldBuildSchemaWithFlywayServeCrudSurviveRestartAndStayIsolatedFromOtherDatabases() throws Exception {
        byte[] photoBytes = jpegBytes();
        long projectId;
        long treeId;
        long photoId;
        int historyRowsAfterFirstStart;

        try (ConfigurableApplicationContext application = startApplication()) {
            String baseUrl = baseUrlOf(application);
            assertThat(get(baseUrl + "/actuator/health", null).body()).contains("\"status\":\"UP\"");
            assertThat(get(baseUrl + "/v3/api-docs", null).statusCode()).as("Swagger wylaczony w profilu prod").isEqualTo(404);

            assertThat(postJson(baseUrl + "/api/users/register", null, """
                    {"profile":{"firstName":"Anna","lastName":"Test","email":"pg18@example.test"},
                     "password":"Pg18Password!","accountType":"ARBORIST"}""").statusCode()).isEqualTo(201);
            String accessToken = login(baseUrl);
            projectId = idOf(postJson(baseUrl + "/api/projects", accessToken, """
                    {"name":"Park PG18","description":"test","clientName":"Gmina","siteAddress":"Parkowa 1","orderDate":"2026-11-01"}"""));
            treeId = idOf(postJson(baseUrl + "/api/projects/" + projectId + "/trees", accessToken, "{\"inventoryNumber\":\"A-1\"}"));
            photoId = idOf(uploadPhoto(baseUrl + "/api/projects/" + projectId + "/trees/" + treeId + "/photos", accessToken, photoBytes));

            try (Connection connection = applicationConnection("arbomachine")) {
                assertThat(singleValue(connection, "select version from arbomachine.flyway_schema_history where success and version is not null order by installed_rank desc limit 1"))
                        .isEqualTo(LATEST_MIGRATION_VERSION);
                assertThat(singleValue(connection, "select count(*) from information_schema.tables where table_schema = 'public'"))
                        .as("aplikacja nie tworzy niczego w schemacie public").isEqualTo("0");
                historyRowsAfterFirstStart = Integer.parseInt(singleValue(connection, "select count(*) from arbomachine.flyway_schema_history"));
            }
        }

        try (ConfigurableApplicationContext restartedApplication = startApplication()) {
            String baseUrl = baseUrlOf(restartedApplication);
            String accessToken = login(baseUrl);
            assertThat(get(baseUrl + "/api/projects/" + projectId + "/trees", accessToken).body()).contains("\"A-1\"");
            HttpResponse<byte[]> photo = httpClient.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/projects/" + projectId
                            + "/trees/" + treeId + "/photos/" + photoId + "/content")).header("Authorization", "Bearer " + accessToken).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(photo.statusCode()).isEqualTo(200);
            assertThat(photo.body()).isEqualTo(photoBytes);
            try (Connection connection = applicationConnection("arbomachine")) {
                assertThat(Integer.parseInt(singleValue(connection, "select count(*) from arbomachine.flyway_schema_history")))
                        .as("restart nie wykonuje migracji ponownie").isEqualTo(historyRowsAfterFirstStart);
            }
        }

        assertThatThrownBy(() -> applicationConnection("arnocrm").close())
                .as("rola ArboMachine nie ma dostepu do bazy innej aplikacji")
                .isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
    }

    private ConfigurableApplicationContext startApplication() {
        // Argumenty wiersza polecen - wyzszy priorytet niz testowe config/application.properties (H2, create-drop).
        return new SpringApplicationBuilder(ArboMachineApplication.class).run(
                "--spring.profiles.active=prod",
                "--server.port=0",
                "--DB_HOST=" + POSTGRES.getHost(),
                "--DB_PORT=" + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                "--DB_NAME=arbomachine",
                "--DB_SCHEMA=arbomachine",
                "--DB_USERNAME=arbomachine",
                "--DB_PASSWORD=" + APPLICATION_DB_PASSWORD,
                "--spring.flyway.enabled=true",
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.jpa.properties.jakarta.persistence.create-database-schemas=false",
                "--JWT_SECRET=postgresql-deployment-test-secret-0123456789abcdef",
                "--SMTP_HOST=localhost",
                "--APP_REGISTRATION_EMAIL_CONFIRMATION_REQUIRED=false",
                "--app.notifications.work-reminders.enabled=false",
                "--ARBOMACHINE_TREE_PHOTOS_DIRECTORY=" + photoDirectory.toAbsolutePath());
    }

    private static String baseUrlOf(ConfigurableApplicationContext application) {
        return "http://localhost:" + application.getEnvironment().getProperty("local.server.port");
    }

    private static Connection applicationConnection(String databaseName) throws SQLException {
        String url = "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/" + databaseName;
        return DriverManager.getConnection(url, "arbomachine", APPLICATION_DB_PASSWORD);
    }

    private static String singleValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private String login(String baseUrl) throws Exception {
        HttpResponse<String> response = postJson(baseUrl + "/api/users/login", null,
                "{\"email\":\"pg18@example.test\",\"password\":\"Pg18Password!\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).path("accessToken").asText();
    }

    private long idOf(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isBetween(200, 201);
        JsonNode body = objectMapper.readTree(response.body());
        return body.path("id").asLong();
    }

    private HttpResponse<String> get(String url, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (accessToken != null) request.header("Authorization", "Bearer " + accessToken);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String url, String accessToken, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (accessToken != null) request.header("Authorization", "Bearer " + accessToken);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> uploadPhoto(String url, String accessToken, byte[] jpeg) throws Exception {
        String boundary = "arbomachine-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"drzewo.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(jpeg);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return httpClient.send(HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] jpegBytes() throws Exception {
        BufferedImage image = new BufferedImage(16, 12, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpeg);
        return jpeg.toByteArray();
    }
}
