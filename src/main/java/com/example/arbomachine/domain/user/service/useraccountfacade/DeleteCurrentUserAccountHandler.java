package com.example.arbomachine.domain.user.service.useraccountfacade;

import org.springframework.stereotype.Component;

/** Usuniecie WLASNEGO konta - konto wskazuje e-mail z tokenu JWT. */
@Component
public class DeleteCurrentUserAccountHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final DeleteUserAccountByIdHandler deleteUserAccountByIdHandler;

    public DeleteCurrentUserAccountHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                           DeleteUserAccountByIdHandler deleteUserAccountByIdHandler) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.deleteUserAccountByIdHandler = deleteUserAccountByIdHandler;
    }

    public void handle(String currentUserEmail) {
        deleteUserAccountByIdHandler.handle(findUserAccountByEmailHandler.handle(currentUserEmail).getId());
    }
}
