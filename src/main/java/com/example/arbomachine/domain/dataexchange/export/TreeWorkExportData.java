package com.example.arbomachine.domain.dataexchange.export;

/** Zalecenie / praca w eksporcie: nazwa, opis i kod symbolu mapy (atrybut - QGIS moze po nim stylowac). */
public record TreeWorkExportData(String name, String description, String mapSymbolCode) {}
