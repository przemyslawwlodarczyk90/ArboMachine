package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class UserAccountQueryServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Spy
    private UserAccountMapper userAccountMapper = new UserAccountMapper();

    @InjectMocks
    private UserAccountQueryService userAccountQueryService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void returnsAccountWithRoleForKnownEmail() {
        UserAccount officialAccount = UserAccount.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .role(UserRole.OFFICIAL).emailConfirmed(true)
                .build();
        when(userAccountRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(officialAccount));

        UserAccountDto foundAccount = userAccountQueryService.getUserAccountByEmail("john.doe@example.com");

        assertEquals(1L, foundAccount.id());
        assertEquals(UserRole.OFFICIAL, foundAccount.role());
        assertEquals("john.doe@example.com", foundAccount.email());
    }

    @Test
    void unknownEmailIsReported() {
        when(userAccountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserAccountNotFoundException.class, () -> userAccountQueryService.getUserAccountByEmail("missing@example.com"));
    }
}
