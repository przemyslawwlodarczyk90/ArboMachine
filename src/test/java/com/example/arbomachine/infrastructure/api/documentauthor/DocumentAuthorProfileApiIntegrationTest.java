package com.example.arbomachine.infrastructure.api.documentauthor;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dane autora opracowania (ETAP 42): zapis w profilu konta, normalizacja tekstu, usuniecie pustym zapisem, izolacja
 * kont, usuniecie razem z kontem i wstawienie do dokumentu PDF projektu. Prawdziwy SecurityConfig, JWT, H2.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:documentauthorapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=document-author-api-test-secret"
})
@AutoConfigureMockMvc
class DocumentAuthorProfileApiIntegrationTest {

    private static final String PATH = "/api/users/me/document-author-profile";
    private static final String FULL_PROFILE = """
            {"authorFullName":"  mgr inż. Anna Dębowa  ","qualifications":"Certyfikowany Inspektor Drzew, nr 123",
             "companyName":"Zieleń Łódź sp. z o.o.","companyDetails":"ul. Lipowa 1, 90-001 Łódź\\nNIP 123-456-78-90",
             "methodsDescription":"Pomiar obwodu na wysokości 130 cm taśmą. Ocena wizualna VTA.","unknownField":"ignored"}""";
    private static final String EMPTY_PROFILE = """
            {"authorFullName":" ","qualifications":"","companyName":null}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private DocumentAuthorProfileRepository documentAuthorProfileRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;

    @BeforeEach
    void createUsers() {
        userAccountA = saveConfirmedArborist("author.user.a@example.com");
        userAccountB = saveConfirmedArborist("author.user.b@example.com");
    }

    @AfterEach
    void deleteAllData() {
        documentAuthorProfileRepository.deleteAll();
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

    private ResultActions saveProfile(UserAccount userAccount, String body) throws Exception {
        return performAs(userAccount, put(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String documentTextOf(UserAccount userAccount) throws Exception {
        long projectId = objectMapper.readTree(performAs(userAccount, post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Park autora\"}")).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        byte[] pdf = performAs(userAccount, get("/api/projects/{p}/documents/project-report", projectId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void accountWithoutProfileGetsEmptyObject() throws Exception {
        performAs(userAccountA, get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfile").value(false))
                .andExpect(jsonPath("$.authorFullName").doesNotExist());
    }

    @Test
    void savedProfileIsTrimmedReturnedAndReplacedAsWhole() throws Exception {
        saveProfile(userAccountA, FULL_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfile").value(true))
                .andExpect(jsonPath("$.authorFullName").value("mgr inż. Anna Dębowa"))
                .andExpect(jsonPath("$.companyDetails").value("ul. Lipowa 1, 90-001 Łódź\nNIP 123-456-78-90"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        saveProfile(userAccountA, "{\"authorFullName\":\"Jan Lipa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorFullName").value("Jan Lipa"))
                .andExpect(jsonPath("$.qualifications").doesNotExist())
                .andExpect(jsonPath("$.methodsDescription").doesNotExist());
        assertThat(documentAuthorProfileRepository.count()).isEqualTo(1);
    }

    @Test
    void emptySaveRemovesProfile() throws Exception {
        saveProfile(userAccountA, FULL_PROFILE).andExpect(status().isOk());
        saveProfile(userAccountA, EMPTY_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfile").value(false));
        assertThat(documentAuthorProfileRepository.count()).isZero();
        saveProfile(userAccountA, EMPTY_PROFILE).andExpect(status().isOk());
    }

    @Test
    void tooLongTextIsRejected() throws Exception {
        saveProfile(userAccountA, "{\"authorFullName\":\"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest());
        assertThat(documentAuthorProfileRepository.count()).isZero();
    }

    @Test
    void profileBelongsOnlyToItsAccount() throws Exception {
        saveProfile(userAccountA, FULL_PROFILE).andExpect(status().isOk());
        performAs(userAccountB, get(PATH)).andExpect(jsonPath("$.hasProfile").value(false));
        assertThat(documentTextOf(userAccountB)).doesNotContain("Anna Dębowa").contains("Dane do uzupełnienia");
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void documentShowsAuthorCompanyAndMethods() throws Exception {
        saveProfile(userAccountA, FULL_PROFILE).andExpect(status().isOk());
        String text = documentTextOf(userAccountA);
        assertThat(text).contains("Autor opracowania", "mgr inż. Anna Dębowa", "Certyfikowany Inspektor Drzew, nr 123",
                "Wykonawca", "Zieleń Łódź sp. z o.o.", "NIP 123-456-78-90",
                "Metody i techniki pracy", "Pomiar obwodu na wysokości 130 cm taśmą.");
    }

    @Test
    void documentWithoutProfileKeepsPlaceholdersAndNoMethodsRow() throws Exception {
        String text = documentTextOf(userAccountA);
        assertThat(text).contains("Autor opracowania", "Wykonawca", "Dane do uzupełnienia").doesNotContain("Metody i techniki pracy");
    }

    @Test
    void deletingAccountDeletesItsProfile() throws Exception {
        saveProfile(userAccountA, FULL_PROFILE).andExpect(status().isOk());
        performAs(userAccountA, delete("/api/users/me")).andExpect(status().isNoContent());
        assertThat(documentAuthorProfileRepository.count()).isZero();
    }
}
