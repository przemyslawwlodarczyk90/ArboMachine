package com.example.arbomachine.domain.treephoto.dto;

import java.math.BigDecimal;

/** Punkt adnotacji we wspolrzednych znormalizowanych do zdjecia: 0 <= x, y <= 1 (nie piksele ekranu). */
public record NormalizedPointDto(BigDecimal x, BigDecimal y) {}
