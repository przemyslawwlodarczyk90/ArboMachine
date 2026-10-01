package com.example.arbomachine.infrastructure.api.projectdocument;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Dokument projektu" (ETAP 22): GET /api/projects/{projectId}/documents/project-report. Prawdziwy SecurityConfig,
 * JWT, H2, pliki zdjec w katalogu tymczasowym. Tresc PDF sprawdzana przez PDFBox (tekst i osadzone obrazy) -
 * bez porownywania wygladu.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:projectdocumentapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=project-document-api-test-secret"
})
@AutoConfigureMockMvc
class ProjectDocumentApiIntegrationTest {

    @TempDir
    static Path photoStorageDirectory;

    @DynamicPropertySource
    static void usePhotoStorageInTemporaryDirectory(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("app.tree-photos.storage-directory", () -> photoStorageDirectory.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private TreePhotoRepository treePhotoRepository;
    @Autowired private TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;

    @BeforeEach
    void createData() throws Exception {
        userAccountA = saveConfirmedArborist("document.user.a@example.com");
        userAccountB = saveConfirmedArborist("document.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Park Źródlany – Łódź\",\"description\":\"Inwentaryzacja żółtych lip\\nul. Świętokrzyska\"}")));
    }

    @AfterEach
    void deleteAllDataAndFiles() throws IOException {
        treePhotoAnnotationRepository.deleteAll();
        treePhotoRepository.deleteAll();
        treeWorkRepository.deleteAll();
        treeRepository.deleteAll();
        projectRepository.deleteAll();
        userAccountRepository.deleteAll();
        try (Stream<Path> files = Files.list(photoStorageDirectory)) {
            for (Path storedFile : files.toList()) Files.delete(storedFile);
        }
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

    private long createTree(String treeJson) throws Exception {
        return readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA)
                .contentType(MediaType.APPLICATION_JSON).content(treeJson)));
    }

    private byte[] generateDocumentOfUserA() throws Exception {
        return performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static String textOf(byte[] pdfBytes) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    private static int imageCountOf(byte[] pdfBytes) throws IOException {
        int imageCount = 0;
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : pdf.getPages()) {
                for (COSName name : page.getResources().getXObjectNames()) {
                    if (page.getResources().getXObject(name) instanceof PDImageXObject) imageCount++;
                }
            }
        }
        return imageCount;
    }

    private static byte[] realImage(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(60, 120, 60));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return bytes.toByteArray();
    }

    @Test
    void ownerReceivesPdfWithSafeFileNameAndPolishText() throws Exception {
        byte[] pdfBytes = performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"arbomachine-projekt-" + projectIdA + ".pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdfBytes.length).isGreaterThan(1000);
        assertThat(new String(Arrays.copyOf(pdfBytes, 5))).isEqualTo("%PDF-");
        String text = textOf(pdfBytes);
        assertThat(text).contains("ArboMachine", "Dokument projektu", "Park Źródlany – Łódź", "Inwentaryzacja żółtych lip",
                "ul. Świętokrzyska", "Dane do uzupełnienia", "Mapa projektu dostępna jest w aplikacji ArboMachine.");
        assertThat(text).doesNotContain("null");
    }

    @Test
    void projectWithoutTreesStillProducesDocument() throws Exception {
        String text = textOf(generateDocumentOfUserA());
        assertThat(text).contains("W projekcie nie ma jeszcze drzew.", "Liczba drzew").doesNotContain("Karty drzew");
    }

    @Test
    void treeDataLocationWorksAndPhotosAreInDocument() throws Exception {
        long treeId = createTree("""
                {"inventoryNumber":"A-12","speciesPolish":"lipa drobnolistna","speciesLatin":"Tilia cordata",
                 "trunkCircumferenceCm":215,"heightM":18.5,"crownDiameterM":9,"notes":"Dziupla na wysokości 3 m",
                 "conditionNotes":"Posusz w koronie","hazardNotes":"Konar nad chodnikiem"}""");
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/location", projectIdA, treeId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":52.2297,\"longitude\":21.0122}"))
                .andExpect(status().isOk());
        performAs(userAccountA, post("/api/projects/{p}/trees/{t}/works", projectIdA, treeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cięcia redukcyjne\",\"description\":\"Redukcja korony od strony jezdni\"}"))
                .andExpect(status().isCreated());
        performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeId)
                .file(new MockMultipartFile("file", "IMG_0001.png", "image/png", realImage("png", 400, 300))))
                .andExpect(status().isCreated());
        performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeId)
                .file(new MockMultipartFile("file", "IMG_0002.jpg", "image/jpeg", realImage("jpg", 2400, 1800))))
                .andExpect(status().isCreated());

        byte[] pdfBytes = generateDocumentOfUserA();
        String text = textOf(pdfBytes);
        assertThat(text).contains("A-12", "lipa drobnolistna", "Tilia cordata", "215 cm", "18,5 m", "9 m",
                "52,2297° N, 21,0122° E", "Dziupla na wysokości 3 m", "Posusz w koronie", "Konar nad chodnikiem",
                "Cięcia redukcyjne", "Redukcja korony od strony jezdni", "Fot. 1", "IMG_0001.png", "Fot. 2", "IMG_0002.jpg");
        assertThat(text.replaceAll("\\s+", " ")).contains("Liczba drzew 1", "Drzewa z lokalizacją 1", "Zdjęcia 2",
                "Zalecenia i prace 1");
        assertThat(imageCountOf(pdfBytes)).isEqualTo(2);
    }

    @Test
    void treeWithOnlyNumberShowsDashesAndShortEmptyMessagesInsteadOfNull() throws Exception {
        createTree("{\"inventoryNumber\":\"7\"}");

        String text = textOf(generateDocumentOfUserA());
        assertThat(text).contains("Drzewo nr 7", "Gatunek nie podany", "—", "Nie podano", "Brak zapisanych zaleceń.",
                "Brak zdjęć.");
        assertThat(text).doesNotContain("null").doesNotContain("Stan drzewa").doesNotContain("Notatki");
    }

    @Test
    void unreadablePhotoFileDoesNotBreakDocumentAndAnnotatedVersionIsUsedWhenAvailable() throws Exception {
        long treeId = createTree("{\"inventoryNumber\":\"8\"}");
        byte[] fakeJpeg = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 500);
        long brokenPhotoId = readId(performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeId)
                .file(new MockMultipartFile("file", "uszkodzone.jpg", "image/jpeg", fakeJpeg))));
        performAs(userAccountA, multipart(HttpMethod.PUT, "/api/projects/{p}/trees/{t}/photos/{f}/annotation", projectIdA, treeId, brokenPhotoId)
                .file(new MockMultipartFile("document", "", "application/json",
                        "{\"schemaVersion\":1,\"elements\":[{\"id\":\"t1\",\"type\":\"TEXT\",\"points\":[{\"x\":0.1,\"y\":0.1}],\"color\":\"RED\",\"strokeSize\":\"SMALL\",\"text\":\"Ciąć\"}]}".getBytes()))
                .file(new MockMultipartFile("renderedImage", "a.png", "image/png", realImage("png", 300, 200))))
                .andExpect(status().isOk());
        performAs(userAccountA, multipart("/api/projects/{p}/trees/{t}/photos", projectIdA, treeId)
                .file(new MockMultipartFile("file", "tez-uszkodzone.jpg", "image/jpeg", fakeJpeg)))
                .andExpect(status().isCreated());

        byte[] pdfBytes = generateDocumentOfUserA();
        String text = textOf(pdfBytes);
        assertThat(text).contains("uszkodzone.jpg (z adnotacją)", "tez-uszkodzone.jpg");
        assertThat(imageCountOf(pdfBytes)).as("adnotowany PNG osadzony, uszkodzony oryginal pominiety").isEqualTo(1);
    }

    @Test
    void otherUserAndMissingProjectGet404AndAnonymousIsRejected() throws Exception {
        performAs(userAccountB, get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(status().isNotFound());
        performAs(userAccountA, get("/api/projects/{p}/documents/project-report", projectIdA + 1000))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{p}/documents/project-report", projectIdA))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
