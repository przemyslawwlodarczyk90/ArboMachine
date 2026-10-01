package com.example.arbomachine.infrastructure.api.dataexchange;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.api.Trigger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Import drzew jest jedna transakcja: awaria bazy przy zapisie drugiego drzewa cofa takze pierwsze (ETAP 23).
 * Awarie symuluje wyzwalacz H2 odrzucajacy wstawienie drzewa o numerze "R-2" - bez mockow repozytorium.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:treeimportrollbacktest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=tree-import-rollback-test-secret"
})
@AutoConfigureMockMvc
class TreeImportRollbackIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenService jwtTokenService;

    /** Odrzuca wstawienie drzewa "R-2" - jak awaria bazy w polowie importu. */
    public static class RejectSecondImportedTreeTrigger implements Trigger {
        private int inventoryNumberColumnIndex = -1;

        @Override
        public void init(Connection connection, String schemaName, String triggerName, String tableName, boolean before, int type)
                throws SQLException {
            try (var columns = connection.getMetaData().getColumns(null, schemaName, tableName, null)) {
                int index = 0;
                while (columns.next()) {
                    if ("INVENTORY_NUMBER".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) inventoryNumberColumnIndex = index;
                    index++;
                }
            }
        }

        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            if (newRow != null && "R-2".equals(newRow[inventoryNumberColumnIndex])) {
                throw new SQLException("Simulated database failure: " + Arrays.toString(newRow));
            }
        }
    }

    @Test
    void databaseFailureDuringImportRollsBackAllTrees() throws Exception {
        String treeTable = jdbcTemplate.queryForObject(
                "select table_schema || '.' || table_name from information_schema.tables where upper(table_name) = 'TREE'", String.class);
        jdbcTemplate.execute("create trigger reject_second_imported_tree before insert on " + treeTable
                + " for each row call \"" + RejectSecondImportedTreeTrigger.class.getName() + "\"");

        UserAccount userAccount = userAccountRepository.save(UserAccount.builder()
                .firstName("Test").lastName("User").email("rollback.user@example.com").passwordHash("unused")
                .role(UserRole.ARBORIST).emailConfirmed(true).build());
        String authorization = "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail());
        long projectId = objectMapper.readTree(mockMvc.perform(post("/api/projects").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Rollback\"}"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        assertThatThrownBy(() -> mockMvc.perform(multipart("/api/projects/{p}/imports/trees", projectId)
                .file(new MockMultipartFile("file", "drzewa.csv", "text/csv",
                        "Numer inwentaryzacyjny\nR-1\nR-2\nR-3\n".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", authorization)))
                .rootCause().hasMessageContaining("Simulated database failure");

        assertThat(treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(projectId))
                .as("drzewo R-1 zapisane przed awaria tez zostalo wycofane").isEmpty();
    }
}
