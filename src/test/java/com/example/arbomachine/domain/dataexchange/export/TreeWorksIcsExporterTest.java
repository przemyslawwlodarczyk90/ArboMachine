package com.example.arbomachine.domain.dataexchange.export;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Kalendarz prac (RFC 5545): wybor prac, wydarzenia calodniowe, escapowanie i zawijanie linii bez dzielenia UTF-8. */
class TreeWorksIcsExporterTest {

    private final TreeWorksIcsExporter exporter = new TreeWorksIcsExporter();

    private static TreeWorkExportData work(long id, String name, String description, String status, String priority, LocalDate plannedDate) {
        return new TreeWorkExportData(id, name, description, null, status, priority, plannedDate, null);
    }

    @Test
    void onlyOpenWorksWithPlannedDateBecomeAllDayEvents() {
        TreeExportData tree = new TreeExportData(7, "G-1", "lipa drobnolistna", "Tilia cordata", null, null, null, null, null, null,
                new BigDecimal("52.1000000"), new BigDecimal("21.2000000"), null, null, null, null,
                List.of(work(1, "Cięcia sanitarne", "Posusz; nad chodnikiem, pilne", "PLANNED", "URGENT", LocalDate.of(2026, 11, 15)),
                        work(2, "Kontrola", null, "RECOMMENDED", null, LocalDate.of(2026, 12, 31)),
                        work(3, "Bez terminu", null, "PLANNED", null, null),
                        work(4, "Wykonana", null, "COMPLETED", null, LocalDate.of(2026, 10, 1))),
                null, List.of());
        String ics = new String(exporter.export(new ProjectExportData(5, "Park, Zamkowy", null,
                Instant.parse("2026-10-02T08:00:00Z"), List.of(tree))).content(), StandardCharsets.UTF_8);
        String unfolded = ics.replace("\r\n ", "");

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics.split("BEGIN:VEVENT", -1)).hasSize(3);
        assertThat(unfolded).contains("UID:arbomachine-tree-work-1@arbomachine", "DTSTART;VALUE=DATE:20261115",
                "DTEND;VALUE=DATE:20261116", "SUMMARY:Cięcia sanitarne – drzewo G-1", "STATUS:CONFIRMED",
                "DTSTAMP:20261002T080000Z", "GEO:52.1000000;21.2000000", "X-WR-CALNAME:ArboMachine – Park\\, Zamkowy",
                "DESCRIPTION:Status: Zaplanowana\\nPriorytet: Pilny\\nDrzewo: G-1 (lipa drobnolistna)\\nProjekt: Park\\, Zamkowy"
                        + "\\n\\nPosusz\\; nad chodnikiem\\, pilne",
                "UID:arbomachine-tree-work-2@arbomachine", "STATUS:TENTATIVE");
        assertThat(unfolded).doesNotContain("Bez terminu", "Wykonana");
    }

    @Test
    void longLinesAreFoldedAt75OctetsWithoutSplittingPolishCharacters() {
        byte[] folded = TreeWorksIcsExporter.folded("SUMMARY:" + "żółw ".repeat(40));
        String[] lines = new String(folded, StandardCharsets.UTF_8).split("\r\n");
        assertThat(lines.length).isGreaterThan(2);
        assertThat(Arrays.stream(lines).mapToInt(line -> line.getBytes(StandardCharsets.UTF_8).length).max().orElseThrow()).isLessThanOrEqualTo(75);
        assertThat(String.join("", Arrays.stream(lines).map(line -> line.startsWith(" ") ? line.substring(1) : line).toList()))
                .isEqualTo("SUMMARY:" + "żółw ".repeat(40));
        assertThat(TreeWorksIcsExporter.escapeText("a\\b;c,d\ne")).isEqualTo("a\\\\b\\;c\\,d\\ne");
    }
}
