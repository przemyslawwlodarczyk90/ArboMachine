package com.example.arbomachine.domain.user.dto;

/** Odpowiedz na udane logowanie - token JWT do wysylania w naglowku "Authorization: Bearer ...". */
public record LoginResponseDto(String accessToken) {}
