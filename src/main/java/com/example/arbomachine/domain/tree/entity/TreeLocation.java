package com.example.arbomachine.domain.tree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lokalizacja drzewa - model V1: sam punkt geograficzny (szerokosc/dlugosc w stopniach dziesietnych, WGS84).
 * Przechowywana jako dwie kolumny tabeli tree (bez osobnej tabeli i bez PostGIS). Zawsze kompletna para -
 * brak lokalizacji to brak calego obiektu (Tree.location == null), nigdy 0,0 ani jedna wspolrzedna.
 * <p>
 * Celowo BEZ dokladnosci, zrodla pomiaru (mapa/GNSS/reczne), wysokosci i czasu pomiaru - te pola dojda tutaj,
 * gdy znany bedzie workflow GNSS (docs/DO_DOPRECYZOWANIA.md). Reszta kodu korzysta z tego obiektu, a nie z dwoch
 * luznych liczb, wiec przejscie na geometrie PostGIS zmieni glownie to miejsce i mapowanie.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeLocation {

    /** numeric(10,7): 7 miejsc po przecinku to ok. 1 cm - wystarcza takze na przyszly pomiar RTK. */
    public static final int COORDINATE_PRECISION = 10;
    public static final int COORDINATE_SCALE = 7;

    public static final String MINIMUM_LATITUDE = "-90";
    public static final String MAXIMUM_LATITUDE = "90";
    public static final String MINIMUM_LONGITUDE = "-180";
    public static final String MAXIMUM_LONGITUDE = "180";

    @Column(name = "latitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal longitude;

    /**
     * Jedyny sposob utworzenia lokalizacji - pilnuje kompletnej pary i zakresow niezaleznie od walidacji requestu.
     * Wiecej niz 7 miejsc po przecinku (np. z odbiornika GNSS) jest zaokraglane do skali kolumny, a nie odrzucane.
     * Ta sama skala sprawia tez, ze equals odpowiada na pytanie "czy to ten sam punkt".
     */
    public static TreeLocation of(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Location requires both latitude and longitude");
        }
        if (isOutside(latitude, MINIMUM_LATITUDE, MAXIMUM_LATITUDE)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (isOutside(longitude, MINIMUM_LONGITUDE, MAXIMUM_LONGITUDE)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        TreeLocation location = new TreeLocation();
        location.latitude = latitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        location.longitude = longitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        return location;
    }

    private static boolean isOutside(BigDecimal coordinate, String minimum, String maximum) {
        return coordinate.compareTo(new BigDecimal(minimum)) < 0 || coordinate.compareTo(new BigDecimal(maximum)) > 0;
    }
}
