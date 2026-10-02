package com.example.arbomachine.infrastructure.api.notification;

import com.example.arbomachine.domain.notification.push.FakeBrowserPushSubscription;
import com.example.arbomachine.domain.notification.push.P256KeyCodec;
import com.example.arbomachine.domain.notification.reminder.SendWorkDueRemindersHandler;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Powiadomienia (ETAPY 29-30): subskrypcje Web Push (SSRF, klucze, wlasnosc), alert "stan zly" z monitoringu,
 * szyfrowana wiadomosc odebrana przez falszywa usluge push i odszyfrowana jak w przegladarce, wygasle subskrypcje,
 * przypomnienia o pracach na jutro (raz dziennie), usuniecie konta z powiadomieniami.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notificationapitest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=notification-api-test-secret",
        "app.push.allow-local-test-endpoints=true",
        "app.push.vapid-subject=mailto:test@example.com",
        "app.notifications.work-reminders.enabled=false"
})
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private TreeWorkRepository treeWorkRepository;
    @Autowired private TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    @Autowired private UserNotificationRepository userNotificationRepository;
    @Autowired private WebPushSubscriptionRepository webPushSubscriptionRepository;
    @Autowired private SendWorkDueRemindersHandler sendWorkDueRemindersHandler;
    @Autowired private JwtTokenService jwtTokenService;

    private HttpServer fakePushService;
    private final List<ReceivedPush> receivedPushes = new CopyOnWriteArrayList<>();
    private final AtomicInteger pushResponseStatus = new AtomicInteger(201);

    private UserAccount userAccountA;
    private UserAccount userAccountB;
    private long projectIdA;
    private long treeIdA1;

    record ReceivedPush(String path, Map<String, List<String>> headers, byte[] body) {}

    @BeforeEach
    void startFakePushServiceAndCreateData() throws Exception {
        fakePushService = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakePushService.createContext("/", exchange -> {
            receivedPushes.add(new ReceivedPush(exchange.getRequestURI().getPath(), Map.copyOf(exchange.getRequestHeaders()),
                    exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(pushResponseStatus.get(), -1);
            exchange.close();
        });
        fakePushService.start();

        userAccountA = saveConfirmedArborist("notification.user.a@example.com");
        userAccountB = saveConfirmedArborist("notification.user.b@example.com");
        projectIdA = readId(performAs(userAccountA, post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Park A\"}")));
        treeIdA1 = readId(performAs(userAccountA, post("/api/projects/{p}/trees", projectIdA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inventoryNumber\":\"G-1\"}")));
    }

    @AfterEach
    void stopFakePushServiceAndDeleteData() {
        fakePushService.stop(0);
        webPushSubscriptionRepository.deleteAll();
        userNotificationRepository.deleteAll();
        treeMonitoringRecordRepository.deleteAll();
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

    private String fakeEndpoint(String device) {
        return "http://127.0.0.1:" + fakePushService.getAddress().getPort() + "/push/" + device;
    }

    private ResultActions subscribe(UserAccount userAccount, String endpoint, FakeBrowserPushSubscription browser) throws Exception {
        return performAs(userAccount, put("/api/notifications/push/subscription").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("endpoint", endpoint, "expirationTime", "",
                        "keys", Map.of("p256dh", browser.p256dh(), "auth", browser.auth())))));
    }

    private boolean isSubscribed(UserAccount userAccount, String endpoint) throws Exception {
        return objectMapper.readTree(performAs(userAccount, post("/api/notifications/push/subscription/status")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("endpoint", endpoint))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("subscribed").asBoolean();
    }

    private JsonNode notificationsOf(UserAccount userAccount) throws Exception {
        return objectMapper.readTree(performAs(userAccount, get("/api/notifications")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void awaitPushCount(int expectedCount) throws InterruptedException {
        for (int attempt = 0; attempt < 100 && receivedPushes.size() < expectedCount; attempt++) Thread.sleep(50);
        assertThat(receivedPushes).hasSize(expectedCount);
    }

    private long createMonitoringRecord(String body) throws Exception {
        return readId(performAs(userAccountA, post("/api/projects/{p}/trees/{t}/monitoring", projectIdA, treeIdA1)
                .contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    @Test
    void serverPublicKeyIsStableP256Point() throws Exception {
        String publicKey = objectMapper.readTree(performAs(userAccountA, get("/api/notifications/push/config"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("publicKey").asText();
        assertThat(P256KeyCodec.fromBase64Url(publicKey)).hasSize(65);
        performAs(userAccountB, get("/api/notifications/push/config")).andExpect(jsonPath("$.publicKey").value(publicKey));
        mockMvc.perform(get("/api/notifications/push/config")).andExpect(status().isUnauthorized());
    }

    @Test
    void subscriptionToUnknownOrInternalAddressesAndInvalidKeysIsRejected() throws Exception {
        FakeBrowserPushSubscription browser = new FakeBrowserPushSubscription();
        for (String endpoint : List.of("http://169.254.169.254/latest/meta-data", "https://evil.example.com/push/1",
                "https://fcm.googleapis.com:8443/fcm/send/1", "https://user@fcm.googleapis.com/fcm/send/1",
                "file:///etc/passwd", "not a url")) {
            subscribe(userAccountA, endpoint, browser).andExpect(status().isBadRequest());
        }
        performAs(userAccountA, put("/api/notifications/push/subscription").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("endpoint", "https://fcm.googleapis.com/fcm/send/1",
                        "keys", Map.of("p256dh", "AAAA", "auth", browser.auth())))))
                .andExpect(status().isBadRequest());
        subscribe(userAccountA, "https://fcm.googleapis.com/fcm/send/abc", browser).andExpect(status().isNoContent());
        subscribe(userAccountA, "https://updates.push.services.mozilla.com/wpush/v2/abc", browser).andExpect(status().isNoContent());
        assertThat(webPushSubscriptionRepository.findAll()).hasSize(2);
    }

    @Test
    void subscriptionBelongsToCurrentAccountAndCanBeRemovedOnlyByIt() throws Exception {
        FakeBrowserPushSubscription browser = new FakeBrowserPushSubscription();
        String endpoint = fakeEndpoint("tablet");
        subscribe(userAccountA, endpoint, browser).andExpect(status().isNoContent());
        assertThat(isSubscribed(userAccountA, endpoint)).isTrue();
        assertThat(isSubscribed(userAccountB, endpoint)).isFalse();

        performAs(userAccountB, post("/api/notifications/push/subscription/removal").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("endpoint", endpoint)))).andExpect(status().isNoContent());
        assertThat(isSubscribed(userAccountA, endpoint)).as("B nie usunie subskrypcji A").isTrue();

        // Ta sama przegladarka po zalogowaniu na konto B przechodzi na B (jedno urzadzenie = jedno konto).
        subscribe(userAccountB, endpoint, browser).andExpect(status().isNoContent());
        assertThat(isSubscribed(userAccountA, endpoint)).isFalse();
        assertThat(isSubscribed(userAccountB, endpoint)).isTrue();
        assertThat(webPushSubscriptionRepository.findAll()).hasSize(1);

        performAs(userAccountB, post("/api/notifications/push/subscription/removal").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("endpoint", endpoint)))).andExpect(status().isNoContent());
        assertThat(webPushSubscriptionRepository.findAll()).isEmpty();
    }

    @Test
    void poorHealthRecordNotifiesOwnerWithEncryptedWebPushOnce() throws Exception {
        FakeBrowserPushSubscription browser = new FakeBrowserPushSubscription();
        subscribe(userAccountA, fakeEndpoint("tablet"), browser).andExpect(status().isNoContent());

        createMonitoringRecord("{\"inspectionDate\":\"2026-09-20\",\"healthStatus\":\"GOOD\"}");
        long poorRecordId = createMonitoringRecord("{\"inspectionDate\":\"2026-10-01\",\"healthStatus\":\"POOR\",\"notes\":\"Pęknięcie\"}");
        awaitPushCount(1);

        ReceivedPush push = receivedPushes.getFirst();
        assertThat(push.path()).isEqualTo("/push/tablet");
        assertThat(push.headers().get("Content-encoding")).containsExactly("aes128gcm");
        assertThat(push.headers().get("Ttl")).containsExactly("86400");
        assertThat(push.headers().get("Authorization").getFirst()).startsWith("vapid t=").contains(", k=");
        JsonNode message = objectMapper.readTree(browser.decrypt(push.body()));
        assertThat(message.get("title").asText()).isEqualTo("Drzewo G-1: stan zły");
        assertThat(message.get("body").asText()).contains("01.10.2026", "Park A");
        assertThat(message.get("url").asText()).isEqualTo("/projects/" + projectIdA + "/trees/" + treeIdA1);

        JsonNode notifications = notificationsOf(userAccountA);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).get("type").asText()).isEqualTo("TREE_POOR_HEALTH");
        assertThat(notificationsOf(userAccountB)).isEmpty();

        // Edycja wpisu, ktory nadal ma ocene "zly" - bez drugiego alertu.
        performAs(userAccountA, put("/api/projects/{p}/trees/{t}/monitoring/{r}", projectIdA, treeIdA1, poorRecordId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"inspectionDate\":\"2026-10-01\",\"healthStatus\":\"POOR\",\"notes\":\"Inne\"}"))
                .andExpect(status().isOk());
        Thread.sleep(300);
        assertThat(receivedPushes).hasSize(1);
        assertThat(notificationsOf(userAccountA)).hasSize(1);
    }

    @Test
    void expiredSubscriptionIsRemovedAndTestNotificationReportsDeliveredDevices() throws Exception {
        subscribe(userAccountA, fakeEndpoint("phone"), new FakeBrowserPushSubscription()).andExpect(status().isNoContent());
        performAs(userAccountA, post("/api/notifications/test")).andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveredDeviceCount").value(1));
        assertThat(notificationsOf(userAccountA).get(0).get("type").asText()).isEqualTo("TEST");

        pushResponseStatus.set(410);
        performAs(userAccountA, post("/api/notifications/test")).andExpect(jsonPath("$.deliveredDeviceCount").value(0));
        assertThat(webPushSubscriptionRepository.findAll()).as("410 Gone = subskrypcja wygasla").isEmpty();
        assertThat(notificationsOf(userAccountA)).as("powiadomienie zostaje na liscie mimo braku push").hasSize(2);
    }

    @Test
    void plannedWorksDueTomorrowProduceOneReminderPerUserAndDay() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 2);
        String worksPath = "/api/projects/" + projectIdA + "/trees/" + treeIdA1 + "/works";
        for (String body : List.of(
                "{\"name\":\"Cięcia sanitarne\",\"status\":\"PLANNED\",\"plannedDate\":\"2026-10-03\"}",
                "{\"name\":\"Wiązanie korony\",\"status\":\"PLANNED\",\"plannedDate\":\"2026-10-03\"}",
                "{\"name\":\"Zalecenie bez planu\",\"status\":\"RECOMMENDED\",\"plannedDate\":\"2026-10-03\"}",
                "{\"name\":\"Pojutrze\",\"status\":\"PLANNED\",\"plannedDate\":\"2026-10-04\"}",
                "{\"name\":\"Wykonana\",\"status\":\"COMPLETED\",\"plannedDate\":\"2026-10-03\"}")) {
            performAs(userAccountA, post(worksPath).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        }
        subscribe(userAccountA, fakeEndpoint("tablet"), new FakeBrowserPushSubscription()).andExpect(status().isNoContent());

        assertThat(sendWorkDueRemindersHandler.handle(today)).isEqualTo(1);
        assertThat(sendWorkDueRemindersHandler.handle(today)).as("drugie uruchomienie tego dnia - bez duplikatu").isZero();
        awaitPushCount(1);

        JsonNode reminder = notificationsOf(userAccountA).get(0);
        assertThat(reminder.get("type").asText()).isEqualTo("WORKS_DUE_TOMORROW");
        assertThat(reminder.get("title").asText()).isEqualTo("Jutro (03.10.2026) zaplanowane prace: 2");
        assertThat(reminder.get("body").asText()).contains("Cięcia sanitarne", "Wiązanie korony").doesNotContain("Zalecenie bez planu", "Pojutrze");
        assertThat(reminder.get("linkPath").asText()).isEqualTo("/projects/" + projectIdA + "/management");
        assertThat(notificationsOf(userAccountB)).isEmpty();
    }

    @Test
    void deletingAccountRemovesItsNotificationsAndSubscriptions() throws Exception {
        subscribe(userAccountB, fakeEndpoint("b"), new FakeBrowserPushSubscription()).andExpect(status().isNoContent());
        performAs(userAccountB, post("/api/notifications/test")).andExpect(status().isOk());
        assertThat(userNotificationRepository.findAll()).hasSize(1);

        performAs(userAccountB, delete("/api/users/me")).andExpect(status().is2xxSuccessful());
        assertThat(userAccountRepository.findByEmail("notification.user.b@example.com")).isEmpty();
        assertThat(userNotificationRepository.findAll()).isEmpty();
        assertThat(webPushSubscriptionRepository.findAll()).isEmpty();
    }
}
