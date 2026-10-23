package com.example.arbomachine.infrastructure.api.project;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pelna sciezka API projektow: prawdziwy SecurityConfig (bez profilu "test"), prawdziwy filtr JWT
 * i prawdziwe tokeny wystawione przez JwtTokenService, baza H2 w pamieci.
 * Dwa konta: A (wlasciciel projektow w wiekszosci testow) i B (probuje dobrac sie do projektow A).
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
class ProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;

    @BeforeEach
    void createTwoConfirmedArboristAccounts() {
        userAccountA = saveConfirmedArborist("user.a@example.com");
        userAccountB = saveConfirmedArborist("user.b@example.com");
    }

    @AfterEach
    void deleteAllProjectsAndAccounts() {
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    private UserAccount saveConfirmedArborist(String email) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash("not-used-in-these-tests")
                .role(UserRole.ARBORIST)
                .emailConfirmed(true)
                .build());
    }

    private String bearerTokenOf(UserAccount userAccount) {
        return "Bearer " + jwtTokenService.createAccessTokenForEmail(userAccount.getEmail());
    }

    private ResultActions postProjectAs(UserAccount userAccount, String requestJson) throws Exception {
        return mockMvc.perform(post("/api/projects")
                .header("Authorization", bearerTokenOf(userAccount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
    }

    /** Tworzy projekt przez API i zwraca jego id. */
    private long createProjectAs(UserAccount userAccount, String projectName) throws Exception {
        String responseJson = postProjectAs(userAccount, "{\"name\":\"" + projectName + "\",\"description\":\"Opis\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("id").asLong();
    }

    // --- tworzenie ---

    @Test
    void authenticatedUserCanCreateProject() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"Park Miejski\",\"description\":\"Inwentaryzacja drzew\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/projects/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Park Miejski"))
                .andExpect(jsonPath("$.description").value("Inwentaryzacja drzew"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void createdProjectIsOwnedByTheAuthenticatedUser() throws Exception {
        long projectId = createProjectAs(userAccountA, "Projekt A");

        Project savedProject = projectRepository.findById(projectId).orElseThrow();
        assertThat(savedProject.getOwner().getId()).isEqualTo(userAccountA.getId());
    }

    @Test
    void ownerIdSentInRequestBodyIsIgnoredAndOwnerComesFromToken() throws Exception {
        String responseJson = postProjectAs(userAccountA,
                "{\"name\":\"Proba przejecia\",\"ownerId\":" + userAccountB.getId() + "}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(responseJson).get("id").asLong();

        assertThat(projectRepository.findByIdAndOwnerId(projectId, userAccountA.getId())).isPresent();
        assertThat(projectRepository.findByIdAndOwnerId(projectId, userAccountB.getId())).isEmpty();
    }

    @Test
    void descriptionIsOptional() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"Bez opisu\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    // --- lista ---

    @Test
    void userGetsOnlyOwnProjects() throws Exception {
        createProjectAs(userAccountA, "Projekt A1");
        createProjectAs(userAccountA, "Projekt A2");
        createProjectAs(userAccountB, "Projekt B1");

        mockMvc.perform(get("/api/projects").header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.containsInAnyOrder("Projekt A1", "Projekt A2")));
    }

    @Test
    void userDoesNotSeeProjectsOfAnotherUser() throws Exception {
        createProjectAs(userAccountA, "Tajny projekt A");

        mockMvc.perform(get("/api/projects").header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- pobranie po id ---

    @Test
    void userCanGetOwnProjectById() throws Exception {
        long projectId = createProjectAs(userAccountA, "Projekt A");

        mockMvc.perform(get("/api/projects/{projectId}", projectId).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Projekt A"));
    }

    @Test
    void userCannotGetProjectOfAnotherUser() throws Exception {
        long projectIdOfA = createProjectAs(userAccountA, "Projekt A");

        mockMvc.perform(get("/api/projects/{projectId}", projectIdOfA).header("Authorization", bearerTokenOf(userAccountB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    @Test
    void foreignProjectAndNonExistentProjectLookTheSame() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}", 999_999).header("Authorization", bearerTokenOf(userAccountA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Project not found"));
    }

    // --- edycja ---

    @Test
    void userCanUpdateOwnProjectAndUpdatedAtMovesForward() throws Exception {
        long projectId = createProjectAs(userAccountA, "Stara nazwa");
        Project projectBeforeUpdate = projectRepository.findById(projectId).orElseThrow();
        Thread.sleep(5); // gwarantuje rozna chwile zapisu

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nowa nazwa\",\"description\":\"Nowy opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nowa nazwa"))
                .andExpect(jsonPath("$.description").value("Nowy opis"));

        Project projectAfterUpdate = projectRepository.findById(projectId).orElseThrow();
        assertThat(projectAfterUpdate.getCreatedAt()).isEqualTo(projectBeforeUpdate.getCreatedAt());
        assertThat(projectAfterUpdate.getUpdatedAt()).isAfter(projectBeforeUpdate.getUpdatedAt());
        assertThat(projectAfterUpdate.getOwner().getId()).isEqualTo(userAccountA.getId());
    }

    @Test
    void patchChangesOnlyFieldsPresentInRequest() throws Exception {
        long projectId = createProjectAs(userAccountA, "Nazwa");

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tylko opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nazwa"))
                .andExpect(jsonPath("$.description").value("Tylko opis"));
    }

    @Test
    void patchCannotChangeIdCreatedAtOrOwner() throws Exception {
        long projectId = createProjectAs(userAccountA, "Nazwa");
        Instant originalCreatedAt = projectRepository.findById(projectId).orElseThrow().getCreatedAt();

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":12345,\"createdAt\":\"2000-01-01T00:00:00Z\",\"ownerId\":" + userAccountB.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));

        Project unchangedProject = projectRepository.findById(projectId).orElseThrow();
        assertThat(unchangedProject.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(unchangedProject.getOwner().getId()).isEqualTo(userAccountA.getId());
    }

    @Test
    void userCannotUpdateProjectOfAnotherUser() throws Exception {
        long projectIdOfA = createProjectAs(userAccountA, "Projekt A");

        mockMvc.perform(patch("/api/projects/{projectId}", projectIdOfA)
                        .header("Authorization", bearerTokenOf(userAccountB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Przejete przez B\"}"))
                .andExpect(status().isNotFound());

        assertThat(projectRepository.findById(projectIdOfA).orElseThrow().getName()).isEqualTo("Projekt A");
    }

    @Test
    void patchWithBlankNameIsRejected() throws Exception {
        long projectId = createProjectAs(userAccountA, "Nazwa");

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Project name must not be blank"));
    }

    // --- walidacja tworzenia ---

    @Test
    void emptyNameIsRejected() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Project name is mandatory"));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void blankNameIsRejected() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Project name is mandatory"));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void requestWithoutNameIsRejected() throws Exception {
        postProjectAs(userAccountA, "{\"description\":\"Sam opis\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Project name is mandatory"));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void nameLongerThanColumnIsRejected() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"" + "x".repeat(256) + "\"}")
                .andExpect(status().isBadRequest());
    }

    // --- etap 45: numer umowy i cel opracowania ---

    @Test
    void contractNumberAndDocumentPurposeAreTrimmedChangedAndCleared() throws Exception {
        String responseJson = postProjectAs(userAccountA,
                "{\"name\":\"Park\",\"contractNumber\":\"  UM/12/2026 \",\"documentPurpose\":\" Inwentaryzacja przed przebudową \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractNumber").value("UM/12/2026"))
                .andExpect(jsonPath("$.documentPurpose").value("Inwentaryzacja przed przebudową"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(patch("/api/projects/{id}", projectId).header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contractNumber\":\"UM/13/2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNumber").value("UM/13/2026"))
                .andExpect(jsonPath("$.documentPurpose").value("Inwentaryzacja przed przebudową"));

        mockMvc.perform(patch("/api/projects/{id}", projectId).header("Authorization", bearerTokenOf(userAccountA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contractNumber\":\"\",\"documentPurpose\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNumber").doesNotExist())
                .andExpect(jsonPath("$.documentPurpose").doesNotExist());
    }

    @Test
    void tooLongContractNumberIsRejected() throws Exception {
        postProjectAs(userAccountA, "{\"name\":\"Park\",\"contractNumber\":\"" + "1".repeat(101) + "\"}")
                .andExpect(status().isBadRequest());
    }

    // --- brak zalogowania ---

    @Test
    void anonymousUserHasNoAccessToProjectApi() throws Exception {
        long projectId = createProjectAs(userAccountA, "Projekt A");

        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/projects/{projectId}", projectId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/projects/{projectId}", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidTokenHasNoAccessToProjectApi() throws Exception {
        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    /** Pomocniczo: odczyt JSON odpowiedzi (np. do debugowania pojedynczego testu). */
    @SuppressWarnings("unused")
    private JsonNode readJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
