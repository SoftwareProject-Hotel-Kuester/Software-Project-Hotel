package com.hsbi.hotelverwaltung.domain;

/**
 * Reinigungsstatus eines Zimmers (laut Domänen-Klassendiagramm).
 * Wird von der täglichen Reinigung (UC25) und beim Check-in (UC22) genutzt.
 */
public enum Reinigungsstatus {

    GEREINIGT("gereinigt"),
    ZU_REINIGEN("zu reinigen"),
    NICHT_REINIGEN("nicht reinigen");

    private final String bezeichnung;

    Reinigungsstatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in Tabellen und Auswahllisten. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}
