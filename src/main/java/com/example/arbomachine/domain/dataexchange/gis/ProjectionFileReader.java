package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uklad z pliku .prj (WKT ESRI / OGC): uklad geograficzny (GEOGCS bez PROJCS - WGS 84 / ETRS89, roznica ponizej
 * metra) albo poprzeczny Merkator z parametrami PL-2000 / PL-1992 (poludnik osiowy + false easting). Inne -> brak
 * dopasowania (import odrzucony z nazwa ukladu).
 */
public final class ProjectionFileReader {

    private static final Pattern PROJCS_NAME = Pattern.compile("PROJCS\\[\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private ProjectionFileReader() {
    }

    public record ProjectionResult(Optional<PolishGridCrs> crs, String name) {}

    public static ProjectionResult read(String wkt) {
        String text = wkt.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        if (!upper.contains("PROJCS")) {
            boolean wgs84Like = upper.contains("WGS") || upper.contains("ETRS") || upper.contains("ETRF") || upper.contains("GRS");
            return new ProjectionResult(wgs84Like ? Optional.of(PolishGridCrs.WGS84) : Optional.empty(), firstName(text));
        }
        Matcher nameMatcher = PROJCS_NAME.matcher(text);
        String name = nameMatcher.find() ? nameMatcher.group(1) : firstName(text);
        Double centralMeridian = parameter(text, "central_meridian");
        Double falseEasting = parameter(text, "false_easting");
        Double scale = parameter(text, "scale_factor");
        if (centralMeridian == null || falseEasting == null || !upper.contains("TRANSVERSE")) return new ProjectionResult(Optional.empty(), name);
        for (PolishGridCrs crs : PolishGridCrs.values()) {
            if (crs == PolishGridCrs.WGS84) continue;
            boolean meridianMatches = Math.abs(crs.centralMeridianDegrees() - centralMeridian) < 1e-6;
            boolean eastingMatches = Math.abs(crs.falseEasting() - falseEasting) < 1;
            boolean scaleMatches = scale == null || Math.abs(crs.scaleFactor() - scale) < 1e-6;
            if (meridianMatches && eastingMatches && scaleMatches) return new ProjectionResult(Optional.of(crs), name);
        }
        return new ProjectionResult(Optional.empty(), name);
    }

    private static Double parameter(String wkt, String parameterName) {
        Matcher matcher = Pattern.compile("PARAMETER\\[\"" + parameterName + "\"\\s*,\\s*([-0-9.Ee+]+)", Pattern.CASE_INSENSITIVE).matcher(wkt);
        return matcher.find() ? Double.valueOf(matcher.group(1)) : null;
    }

    private static String firstName(String wkt) {
        Matcher matcher = Pattern.compile("\\[\"([^\"]*)\"").matcher(wkt);
        return matcher.find() ? matcher.group(1) : wkt.substring(0, Math.min(40, wkt.length()));
    }
}
