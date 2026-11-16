package com.example.arbomachine.domain.demo.facade;

import com.example.arbomachine.domain.demo.dto.DemoModeStatusResponse;
import com.example.arbomachine.domain.demo.dto.DemoSessionResponse;
import com.example.arbomachine.domain.demo.service.demomodefacade.GetDemoModeStatusHandler;
import com.example.arbomachine.domain.demo.service.demomodefacade.StartDemoSessionHandler;
import org.springframework.stereotype.Component;

/** Tryb demo: czy jest dostepny i wejscie bez logowania. Deleguje do service/demomodefacade. */
@Component
public class DemoModeFacade {
    private final GetDemoModeStatusHandler getDemoModeStatusHandler;
    private final StartDemoSessionHandler startDemoSessionHandler;

    public DemoModeFacade(GetDemoModeStatusHandler getDemoModeStatusHandler, StartDemoSessionHandler startDemoSessionHandler) {
        this.getDemoModeStatusHandler = getDemoModeStatusHandler;
        this.startDemoSessionHandler = startDemoSessionHandler;
    }

    public DemoModeStatusResponse getDemoModeStatus() {
        return getDemoModeStatusHandler.handle();
    }

    public DemoSessionResponse startDemoSession() {
        return startDemoSessionHandler.handle();
    }
}
