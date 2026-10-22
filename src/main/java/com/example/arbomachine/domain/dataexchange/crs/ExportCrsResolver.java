package com.example.arbomachine.domain.dataexchange.crs;

import com.example.arbomachine.domain.dataexchange.exception.UnsupportedExportCrsException;
import com.example.arbomachine.domain.dataexchange.export.TreeExportData;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Etap 34: uklad wspolrzednych eksportu GIS wybrany przez uzytkownika. Domena nadal trzyma WGS84 - przeliczenie tylko
 * przy zapisie pliku. Obslugiwane: EPSG:4326 (domyslnie), EPSG:2180 (PL-1992), EPSG:2176-2179 (PL-2000 strefy 5-8)
 * i "PL-2000" = strefa, w ktorej lezy najwiecej drzew projektu (projekt na granicy stref - jedna strefa dla calego pliku).
 */
@Component
public class ExportCrsResolver {

    public static final String PL_2000_AUTOMATIC_ZONE = "PL-2000";
    /** Strefa 7 (Polska centralna), gdy projekt nie ma jeszcze drzew z lokalizacja. */
    private static final PolishGridCrs DEFAULT_PL_2000_ZONE = PolishGridCrs.PL_2000_ZONE_7;

    private final Wgs84CoordinateTransformer wgs84CoordinateTransformer;

    public ExportCrsResolver(Wgs84CoordinateTransformer wgs84CoordinateTransformer) {
        this.wgs84CoordinateTransformer = wgs84CoordinateTransformer;
    }

    public CoordinateTransformer resolve(String requestedCrs, List<TreeExportData> trees) {
        if (requestedCrs == null || requestedCrs.isBlank()) return wgs84CoordinateTransformer;
        String normalized = requestedCrs.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals(PL_2000_AUTOMATIC_ZONE)) {
            return new PolishGridCoordinateTransformer(pl2000ZoneOf(trees));
        }
        if (!normalized.startsWith("EPSG:")) throw unsupported(requestedCrs);
        int epsgCode;
        try {
            epsgCode = Integer.parseInt(normalized.substring("EPSG:".length()));
        } catch (NumberFormatException notNumber) {
            throw unsupported(requestedCrs);
        }
        PolishGridCrs crs = PolishGridCrs.fromEpsgCode(epsgCode).orElseThrow(() -> unsupported(requestedCrs));
        return crs == PolishGridCrs.WGS84 ? wgs84CoordinateTransformer : new PolishGridCoordinateTransformer(crs);
    }

    /**
     * Strefa PL-2000, w ktorej lezy najwiecej drzew z lokalizacja (remis - strefa wczesniejszego drzewa). Nie srednia
     * dlugosc: jedno drzewo z bledna lokalizacja nie przesuwa wtedy strefy calego projektu.
     */
    static PolishGridCrs pl2000ZoneOf(List<TreeExportData> trees) {
        Map<PolishGridCrs, Long> treeCountByZone = trees.stream().filter(TreeExportData::hasLocation)
                .collect(Collectors.groupingBy(tree -> PolishGridCrs.pl2000ZoneForLongitude(tree.longitude().doubleValue()),
                        LinkedHashMap::new, Collectors.counting()));
        PolishGridCrs mostCommonZone = DEFAULT_PL_2000_ZONE;
        long mostTrees = 0;
        for (Map.Entry<PolishGridCrs, Long> zoneAndCount : treeCountByZone.entrySet()) {
            if (zoneAndCount.getValue() > mostTrees) {
                mostCommonZone = zoneAndCount.getKey();
                mostTrees = zoneAndCount.getValue();
            }
        }
        return mostCommonZone;
    }

    private static UnsupportedExportCrsException unsupported(String requestedCrs) {
        return new UnsupportedExportCrsException("Unsupported export CRS: " + requestedCrs
                + " (use EPSG:4326, EPSG:2180, EPSG:2176-2179 or PL-2000)");
    }
}
