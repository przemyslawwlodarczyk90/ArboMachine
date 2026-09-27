package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountStatisticsDto;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Liczniki kont na pulpit admina. */
@Component
public class GetUserAccountStatisticsHandler {
    private final UserAccountRepository userAccountRepository;

    public GetUserAccountStatisticsHandler(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountStatisticsDto handle() {
        return new UserAccountStatisticsDto(
                userAccountRepository.count(),
                userAccountRepository.countByRole(UserRole.ARBORIST),
                userAccountRepository.countByRole(UserRole.OFFICIAL),
                userAccountRepository.countByRole(UserRole.ADMIN),
                userAccountRepository.countByEmailConfirmed(false),
                userAccountRepository.countByLockedByAdmin(true)
        );
    }
}
