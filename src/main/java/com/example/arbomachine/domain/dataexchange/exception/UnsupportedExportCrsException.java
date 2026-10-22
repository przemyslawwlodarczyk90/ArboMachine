package com.example.arbomachine.domain.dataexchange.exception;

/** Etap 34: nieobslugiwany uklad wspolrzednych eksportu GIS (HTTP 400). */
public class UnsupportedExportCrsException extends RuntimeException {
    public UnsupportedExportCrsException(String message) {
        super(message);
    }
}
