package com.hsbi.hotelverwaltung.domain;

/**
 * Rolle eines Mitarbeiters. Sie legt fest, welche Bereiche der Anwendung
 * er sehen und bedienen darf. Gäste haben keine Rolle, sie sind
 * schon durch die Klasse Gast erkennbar.
 *
 * Die Hotelleitung ist technisch ein Mitarbeiter mit höheren Rechten,
 * deshalb gibt es dafür keine eigene Klasse.
 */
public enum Rolle {

    MITARBEITER("Mitarbeiter"),
    HOTELLEITUNG("Hotelleitung");

    private final String bezeichnung;

    Rolle(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /** Lesbarer Name für die Oberfläche, z. B. in einer Benutzerliste. */
    public String getBezeichnung() {
        return bezeichnung;
    }
}