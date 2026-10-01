package com.example.arbomachine.domain.tree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lokalizacja drzewa: punkt geograficzny (szerokosc/dlugosc w stopniach dziesietnych, WGS84) i - od etapu 25 -
 * opcjonalne dane pomiaru: wysokosc n.p.m., dokladnosc pozioma i zrodlo punktu (reczne / mapa / GNSS urzadzenia /
 * import). Przechowywana jako kolumny tabeli tree (bez osobnej tabeli i bez PostGIS). Para wspolrzednych zawsze
 * kompletna - brak lokalizacji to brak calego obiektu (Tree.location == null), nigdy 0,0 ani jedna wspolrzedna.
 * <p>
 * Celowo BEZ czasu pomiaru, PDOP, liczby satelitow, typu rozwiazania RTK i wysokosci anteny - dojda tutaj, gdy
 * workflow GNSS/RTK zostanie potwierdzony (docs/DO_DOPRECYZOWANIA.md). Reszta kodu korzysta z tego obiektu, a nie
 * z luznych liczb, wiec przejscie na geometrie PostGIS zmieni glownie to miejsce i mapowanie.
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

    /** Dane pomiaru w metrach: numeric(7,2) - centymetry wystarcza takze dla RTK. */
    public static final int MEASUREMENT_PRECISION = 7;
    public static final int MEASUREMENT_SCALE = 2;
    /** Zakres wysokosci n.p.m. - ochrona przed bledami odbiornika (Polska: ok. -2 do 2500 m). */
    public static final String MINIMUM_ALTITUDE_M = "-500";
    public static final String MAXIMUM_ALTITUDE_M = "9000";
    /** Dokladnosc pozioma (promien) - GPS przegladarki bez sygnalu potrafi zwrocic kilka km; powyzej 10 km to blad. */
    public static final String MINIMUM_ACCURACY_M = "0";
    public static final String MAXIMUM_ACCURACY_M = "10000";

    @Column(name = "latitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal longitude;

    /** Wysokosc n.p.m. w metrach (z GNSS, jesli urzadzenie ja podaje) - null = brak. */
    @Column(name = "location_altitude_m", precision = MEASUREMENT_PRECISION, scale = MEASUREMENT_SCALE)
    private BigDecimal altitudeM;

    /** Dokladnosc pozioma w metrach (promien, jak Geolocation API: ok. 95% ufnosci) - null = nieznana. */
    @Column(name = "location_accuracy_m", precision = MEASUREMENT_PRECISION, scale = MEASUREMENT_SCALE)
    private BigDecimal accuracyM;

    /** Zrodlo punktu - null = punkt sprzed etapu 25 (nieznane). */
    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", length = 32)
    private TreeLocationSource source;

    /** Sam punkt, bez danych pomiaru (zrodlo nieznane) - m.in. dla starszych wywolan. */
    public static TreeLocation of(BigDecimal latitude, BigDecimal longitude) {
        return of(latitude, longitude, null, null, null);
    }

    /**
     * Jedyny sposob utworzenia lokalizacji - pilnuje kompletnej pary i zakresow niezaleznie od walidacji requestu.
     * Wiecej miejsc po przecinku (np. z odbiornika GNSS) jest zaokraglane do skali kolumn, a nie odrzucane.
     * Ta sama skala sprawia tez, ze equals odpowiada na pytanie "czy to ten sam pomiar".
     */
    public static TreeLocation of(BigDecimal latitude, BigDecimal longitude, BigDecimal altitudeM, BigDecimal accuracyM,
                                  TreeLocationSource source) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Location requires both latitude and longitude");
        }
        if (isOutside(latitude, MINIMUM_LATITUDE, MAXIMUM_LATITUDE)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (isOutside(longitude, MINIMUM_LONGITUDE, MAXIMUM_LONGITUDE)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (altitudeM != null && isOutside(altitudeM, MINIMUM_ALTITUDE_M, MAXIMUM_ALTITUDE_M)) {
            throw new IllegalArgumentException("Altitude must be between -500 and 9000 m");
        }
        if (accuracyM != null && isOutside(accuracyM, MINIMUM_ACCURACY_M, MAXIMUM_ACCURACY_M)) {
            throw new IllegalArgumentException("Accuracy must be between 0 and 10000 m");
        }
        TreeLocation location = new TreeLocation();
        location.latitude = latitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        location.longitude = longitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        location.altitudeM = altitudeM == null ? null : altitudeM.setScale(MEASUREMENT_SCALE, RoundingMode.HALF_UP);
        location.accuracyM = accuracyM == null ? null : accuracyM.setScale(MEASUREMENT_SCALE, RoundingMode.HALF_UP);
        location.source = source;
        return location;
    }

    private static boolean isOutside(BigDecimal value, String minimum, String maximum) {
        return value.compareTo(new BigDecimal(minimum)) < 0 || value.compareTo(new BigDecimal(maximum)) > 0;
    }
}
