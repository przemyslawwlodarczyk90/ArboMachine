package com.example.arbomachine.domain.treephoto.exception;

/**
 * Plik nie jest obslugiwanym zdjeciem (HTTP 415): zadeklarowany typ spoza listy albo zawartosc pliku
 * nie odpowiada zadeklarowanemu typowi (np. dowolny plik z nazwa konczaca sie na .jpg).
 */
public class UnsupportedTreePhotoTypeException extends RuntimeException {
    public UnsupportedTreePhotoTypeException(String message) {
        super(message);
    }
}
