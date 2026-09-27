package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import org.springframework.stereotype.Component;

/** Konto zalogowanego uzytkownika jako DTO (GET /api/users/me). */
@Component
public class GetCurrentUserAccountHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final UserAccountMapper userAccountMapper;

    public GetCurrentUserAccountHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                        UserAccountMapper userAccountMapper) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto handle(String currentUserEmail) {
        return userAccountMapper.toUserAccountDto(findUserAccountByEmailHandler.handle(currentUserEmail));
    }
}
