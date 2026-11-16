package com.example.arbomachine.domain.demo.seed;

import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tryb demo end-to-end z prawdziwym SecurityConfig i JWT: seeder przy starcie, wejscie bez logowania, odczyt danych
 * pokazowych i odrzucenie KAZDEJ proby zapisu przez API (niezaleznie od frontendu).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demomodetest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=demo-mode-test-secret",
        "app.demo.enabled=true"
})
@AutoConfigureMockMvc
class DemoModeIntegrationTest {

    @TempDir
    static Path photoStorageDirectory;

    @DynamicPropertySource
    static void usePhotoStorageInTemporaryDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.tree-photos.storage-directory", () -> photoStorageDirectory.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DemoDataSeeder demoDataSeeder;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private TreePhotoRepository treePhotoRepository;
    @Autowired private TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private PlatformTransactionManager transactionManager;

    private String startDemoSession() throws Exception {
        String body = mockMvc.perform(post("/api/demo/session"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    private UserAccount demoAccount() {
        return userAccountRepository.findByEmail(DemoAccountPolicy.DEFAULT_DEMO_ACCOUNT_EMAIL).orElseThrow();
    }

    @Test
    void seededDemoProjectsAreReadableWithoutSignIn() throws Exception {
        mockMvc.perform(get("/api/demo")).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        String token = startDemoSession();

        JsonNode projects = objectMapper.readTree(mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(projects).hasSize(4);
        assertThat(projects.get(0).get("name").asString()).isEqualTo(DemoProjectsWriter.ANCHOR_PROJECT_NAME);

        for (JsonNode project : projects) {
            long projectId = project.get("id").asLong();
            mockMvc.perform(get("/api/projects/{p}/trees", projectId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/projects/{p}/summary", projectId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        long demoOwnerId = demoAccount().getId();
        List<Project> demoProjects = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(demoOwnerId);
        long treeCount = demoProjects.stream().mapToLong(p -> treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(p.getId()).size()).sum();
        assertThat(treeCount).isEqualTo(204);
        assertThat(treePhotoRepository.count()).isGreaterThan(80);
        assertThat(treePhotoAnnotationRepository.count()).isGreaterThan(15);
        assertThat(photoStorageDirectory.toFile().list()).hasSizeGreaterThan(100);
    }

    @Test
    void everyWriteFromDemoAccountIsRejectedByTheApi() throws Exception {
        String bearer = "Bearer " + startDemoSession();
        Project park = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(demoAccount().getId()).getFirst();
        long treeId = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(park.getId()).getFirst().getId();

        mockMvc.perform(post("/api/projects").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nowy\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DEMO_READ_ONLY"));
        mockMvc.perform(patch("/api/projects/{p}/trees/{t}", park.getId(), treeId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryNumber\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/projects/{p}", park.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/projects/{p}/trees/{t}", park.getId(), treeId).header("Authorization", bearer))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/projects/{p}/trees/{t}/photos", park.getId(), treeId).header("Authorization", bearer)
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1})))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"X\",\"lastName\":\"Y\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/me").header("Authorization", bearer)).andExpect(status().isForbidden());

        assertThat(projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(demoAccount().getId())).hasSize(4);
        assertThat(treeRepository.findById(treeId)).isPresent();
        assertThat(userAccountRepository.findByEmail(DemoAccountPolicy.DEFAULT_DEMO_ACCOUNT_EMAIL)).isPresent();
    }

    @Test
    void demoAccountCannotSignInWithPasswordButOtherArboristsCanStillWrite() throws Exception {
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + DemoAccountPolicy.DEFAULT_DEMO_ACCOUNT_EMAIL + "\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());

        UserAccount arborist = userAccountRepository.save(UserAccount.builder().firstName("A").lastName("B")
                .email("regular.arborist@example.com").passwordHash("unused").role(UserRole.ARBORIST).emailConfirmed(true).build());
        try {
            mockMvc.perform(post("/api/projects").header("Authorization", "Bearer " + jwtTokenService.createAccessTokenForEmail(arborist.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Mój projekt\"}"))
                    .andExpect(status().isCreated());
        } finally {
            projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(arborist.getId()).forEach(projectRepository::delete);
            userAccountRepository.delete(arborist);
        }
    }

    @Test
    void restartDoesNotDuplicateDataAndShiftsDatesToToday() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long ownerId = demoAccount().getId();
        Map<Long, LocalDate> plannedDatesBefore = transaction.execute(status -> {
            Project park = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).getFirst();
            // symulacja: dane zasiane 3 dni temu
            park.setOrderDate(park.getOrderDate().minusDays(3));
            return allWorks(ownerId).stream().filter(work -> work.getPlannedDate() != null)
                    .collect(Collectors.toMap(TreeWork::getId, TreeWork::getPlannedDate));
        });

        demoDataSeeder.prepareDemoData();

        assertThat(projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).hasSize(4);
        transaction.executeWithoutResult(status -> {
            Project park = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).getFirst();
            assertThat(park.getOrderDate())
                    .isEqualTo(LocalDate.now(ZoneId.of("Europe/Warsaw")).minusDays(DemoProjectsWriter.ANCHOR_ORDER_DAYS_AGO));
            for (TreeWork work : allWorks(ownerId)) {
                if (work.getPlannedDate() == null) continue;
                assertThat(work.getPlannedDate()).isEqualTo(plannedDatesBefore.get(work.getId()).plusDays(3));
            }
        });
    }

    private List<TreeWork> allWorks(Long ownerId) {
        return projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .flatMap(project -> treeWorkRepository.findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(project.getId()).stream())
                .toList();
    }
}
