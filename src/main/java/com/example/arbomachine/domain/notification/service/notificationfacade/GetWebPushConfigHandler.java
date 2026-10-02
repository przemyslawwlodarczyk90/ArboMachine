package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.notification.dto.WebPushConfigResponse;
import com.example.arbomachine.domain.notification.push.WebPushServerKeyProvider;
import org.springframework.stereotype.Component;

/** Klucz publiczny VAPID dla przegladarki (potrzebny do PushManager.subscribe). */
@Component
public class GetWebPushConfigHandler {
    private final WebPushServerKeyProvider webPushServerKeyProvider;

    public GetWebPushConfigHandler(WebPushServerKeyProvider webPushServerKeyProvider) {
        this.webPushServerKeyProvider = webPushServerKeyProvider;
    }

    public WebPushConfigResponse handle() {
        return new WebPushConfigResponse(webPushServerKeyProvider.serverKeys().publicKeyBase64Url());
    }
}
