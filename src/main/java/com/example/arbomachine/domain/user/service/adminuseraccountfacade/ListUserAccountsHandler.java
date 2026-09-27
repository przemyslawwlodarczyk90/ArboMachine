package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Lista kont posortowana po id; roleFilter == null oznacza wszystkie role. */
@Component
public class ListUserAccountsHandler {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;

    public ListUserAccountsHandler(UserAccountRepository userAccountRepository, UserAccountMapper userAccountMapper) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
    }

    public List<UserAccountDto> handle(UserRole roleFilter) {
        List<UserAccount> userAccounts = roleFilter == null
                ? userAccountRepository.findAll()
                : userAccountRepository.findByRole(roleFilter);
        return userAccounts.stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(userAccountMapper::toUserAccountDto)
                .toList();
    }
}
