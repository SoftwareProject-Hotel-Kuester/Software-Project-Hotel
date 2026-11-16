package com.hsbi.hotelverwaltung.domain;

/** Status einer Buchung (laut Domänen-Klassendiagramm). */
public enum Buchungsstatus {

    GEBUCHT("gebucht"),
    EINGECHECKT("eingecheckt"),
    AUSGECHECKT("ausgecheckt"),
    STORNIERT("storniert");

    private final String bezeichnung;

    Buchungsstatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in Tabellen und Auswahllisten. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}
