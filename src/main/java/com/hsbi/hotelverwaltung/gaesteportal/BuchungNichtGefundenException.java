package com.hsbi.hotelverwaltung.gaesteportal;

/**
 * Die Buchung existiert nicht oder gehört nicht dem angemeldeten Gast.
 *
 * Beide Fälle bekommen absichtlich dieselbe Meldung: Sonst könnte ein Gast
 * durch Ausprobieren von Buchungsnummern herausfinden, welche Buchungen es gibt.
 */
public class BuchungNichtGefundenException extends RuntimeException {

    public BuchungNichtGefundenException(Integer buchungsNr) {
        super("Buchung " + buchungsNr + " wurde nicht gefunden.");
    }
}
