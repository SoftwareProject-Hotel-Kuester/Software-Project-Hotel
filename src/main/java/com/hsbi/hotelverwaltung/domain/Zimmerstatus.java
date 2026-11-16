package com.hsbi.hotelverwaltung.domain;

/** Reinigungs- bzw. Sperrstatus eines Zimmers (laut Domänen-Klassendiagramm). */
public enum Zimmerstatus {

    GEREINIGT("gereinigt"),
    ZU_REINIGEN("zu reinigen"),
    NICHT_REINIGEN("nicht reinigen"),
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
