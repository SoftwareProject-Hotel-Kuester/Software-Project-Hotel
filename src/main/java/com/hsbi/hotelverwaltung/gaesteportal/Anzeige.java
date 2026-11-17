package com.hsbi.hotelverwaltung.gaesteportal;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Hilfsklasse, damit Datum und Preis auf allen Seiten des Gästeportals
// gleich aussehen, z. B. "15.06.2027" und "360,00 €".
// Die Methoden sind static: Man ruft sie direkt auf, z. B. Anzeige.datum(...),
// ohne vorher ein Objekt zu erzeugen.
class Anzeige {

    private static final DateTimeFormatter DATUMSFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Bekommt: ein Datum. Gibt zurück: das Datum als Text, z. B. "15.06.2027".
    static String datum(LocalDate datum) {
        if (datum == null) {
            return "";
        }
        return DATUMSFORMAT.format(datum);
    }

    // Bekommt: einen Betrag. Gibt zurück: den Betrag in Euro, z. B. "360,00 €".
    static String preis(BigDecimal betrag) {
        if (betrag == null) {
            return "";
        }
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(betrag);
    }
}
