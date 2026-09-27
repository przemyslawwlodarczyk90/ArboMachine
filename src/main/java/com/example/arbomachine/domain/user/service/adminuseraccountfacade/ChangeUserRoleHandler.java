package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Zmiana roli konta przez admina - tylko miedzy ARBORIST i OFFICIAL. */
@Component
public class ChangeUserRoleHandler {
    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler;
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public ChangeUserRoleHandler(FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler,
                                 UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.findUserAccountManageableByAdminHandler = findUserAccountManageableByAdminHandler;
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto handle(Long userAccountId, AssignableUserRole newRole) {
        UserAccount userAccount = findUserAccountManageableByAdminHandler.handle(userAccountId);
        userAccount.setRole(newRole.toUserRole());
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }
}
