package com.example.arbomachine.domain.demo.dto;

/** POST /api/demo/session - krotki token konta demo (tylko do odczytu). */
public record DemoSessionResponse(String accessToken, long validForSeconds) {}
