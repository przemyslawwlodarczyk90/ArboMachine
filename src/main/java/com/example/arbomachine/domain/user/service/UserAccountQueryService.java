package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

/** Odczyt kont po adresie e-mail (e-mail jest tez nazwa uzytkownika w tokenie JWT). */
@Service
public class UserAccountQueryService {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public UserAccountQueryService(UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccountDto getUserAccountByEmail(String email) {
        return userAccountMapper.toUserAccountDto(getUserAccountEntityByEmail(email));
    }

    public UserAccount getUserAccountEntityByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
    }
}
