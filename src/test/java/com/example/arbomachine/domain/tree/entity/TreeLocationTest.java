package com.example.arbomachine.domain.tree.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Niezmienniki TreeLocation niezalezne od walidacji requestu: kompletna para, zakresy, skala kolumny. */
class TreeLocationTest {

    @Test
    void createsLocationWithCoordinatesScaledToColumn() {
        TreeLocation location = TreeLocation.of(new BigDecimal("52.2296756"), new BigDecimal("21.01"));

        assertThat(location.getLatitude()).isEqualTo(new BigDecimal("52.2296756"));
        assertThat(location.getLongitude()).isEqualTo(new BigDecimal("21.0100000"));
    }

    @Test
    void samePointWrittenDifferentlyIsEqual() {
        assertThat(TreeLocation.of(new BigDecimal("52.5"), new BigDecimal("21")))
                .isEqualTo(TreeLocation.of(new BigDecimal("52.50000000"), new BigDecimal("21.0")));
    }

    @Test
    void partialLocationIsRejected() {
        assertThatThrownBy(() -> TreeLocation.of(new BigDecimal("52"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeLocation.of(null, new BigDecimal("21"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coordinatesOutsideRangeAreRejected() {
        assertThatThrownBy(() -> TreeLocation.of(new BigDecimal("90.1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeLocation.of(new BigDecimal("-90.1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeLocation.of(BigDecimal.ZERO, new BigDecimal("180.1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeLocation.of(BigDecimal.ZERO, new BigDecimal("-180.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
