package com.example.arbomachine.infrastructure.api.notification;

import com.example.arbomachine.domain.notification.dto.TestNotificationResponse;
import com.example.arbomachine.domain.notification.dto.UserNotificationResponse;
import com.example.arbomachine.domain.notification.dto.WebPushConfigResponse;
import com.example.arbomachine.domain.notification.dto.WebPushEndpointRequest;
import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionRequest;
import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionStatusResponse;
import com.example.arbomachine.domain.notification.facade.NotificationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Powiadomienia WLASNEGO konta (SecurityConfig: /api/notifications/** - kazdy zalogowany). Konto zawsze z JWT;
 * zadne id uzytkownika nie przychodzi z zadania.
 */
@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Powiadomienia", description = "Lista powiadomien i Web Push na urzadzeniach zalogowanego uzytkownika")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    public NotificationController(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Operation(summary = "Ostatnie powiadomienia zalogowanego uzytkownika (najnowsze pierwsze)")
    @GetMapping
    public ResponseEntity<List<UserNotificationResponse>> getNotifications(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(notificationFacade.getNotificationsOfCurrentUser(authenticatedUser.getUsername()));
    }

    @Operation(summary = "Klucz publiczny VAPID serwera (applicationServerKey)")
    @GetMapping("/push/config")
    public ResponseEntity<WebPushConfigResponse> getWebPushConfig() {
        return ResponseEntity.ok(notificationFacade.getWebPushConfig());
    }

    @Operation(summary = "Wlaczenie push na tym urzadzeniu - zapis subskrypcji przegladarki (PushSubscription.toJSON())")
    @PutMapping("/push/subscription")
    public ResponseEntity<Void> saveSubscription(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                 @Valid @RequestBody WebPushSubscriptionRequest request) {
        notificationFacade.saveWebPushSubscriptionOfCurrentUser(authenticatedUser.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Wylaczenie push na tym urzadzeniu (idempotentne)")
    @PostMapping("/push/subscription/removal")
    public ResponseEntity<Void> removeSubscription(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                   @Valid @RequestBody WebPushEndpointRequest request) {
        notificationFacade.removeWebPushSubscriptionOfCurrentUser(authenticatedUser.getUsername(), request.endpoint());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Czy subskrypcja tej przegladarki jest zapisana dla zalogowanego konta")
    @PostMapping("/push/subscription/status")
    public ResponseEntity<WebPushSubscriptionStatusResponse> getSubscriptionStatus(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                                   @Valid @RequestBody WebPushEndpointRequest request) {
        return ResponseEntity.ok(notificationFacade.getWebPushSubscriptionStatusOfCurrentUser(authenticatedUser.getUsername(),
                request.endpoint()));
    }

    @Operation(summary = "Powiadomienie testowe na wszystkie urzadzenia zalogowanego uzytkownika")
    @PostMapping("/test")
    public ResponseEntity<TestNotificationResponse> sendTestNotification(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(notificationFacade.sendTestNotificationToCurrentUser(authenticatedUser.getUsername()));
    }
}
