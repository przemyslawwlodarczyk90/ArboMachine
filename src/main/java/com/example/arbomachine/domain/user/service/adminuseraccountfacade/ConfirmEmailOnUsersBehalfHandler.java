package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Potwierdzenie e-maila za uzytkownika, np. gdy mail aktywacyjny nie dotarl. */
@Component
public class ConfirmEmailOnUsersBehalfHandler {
    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler;
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public ConfirmEmailOnUsersBehalfHandler(FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler,
                                            UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.findUserAccountManageableByAdminHandler = findUserAccountManageableByAdminHandler;
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto handle(Long userAccountId) {
        UserAccount userAccount = findUserAccountManageableByAdminHandler.handle(userAccountId);
        userAccount.setEmailConfirmed(true);
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }
}
