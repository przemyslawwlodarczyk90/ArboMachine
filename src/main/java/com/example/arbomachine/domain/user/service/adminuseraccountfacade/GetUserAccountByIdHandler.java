package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Pojedyncze konto dla admina (odczyt - takze konta administratorow). */
@Component
public class GetUserAccountByIdHandler {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public GetUserAccountByIdHandler(UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto handle(Long userAccountId) {
        return userAccountRepository.findById(userAccountId)
                .map(userAccountMapper::toUserAccountDto)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
    }
}
