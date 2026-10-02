package com.example.arbomachine.domain.notification.dto;

import com.example.arbomachine.domain.notification.entity.WebPushSubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body wylaczenia push i sprawdzenia stanu - sam endpoint subskrypcji tej przegladarki. */
public record WebPushEndpointRequest(
        @NotBlank(message = "Push endpoint is required")
        @Size(max = WebPushSubscription.MAXIMUM_ENDPOINT_LENGTH, message = "Push endpoint is too long")
        String endpoint
) {}
