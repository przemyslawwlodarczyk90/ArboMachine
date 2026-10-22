package com.example.arbomachine.domain.dataexchange.crs;

import java.util.Arrays;
import java.util.Optional;

/**
 * Uklady wspolrzednych przyjmowane przy imporcie GIS (etap 29): WGS 84 i polskie uklady plaskie PL-2000 (strefy 5-8)
 * oraz PL-1992 (elipsoida GRS80, odwzorowanie Gaussa-Krugera). Wspolrzedne plaskie podajemy jak w plikach GIS/CAD:
 * x = easting (EPSG "Y"), y = northing (EPSG "X"). Domena zawsze przechowuje WGS 84 (TreeLocation).
 */
public enum PolishGridCrs {
    WGS84(4326, 0, 0, 0, 0),
    PL_2000_ZONE_5(2176, 15, 0.999923, 5_500_000, 0),
    PL_2000_ZONE_6(2177, 18, 0.999923, 6_500_000, 0),
    PL_2000_ZONE_7(2178, 21, 0.999923, 7_500_000, 0),
    PL_2000_ZONE_8(2179, 24, 0.999923, 8_500_000, 0),
    PL_1992(2180, 19, 0.9993, 500_000, -5_300_000);

    private final int epsgCode;
    private final double centralMeridianDegrees;
    private final double scaleFactor;
    private final double falseEasting;
    private final double falseNorthing;

    PolishGridCrs(int epsgCode, double centralMeridianDegrees, double scaleFactor, double falseEasting, double falseNorthing) {
        this.epsgCode = epsgCode;
        this.centralMeridianDegrees = centralMeridianDegrees;
        this.scaleFactor = scaleFactor;
        this.falseEasting = falseEasting;
        this.falseNorthing = falseNorthing;
    }

    public int epsgCode() {
        return epsgCode;
    }

    public double centralMeridianDegrees() {
        return centralMeridianDegrees;
    }

    public double scaleFactor() {
        return scaleFactor;
    }

    public double falseEasting() {
        return falseEasting;
    }

    public String code() {
        return "EPSG:" + epsgCode;
    }

    /** Etap 34: nazwa ukladu jak w rejestrze EPSG (np. do GeoPackage i README eksportu). */
    public String displayName() {
        return switch (this) {
            case WGS84 -> "WGS 84";
            case PL_1992 -> "ETRS89 / Poland CS92";
            default -> "ETRS89 / Poland CS2000 zone " + (epsgCode - 2171);
        };
    }

    /**
     * Etap 34: definicja WKT (OGC WKT1) ukladu polskiego - biblioteka GeoPackage nie ma EPSG:2176-2180 w swoich
     * definicjach, wiec zapisujemy je sami (parametry jak w rejestrze EPSG; programy GIS i tak rozpoznaja uklad po
     * AUTHORITY). Bez AXIS - wspolrzedne w pliku sa w kolejnosci GIS: x = easting, y = northing.
     */
    public String wktDefinition() {
        if (this == WGS84) throw new IllegalStateException("WGS 84 is defined by the GeoPackage library");
        return "PROJCS[\"" + displayName() + "\",GEOGCS[\"ETRS89\",DATUM[\"European_Terrestrial_Reference_System_1989\","
                + "SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],"
                + "AUTHORITY[\"EPSG\",\"6258\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                + "UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4258\"]],"
                + "PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],"
                + "PARAMETER[\"central_meridian\"," + plainNumber(centralMeridianDegrees) + "],"
                + "PARAMETER[\"scale_factor\"," + plainNumber(scaleFactor) + "],"
                + "PARAMETER[\"false_easting\"," + plainNumber(falseEasting) + "],"
                + "PARAMETER[\"false_northing\"," + plainNumber(falseNorthing) + "],"
                + "UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AUTHORITY[\"EPSG\",\"" + epsgCode + "\"]]";
    }

    private static String plainNumber(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static Optional<PolishGridCrs> fromEpsgCode(int epsgCode) {
        return Arrays.stream(values()).filter(crs -> crs.epsgCode == epsgCode).findFirst();
    }

    /**
     * Uklad odgadniety z wielkosci wspolrzednych (pliki bez opisu ukladu, np. DXF): |x| <= 180 i |y| <= 90 -> WGS 84;
     * easting 5,3-8,7 mln i northing 5,4-6,2 mln -> PL-2000 ze strefa z pierwszej cyfry eastingu; easting 100-900 tys.
     * i northing 100-800 tys. -> PL-1992. Inaczej - brak (plik odrzucony jako nieobslugiwany uklad).
     */
    public static Optional<PolishGridCrs> guessFromCoordinates(double x, double y) {
        if (Math.abs(x) <= 180 && Math.abs(y) <= 90) return Optional.of(WGS84);
        if (x >= 5_300_000 && x < 8_700_000 && y >= 5_400_000 && y <= 6_200_000) {
            return fromEpsgCode(2176 + (int) (x / 1_000_000) - 5).filter(crs -> crs != WGS84 && crs != PL_1992);
        }
        if (x >= 100_000 && x <= 900_000 && y >= 100_000 && y <= 800_000) return Optional.of(PL_1992);
        return Optional.empty();
    }

    /** Etap 31: strefa PL-2000 dla dlugosci geograficznej (poludnik osiowy 15/18/21/24 +- 1,5 stopnia; poza Polska - najblizsza). */
    public static PolishGridCrs pl2000ZoneForLongitude(double longitude) {
        int zoneNumber = (int) Math.min(8, Math.max(5, Math.round(longitude / 3)));
        return fromEpsgCode(2171 + zoneNumber).orElseThrow();
    }

    /** Etap 31: WGS 84 (dlugosc, szerokosc w stopniach) -> [easting, northing] w metrach tego ukladu (dokumenty, eksport). */
    public double[] fromWgs84(double longitude, double latitude) {
        if (this == WGS84) return new double[]{longitude, latitude};
        double[] planar = TransverseMercator.forward(longitude, latitude, scaleFactor, centralMeridianDegrees);
        return new double[]{planar[0] + falseEasting, planar[1] + falseNorthing};
    }

    /** Wspolrzedne pliku -> WGS 84 [dlugosc, szerokosc] w stopniach. */
    public double[] toWgs84(double x, double y) {
        if (this == WGS84) return new double[]{x, y};
        return TransverseMercator.inverse(x - falseEasting, y - falseNorthing, scaleFactor, centralMeridianDegrees);
    }

    /**
     * Odwrotne odwzorowanie poprzeczne Merkatora na elipsoidzie GRS80 - szeregi Krugera (n^4), dokladnosc ponizej
     * milimetra w zasiegu polskich ukladow. Bez bibliotek zewnetrznych.
     */
    static final class TransverseMercator {
        private static final double A = 6_378_137.0;
        private static final double F = 1 / 298.257222101;
        private static final double N = F / (2 - F);
        private static final double RECTIFYING_RADIUS = A / (1 + N) * (1 + N * N / 4 + Math.pow(N, 4) / 64);
        private static final double ECCENTRICITY = Math.sqrt(F * (2 - F));
        private static final double[] ALPHA = {
                N / 2 - 2 * N * N / 3 + 5 * Math.pow(N, 3) / 16 + 41 * Math.pow(N, 4) / 180,
                13 * N * N / 48 - 3 * Math.pow(N, 3) / 5 + 557 * Math.pow(N, 4) / 1440,
                61 * Math.pow(N, 3) / 240 - 103 * Math.pow(N, 4) / 140,
                49561 * Math.pow(N, 4) / 161280};
        private static final double[] BETA = {
                N / 2 - 2 * N * N / 3 + 37 * Math.pow(N, 3) / 96 - Math.pow(N, 4) / 360,
                N * N / 48 + Math.pow(N, 3) / 15 - 437 * Math.pow(N, 4) / 1440,
                17 * Math.pow(N, 3) / 480 - 37 * Math.pow(N, 4) / 840,
                4397 * Math.pow(N, 4) / 161280};
        private static final double[] DELTA = {
                2 * N - 2 * N * N / 3 - 2 * Math.pow(N, 3) + 116 * Math.pow(N, 4) / 45,
                7 * N * N / 3 - 8 * Math.pow(N, 3) / 5 - 227 * Math.pow(N, 4) / 45,
                56 * Math.pow(N, 3) / 15 - 136 * Math.pow(N, 4) / 35,
                4279 * Math.pow(N, 4) / 630};

        private TransverseMercator() {
        }

        /** Odwzorowanie wprost (szeregi Krugera) -> [easting, northing] bez przesuniec ukladu. */
        static double[] forward(double longitude, double latitude, double scaleFactor, double centralMeridianDegrees) {
            double phi = Math.toRadians(latitude);
            double lambda = Math.toRadians(longitude - centralMeridianDegrees);
            double t = Math.sinh(atanh(Math.sin(phi)) - ECCENTRICITY * atanh(ECCENTRICITY * Math.sin(phi)));
            double xiPrime = Math.atan2(t, Math.cos(lambda));
            double etaPrime = atanh(Math.sin(lambda) / Math.sqrt(1 + t * t));
            double xi = xiPrime;
            double eta = etaPrime;
            for (int j = 1; j <= 4; j++) {
                xi += ALPHA[j - 1] * Math.sin(2 * j * xiPrime) * Math.cosh(2 * j * etaPrime);
                eta += ALPHA[j - 1] * Math.cos(2 * j * xiPrime) * Math.sinh(2 * j * etaPrime);
            }
            return new double[]{scaleFactor * RECTIFYING_RADIUS * eta, scaleFactor * RECTIFYING_RADIUS * xi};
        }

        private static double atanh(double value) {
            return 0.5 * Math.log((1 + value) / (1 - value));
        }

        static double[] inverse(double easting, double northing, double scaleFactor, double centralMeridianDegrees) {
            double xi = northing / (scaleFactor * RECTIFYING_RADIUS);
            double eta = easting / (scaleFactor * RECTIFYING_RADIUS);
            double xiPrime = xi;
            double etaPrime = eta;
            for (int j = 1; j <= 4; j++) {
                xiPrime -= BETA[j - 1] * Math.sin(2 * j * xi) * Math.cosh(2 * j * eta);
                etaPrime -= BETA[j - 1] * Math.cos(2 * j * xi) * Math.sinh(2 * j * eta);
            }
            double chi = Math.asin(Math.sin(xiPrime) / Math.cosh(etaPrime));
            double latitude = chi;
            for (int j = 1; j <= 4; j++) latitude += DELTA[j - 1] * Math.sin(2 * j * chi);
            double longitude = Math.toRadians(centralMeridianDegrees) + Math.atan2(Math.sinh(etaPrime), Math.cos(xiPrime));
            return new double[]{Math.toDegrees(longitude), Math.toDegrees(latitude)};
        }
    }
}
