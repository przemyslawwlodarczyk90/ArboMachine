package com.example.arbomachine.infrastructure.api.demo;

import com.example.arbomachine.domain.demo.dto.DemoModeStatusResponse;
import com.example.arbomachine.domain.demo.dto.DemoSessionResponse;
import com.example.arbomachine.domain.demo.facade.DemoModeFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publiczne endpointy trybu demo (bez logowania). */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "Tryb demo", description = "Przegladanie przykladowych projektow arborysty bez konta, tylko do odczytu")
public class DemoModeController {

    private final DemoModeFacade demoModeFacade;

    public DemoModeController(DemoModeFacade demoModeFacade) {
        this.demoModeFacade = demoModeFacade;
    }

    @Operation(summary = "Czy tryb demo jest dostepny")
    @GetMapping
    public ResponseEntity<DemoModeStatusResponse> getDemoModeStatus() {
        return ResponseEntity.ok(demoModeFacade.getDemoModeStatus());
    }

    @Operation(summary = "Wejscie do trybu demo - zwraca krotki token konta demo (tylko do odczytu)")
    @PostMapping("/session")
    public ResponseEntity<DemoSessionResponse> startDemoSession() {
        return ResponseEntity.ok(demoModeFacade.startDemoSession());
    }
}
