package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfDrawing;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfPoint;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfPolyline;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Rysunek DXF -> warstwy GIS wg typowej konwencji warstw eksportu z aplikacji terenowych (etap 29):
 * <ul>
 *   <li>drzewa: okregi/punkty na warstwach konczacych sie na "-Point" (np. "...-Deciduous-Point", "...-Coniferous-Point").
 *       Numer drzewa - tekst na warstwie "-Label" na koncu odnosnika (polilinii zaczynajacej sie w punkcie drzewa):
 *       dopasowanie GLOBALNE (najlepsze pary najpierw), koszt = odleglosc linii bazowej tekstu od pol wysokosci ponizej
 *       konca odnosnika (waga 10) + odleglosc w poziomie od strony, w ktora wskazuje odnosnik (etykieta po lewej konczy
 *       sie przy odnosniku - szerokosc ~0,6 wysokosci na znak). Punkt bez odnosnika - najblizszy wolny tekst do 6 jednostek.
 *       Na przykladowym rysunku arborysty: 836 z 839 numerow zgodnych z arkuszem (2 bledne, 1 brak) - dlatego import
 *       z rysunku ostrzega, ze numery trzeba sprawdzic (zrodlem danych pozostaje arkusz),</li>
 *   <li>obszary: polilinie na warstwach konczacych sie na "-Area"; gdy takich brak - zamkniete polilinie innych warstw
 *       (bez warstw opisow).</li>
 * </ul>
 * Rodzaj drzewa z nazwy warstwy, obwod i korona z okregow - NIE sa importowane. Uklad wspolrzednych odgadywany z wartosci.
 */
public final class DxfLayerExtractor {

    private static final double LEADER_START_TOLERANCE = 0.05;
    private static final double LEADER_END_SEARCH_DISTANCE = 3.0;
    private static final double CHARACTER_WIDTH_PER_HEIGHT = 0.6;
    private static final double VERTICAL_COST_WEIGHT = 10;
    private static final double NEAREST_LABEL_DISTANCE = 6.0;

    private DxfLayerExtractor() {
    }

    private record Candidate(double cost, int pointIndex, int textIndex) {}

    public static GisLayer treeLayer(DxfDrawing drawing) {
        List<DxfPoint> treePoints = drawing.points().stream().filter(point -> DxfDrawingReader.layerEndsWith(point.layer(), "-point")).toList();
        List<DxfText> labels = drawing.texts().stream().filter(text -> DxfDrawingReader.layerEndsWith(text.layer(), "-label")).toList();
        List<DxfPolyline> leaders = drawing.polylines().stream().filter(line -> DxfDrawingReader.layerEndsWith(line.layer(), "-label")).toList();

        Map<Integer, Integer> labelIndexByPointIndex = matchLabelsByLeaders(treePoints, labels, leaders);
        List<GisFeature> features = new ArrayList<>();
        Set<Integer> fallbackUsed = new HashSet<>(labelIndexByPointIndex.values());
        for (int pointIndex = 0; pointIndex < treePoints.size(); pointIndex++) {
            DxfPoint point = treePoints.get(pointIndex);
            Integer labelIndex = labelIndexByPointIndex.get(pointIndex);
            if (labelIndex == null) {
                labelIndex = nearestFreeLabel(point, labels, fallbackUsed).orElse(null);
                if (labelIndex != null) fallbackUsed.add(labelIndex);
            }
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("inventoryNumber", labelIndex == null ? null : labels.get(labelIndex).text());
            features.add(new GisFeature(features.size() + 1, new GisFeature.Point(point.x(), point.y()), attributes));
        }
        return GisLayer.of("trees", features, Optional.empty());
    }

    public static GisLayer standLayer(DxfDrawing drawing) {
        List<DxfPolyline> areas = drawing.polylines().stream().filter(line -> DxfDrawingReader.layerEndsWith(line.layer(), "-area")).toList();
        if (areas.isEmpty()) {
            areas = drawing.polylines().stream().filter(DxfPolyline::closed)
                    .filter(line -> !DxfDrawingReader.layerEndsWith(line.layer(), "-label")).toList();
        }
        List<GisFeature> features = new ArrayList<>();
        for (DxfPolyline area : areas) {
            if (area.vertices().size() < 3) continue;
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("layer", area.layer());
            features.add(new GisFeature(features.size() + 1, new GisFeature.Polygon(List.of(area.vertices())), attributes));
        }
        return GisLayer.of("stands", features, Optional.empty());
    }

    /** Globalne dopasowanie punkt -> opis przez odnosniki: najtansze pary najpierw, kazdy punkt i opis najwyzej raz. */
    private static Map<Integer, Integer> matchLabelsByLeaders(List<DxfPoint> points, List<DxfText> labels, List<DxfPolyline> leaders) {
        List<Candidate> candidates = new ArrayList<>();
        for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
            DxfPoint point = points.get(pointIndex);
            for (DxfPolyline leader : leaders) {
                double[] start = leader.vertices().getFirst();
                if (distance(start[0], start[1], point.x(), point.y()) > LEADER_START_TOLERANCE) continue;
                double[] end = leader.vertices().getLast();
                double[] beforeEnd = leader.vertices().get(leader.vertices().size() - 2);
                boolean pointsRight = end[0] - beforeEnd[0] >= 0;
                for (int textIndex = 0; textIndex < labels.size(); textIndex++) {
                    DxfText text = labels.get(textIndex);
                    double width = CHARACTER_WIDTH_PER_HEIGHT * text.height() * text.text().length();
                    if (distance(text.x(), text.y(), end[0], end[1]) > LEADER_END_SEARCH_DISTANCE + width) continue;
                    double horizontal = pointsRight ? Math.abs(text.x() - end[0]) : Math.abs(text.x() + width - end[0]);
                    double vertical = Math.abs(text.y() - end[1] + text.height() / 2);
                    candidates.add(new Candidate(vertical * VERTICAL_COST_WEIGHT + horizontal, pointIndex, textIndex));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::cost));
        Map<Integer, Integer> labelIndexByPointIndex = new LinkedHashMap<>();
        Set<Integer> usedLabels = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (labelIndexByPointIndex.containsKey(candidate.pointIndex()) || usedLabels.contains(candidate.textIndex())) continue;
            labelIndexByPointIndex.put(candidate.pointIndex(), candidate.textIndex());
            usedLabels.add(candidate.textIndex());
        }
        return labelIndexByPointIndex;
    }

    private static Optional<Integer> nearestFreeLabel(DxfPoint point, List<DxfText> labels, Set<Integer> usedLabels) {
        Integer best = null;
        double bestDistance = NEAREST_LABEL_DISTANCE;
        for (int textIndex = 0; textIndex < labels.size(); textIndex++) {
            if (usedLabels.contains(textIndex)) continue;
            double candidateDistance = distance(labels.get(textIndex).x(), labels.get(textIndex).y(), point.x(), point.y());
            if (candidateDistance <= bestDistance) {
                bestDistance = candidateDistance;
                best = textIndex;
            }
        }
        return Optional.ofNullable(best);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /**
     * Rysunek DXF jako warstwy podgladu (etap 31 - warstwy referencyjne projektu): jedna warstwa GIS na warstwe rysunku;
     * okregi / punkty -> punkty, zamkniete polilinie -> wielokaty, otwarte polilinie i odcinki -> linie. Teksty pominiete
     * (opisy CAD - do doprecyzowania, czy pokazywac je na mapie).
     */
    public static List<GisLayer> referenceLayers(DxfDrawing drawing) {
        Map<String, List<GisFeature>> featuresByLayer = new LinkedHashMap<>();
        for (DxfPoint point : drawing.points()) {
            addFeature(featuresByLayer, point.layer(), new GisFeature.Point(point.x(), point.y()));
        }
        for (DxfPolyline polyline : Stream.concat(drawing.polylines().stream(), drawing.lineSegments().stream()).toList()) {
            if (polyline.closed() && polyline.vertices().size() >= 3) {
                List<double[]> ring = new ArrayList<>(polyline.vertices());
                ring.add(polyline.vertices().getFirst());
                List<List<double[]>> rings = new ArrayList<>();
                rings.add(ring);
                addFeature(featuresByLayer, polyline.layer(), new GisFeature.Polygon(rings));
            } else {
                addFeature(featuresByLayer, polyline.layer(), new GisFeature.LineString(polyline.vertices()));
            }
        }
        return featuresByLayer.entrySet().stream()
                .map(entry -> GisLayer.of(entry.getKey().isBlank() ? "0" : entry.getKey(), entry.getValue(), Optional.empty()))
                .toList();
    }

    private static void addFeature(Map<String, List<GisFeature>> featuresByLayer, String layer, GisFeature.Geometry geometry) {
        List<GisFeature> features = featuresByLayer.computeIfAbsent(layer, ignored -> new ArrayList<>());
        features.add(new GisFeature(features.size() + 1, geometry, Map.of()));
    }
}
