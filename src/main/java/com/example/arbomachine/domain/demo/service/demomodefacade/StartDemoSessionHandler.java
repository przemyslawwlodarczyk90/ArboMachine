package com.example.arbomachine.domain.demo.service.demomodefacade;

import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import com.example.arbomachine.domain.demo.dto.DemoSessionResponse;
import com.example.arbomachine.domain.demo.exception.DemoModeUnavailableException;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.springframework.stereotype.Component;

/**
 * Wejscie do trybu demo bez logowania: token konta demo z krotka waznoscia. Haslo konta demo jest losowe i nieznane,
 * wiec jedyna droga do tego konta jest ten endpoint, a zapis blokuje DemoReadOnlyFilter.
 */
@Component
public class StartDemoSessionHandler {
    private final DemoAccountPolicy demoAccountPolicy;
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenService jwtTokenService;

    public StartDemoSessionHandler(DemoAccountPolicy demoAccountPolicy, UserAccountRepository userAccountRepository,
                                   JwtTokenService jwtTokenService) {
        this.demoAccountPolicy = demoAccountPolicy;
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public DemoSessionResponse handle() {
        if (!demoAccountPolicy.isDemoEnabled()) {
            throw new DemoModeUnavailableException("Demo mode is not available");
        }
        UserAccount demoAccount = userAccountRepository
                .findByEmailAndEmailConfirmed(demoAccountPolicy.demoAccountEmail(), true)
                .filter(account -> !account.isLockedByAdmin())
                .orElseThrow(() -> new DemoModeUnavailableException("Demo mode is not available"));
        String accessToken = jwtTokenService.createAccessTokenForEmail(demoAccount.getEmail(),
                demoAccountPolicy.sessionDuration());
        return new DemoSessionResponse(accessToken, demoAccountPolicy.sessionDuration().toSeconds());
    }
}
