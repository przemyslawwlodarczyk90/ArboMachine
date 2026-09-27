package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Blokada / odblokowanie konta. Zablokowane konto traci dostep od razu - filtr JWT odrzuca tez stare tokeny. */
@Component
public class SetAccountLockedByAdminHandler {
    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler;
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public SetAccountLockedByAdminHandler(FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler,
                                          UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.findUserAccountManageableByAdminHandler = findUserAccountManageableByAdminHandler;
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto handle(Long userAccountId, boolean lockedByAdmin) {
        UserAccount userAccount = findUserAccountManageableByAdminHandler.handle(userAccountId);
        userAccount.setLockedByAdmin(lockedByAdmin);
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }
}
