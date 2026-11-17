package com.hsbi.hotelverwaltung.gaesteportal;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Einheitliche Formatierung von Datum und Preis für die Oberfläche. */
final class Anzeige {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Anzeige() {
    }

    static String datum(LocalDate datum) {
        return datum == null ? "" : DATUM.format(datum);
    }

    /** z. B. "360,00 €" */
    static String preis(BigDecimal betrag) {
        return betrag == null ? "" : NumberFormat.getCurrencyInstance(Locale.GERMANY).format(betrag);
    }
}
