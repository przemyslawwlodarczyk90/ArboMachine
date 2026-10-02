package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.notification.dto.UserNotificationResponse;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Ostatnie powiadomienia zalogowanego uzytkownika (najnowsze pierwsze, V1: bez stronicowania i stanu "przeczytane"). */
@Component
public class GetNotificationsOfCurrentUserHandler {

    static final int RECENT_NOTIFICATIONS_LIMIT = 30;

    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final UserNotificationRepository userNotificationRepository;

    public GetNotificationsOfCurrentUserHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                                UserNotificationRepository userNotificationRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> handle(String currentUserEmail) {
        UserAccount currentUser = findUserAccountByEmailHandler.handle(currentUserEmail);
        return userNotificationRepository.findAllByUserAccountIdOrderByCreatedAtDescIdDesc(currentUser.getId(),
                        PageRequest.of(0, RECENT_NOTIFICATIONS_LIMIT)).stream()
                .map(notification -> new UserNotificationResponse(notification.getId(), notification.getType().name(),
                        notification.getTitle(), notification.getBody(), notification.getLinkPath(), notification.getCreatedAt()))
                .toList();
    }
}
