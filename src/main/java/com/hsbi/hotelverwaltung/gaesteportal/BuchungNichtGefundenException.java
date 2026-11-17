package com.hsbi.hotelverwaltung.gaesteportal;

// Eigene Fehlerart für das Gästeportal: Die Buchung gibt es nicht
// oder sie gehört einem anderen Gast (UC18 Schritt 4, später auch UC19 und UC20).
//
// Beide Fälle bekommen absichtlich dieselbe Meldung. Sonst könnte ein Gast durch
// Ausprobieren von Buchungsnummern herausfinden, welche Buchungen es gibt.
//
// extends RuntimeException: Der Fehler muss nicht in jeder Methode mit "throws"
// angekündigt werden. Wer ihn behandeln will, fängt ihn mit try/catch ab (siehe MeineBuchungenView).
public class BuchungNichtGefundenException extends RuntimeException {

    // Bekommt: die Buchungsnummer, nach der gesucht wurde. Sie steht dann in der Fehlermeldung.
    public BuchungNichtGefundenException(Integer buchungsNr) {
        super("Buchung " + buchungsNr + " wurde nicht gefunden.");
    }
}
