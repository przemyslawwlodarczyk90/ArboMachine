package com.example.arbomachine.domain.notification.dto;

import com.example.arbomachine.domain.notification.entity.WebPushSubscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body PUT /api/notifications/push/subscription - wynik PushSubscription.toJSON() przegladarki
 * ({ endpoint, expirationTime, keys: { p256dh, auth } }; expirationTime ignorowane). Konto pochodzi z JWT.
 */
public record WebPushSubscriptionRequest(
        @NotBlank(message = "Push endpoint is required")
        @Size(max = WebPushSubscription.MAXIMUM_ENDPOINT_LENGTH, message = "Push endpoint is too long")
        String endpoint,

        @NotNull(message = "Push subscription keys are required")
        @Valid
        Keys keys
) {

    public record Keys(
            @NotBlank(message = "Push subscription key p256dh is required")
            @Size(max = 128, message = "Push subscription key p256dh is too long")
            String p256dh,

            @NotBlank(message = "Push subscription secret auth is required")
            @Size(max = 64, message = "Push subscription secret auth is too long")
            String auth
    ) {}
}
