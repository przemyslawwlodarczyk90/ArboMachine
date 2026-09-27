package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.service.useraccountfacade.DeleteUserAccountByIdHandler;
import org.springframework.stereotype.Component;

/** Usuniecie cudzego konta przez admina (nie dotyczy kont administratorow). */
@Component
public class DeleteUserAccountAsAdminHandler {
    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler;
    private final DeleteUserAccountByIdHandler deleteUserAccountByIdHandler;

    public DeleteUserAccountAsAdminHandler(FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler,
                                           DeleteUserAccountByIdHandler deleteUserAccountByIdHandler) {
        this.findUserAccountManageableByAdminHandler = findUserAccountManageableByAdminHandler;
        this.deleteUserAccountByIdHandler = deleteUserAccountByIdHandler;
    }

    public void handle(Long userAccountId) {
        findUserAccountManageableByAdminHandler.handle(userAccountId);
        deleteUserAccountByIdHandler.handle(userAccountId);
    }
}
