package com.hsbi.hotelverwaltung.domain;

/** Belegungsart eines Zimmers (laut Domänen-Klassendiagramm). */
public enum Belegungsart {

    EINZEL("Einzelzimmer"),
    DOPPEL("Doppelzimmer"),
    FAMILIE("Familienzimmer");

    private final String bezeichnung;

    Belegungsart(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in Tabellen und Auswahllisten. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}
