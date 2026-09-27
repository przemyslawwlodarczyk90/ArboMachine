package com.example.arbomachine.infrastructure.api.project;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Konto bedace wlascicielem projektow nie moze zostac usuniete (ani samodzielnie, ani przez admina) -
 * inaczej projekty zostalyby bez wlasciciela albo baza odrzucilaby usuniecie kluczem obcym (HTTP 500).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:projectapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=project-api-test-secret"
})
@AutoConfigureMockMvc
class ProjectOwnerAccountDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @AfterEach
    void deleteAllProjectsAndAccounts() {
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    private UserAccount saveConfirmedAccount(String email, UserRole role) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Test").lastName("User").email(email).passwordHash("unused")
                .role(role).emailConfirmed(true)
                .build());
    }

    private String bearerTokenOf(UserAccount userAccount) {
        return "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail());
    }

    private void createProjectAs(UserAccount owner) throws Exception {
        mockMvc.perform(post("/api/projects").header("Authorization", bearerTokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Projekt\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void userOwningProjectsCannotDeleteOwnAccount() throws Exception {
        UserAccount projectOwner = saveConfirmedAccount("owner@example.com", UserRole.ARBORIST);
        createProjectAs(projectOwner);

        mockMvc.perform(delete("/api/users/me").header("Authorization", bearerTokenOf(projectOwner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("User account owns projects and cannot be deleted"));

        assertThat(userAccountRepository.findById(projectOwner.getId())).isPresent();
        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCannotDeleteAccountOwningProjects() throws Exception {
        UserAccount projectOwner = saveConfirmedAccount("owner@example.com", UserRole.ARBORIST);
        UserAccount admin = saveConfirmedAccount("admin@example.com", UserRole.ADMIN);
        createProjectAs(projectOwner);

        mockMvc.perform(delete("/api/admin/users/{userAccountId}", projectOwner.getId())
                        .header("Authorization", bearerTokenOf(admin)))
                .andExpect(status().isConflict());

        assertThat(userAccountRepository.findById(projectOwner.getId())).isPresent();
    }

    @Test
    void userWithoutProjectsCanStillDeleteOwnAccount() throws Exception {
        UserAccount userWithoutProjects = saveConfirmedAccount("no.projects@example.com", UserRole.OFFICIAL);

        mockMvc.perform(delete("/api/users/me").header("Authorization", bearerTokenOf(userWithoutProjects)))
                .andExpect(status().isNoContent());

        assertThat(userAccountRepository.findById(userWithoutProjects.getId())).isEmpty();
    }
}
