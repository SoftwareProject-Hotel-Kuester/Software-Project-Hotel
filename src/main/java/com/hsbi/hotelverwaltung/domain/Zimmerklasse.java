package com.hsbi.hotelverwaltung.domain;

/** Klasse eines Zimmers (laut Domänen-Klassendiagramm). */
public enum Zimmerklasse {

    ECONOMY("Economy"),
    STANDARD("Standard"),
    PREMIUM("Premium");

    private final String bezeichnung;

    Zimmerklasse(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in Tabellen und Auswahllisten. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}
