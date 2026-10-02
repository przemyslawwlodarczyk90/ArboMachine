package com.example.arbomachine.domain.dataexchange.gis;

/**
 * Pliku GIS nie da sie odczytac jako calosci. reason - kod do komunikatu (UNREADABLE_FILE, UNSUPPORTED_CRS,
 * NO_SHAPEFILE_IN_ZIP...), detail - np. nazwa ukladu.
 */
public class GisFileException extends RuntimeException {
    public enum Reason { UNREADABLE_FILE, UNSUPPORTED_CRS, NO_SHAPEFILE_IN_ZIP }

    private final Reason reason;
    private final String detail;

    public GisFileException(Reason reason, String detail) {
        super("GIS file rejected: " + reason + (detail == null ? "" : " (" + detail + ")"));
        this.reason = reason;
        this.detail = detail;
    }

    public Reason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }
}
