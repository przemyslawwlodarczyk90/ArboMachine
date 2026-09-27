package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.AccountLockedByAdminException;
import com.example.arbomachine.domain.user.exception.EmailNotConfirmedException;
import com.example.arbomachine.domain.user.exception.InvalidCredentialsException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Weryfikacja e-maila i hasla oraz wydanie tokenu JWT. */
@Service
public class UserLoginService {
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public UserLoginService(UserAccountRepository userAccountRepository, JwtTokenService jwtTokenService,
                            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Zwraca token JWT. Kolejnosc sprawdzen jest celowa: najpierw haslo, dopiero potem status konta -
     * inaczej po samym e-mailu daloby sie ustalic, ze konto istnieje i jest nieaktywne/zablokowane.
     */
    public String loginAndIssueAccessToken(LoginRequestDto loginRequest) {
        UserAccount userAccount = userAccountRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(loginRequest.password(), userAccount.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (userAccount.isLockedByAdmin()) {
            throw new AccountLockedByAdminException("Account is locked");
        }
        if (!userAccount.isEmailConfirmed()) {
            throw new EmailNotConfirmedException("User is not activated");
        }
        return jwtTokenService.createAccessTokenForEmail(userAccount.getEmail());
    }
}
