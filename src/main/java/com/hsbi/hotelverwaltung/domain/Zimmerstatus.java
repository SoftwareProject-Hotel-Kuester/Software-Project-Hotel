package com.hsbi.hotelverwaltung.domain;

/** Belegungs- bzw. Sperrstatus eines Zimmers (laut Domänen-Klassendiagramm). */
public enum Zimmerstatus {

    BELEGT("belegt"),
    NICHT_BELEGT("nicht belegt"),
    GESPERRT("gesperrt");

    private final String bezeichnung;

    Zimmerstatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in Tabellen und Auswahllisten. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}
