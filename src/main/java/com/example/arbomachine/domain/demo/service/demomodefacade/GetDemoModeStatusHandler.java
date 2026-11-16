package com.example.arbomachine.domain.demo.service.demomodefacade;

import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import com.example.arbomachine.domain.demo.dto.DemoModeStatusResponse;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Demo jest dostepne, gdy jest wlaczone w konfiguracji i konto demo (z danymi z seedera) istnieje. */
@Component
public class GetDemoModeStatusHandler {
    private final DemoAccountPolicy demoAccountPolicy;
    private final UserAccountRepository userAccountRepository;

    public GetDemoModeStatusHandler(DemoAccountPolicy demoAccountPolicy, UserAccountRepository userAccountRepository) {
        this.demoAccountPolicy = demoAccountPolicy;
        this.userAccountRepository = userAccountRepository;
    }

    public DemoModeStatusResponse handle() {
        boolean available = demoAccountPolicy.isDemoEnabled()
                && userAccountRepository.findByEmailAndEmailConfirmed(demoAccountPolicy.demoAccountEmail(), true)
                .filter(account -> !account.isLockedByAdmin())
                .isPresent();
        return new DemoModeStatusResponse(available);
    }
}
