package com.example.arbomachine.domain.dataexchange.export;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kalendarz prac projektu w formacie iCalendar (RFC 5545) - etap 30. Jedno wydarzenie CALODNIOWE na kazda prace
 * DO WYKONANIA (zalecona albo zaplanowana) z terminem; prace bez terminu, wykonane i anulowane sa pomijane.
 * UID stabilny (id pracy) - ponowny import do kalendarza aktualizuje wydarzenia zamiast je dublowac.
 * Bez stref czasowych (daty bez czasu) i bez subskrypcji online (plik do pobrania). Generowany recznie - bez bibliotek.
 */
@Component
public class TreeWorksIcsExporter {

    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ICS_UTC_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAXIMUM_LINE_OCTETS = 75;
    private static final Map<String, String> STATUS_LABELS = Map.of("RECOMMENDED", "Zalecenie", "PLANNED", "Zaplanowana");
    private static final Map<String, String> PRIORITY_LABELS = Map.of("LOW", "Niski", "NORMAL", "Normalny", "HIGH", "Wysoki",
            "URGENT", "Pilny");

    public ExportedFile export(ProjectExportData data) {
        List<String> lines = new ArrayList<>();
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:-//ArboMachine//Kalendarz prac V1//PL");
        lines.add("CALSCALE:GREGORIAN");
        lines.add("METHOD:PUBLISH");
        lines.add("X-WR-CALNAME:" + escapeText("ArboMachine – " + data.projectName()));
        String timestamp = ICS_UTC_TIMESTAMP.format(data.exportedAt());
        for (TreeExportData tree : data.trees()) {
            for (TreeWorkExportData work : tree.works()) {
                if (!work.isOpen() || work.plannedDate() == null) continue;
                lines.add("BEGIN:VEVENT");
                lines.add("UID:arbomachine-tree-work-" + work.workId() + "@arbomachine");
                lines.add("DTSTAMP:" + timestamp);
                lines.add("DTSTART;VALUE=DATE:" + ICS_DATE.format(work.plannedDate()));
                lines.add("DTEND;VALUE=DATE:" + ICS_DATE.format(work.plannedDate().plusDays(1)));
                lines.add("SUMMARY:" + escapeText(work.name() + " – drzewo " + tree.inventoryNumber()));
                lines.add("DESCRIPTION:" + escapeText(descriptionOf(data, tree, work)));
                lines.add("STATUS:" + ("PLANNED".equals(work.status()) ? "CONFIRMED" : "TENTATIVE"));
                lines.add("TRANSP:TRANSPARENT");
                lines.add("CATEGORIES:ArboMachine");
                if (tree.hasLocation()) {
                    lines.add("GEO:" + tree.latitude().toPlainString() + ";" + tree.longitude().toPlainString());
                }
                lines.add("END:VEVENT");
            }
        }
        lines.add("END:VCALENDAR");

        ByteArrayOutputStream ics = new ByteArrayOutputStream();
        for (String line : lines) ics.writeBytes(folded(line));
        return new ExportedFile(ics.toByteArray(), "text/calendar; charset=UTF-8",
                "arbomachine-projekt-" + data.projectId() + "-kalendarz-prac.ics");
    }

    private static String descriptionOf(ProjectExportData data, TreeExportData tree, TreeWorkExportData work) {
        StringBuilder description = new StringBuilder();
        description.append("Status: ").append(STATUS_LABELS.getOrDefault(work.status(), work.status()));
        if (work.priority() != null) description.append("\nPriorytet: ").append(PRIORITY_LABELS.getOrDefault(work.priority(), work.priority()));
        String species = tree.speciesPolish() != null ? tree.speciesPolish() : tree.speciesLatin();
        description.append("\nDrzewo: ").append(tree.inventoryNumber()).append(species == null ? "" : " (" + species + ")");
        description.append("\nProjekt: ").append(data.projectName());
        if (work.description() != null) description.append("\n\n").append(work.description());
        return description.toString();
    }

    /** Escapowanie wartosci TEXT (RFC 5545, 3.3.11): \ ; , i nowe linie. */
    static String escapeText(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    /** Linia zakonczona CRLF i zawinieta co najwyzej co 75 bajtow (RFC 5545, 3.1) bez dzielenia znakow UTF-8. */
    static byte[] folded(String line) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int lineOctets = 0;
        for (int index = 0; index < line.length(); ) {
            int codePoint = line.codePointAt(index);
            byte[] character = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
            if (lineOctets + character.length > MAXIMUM_LINE_OCTETS) {
                output.writeBytes("\r\n ".getBytes(StandardCharsets.US_ASCII));
                lineOctets = 1;
            }
            output.writeBytes(character);
            lineOctets += character.length;
            index += Character.charCount(codePoint);
        }
        output.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
        return output.toByteArray();
    }
}
