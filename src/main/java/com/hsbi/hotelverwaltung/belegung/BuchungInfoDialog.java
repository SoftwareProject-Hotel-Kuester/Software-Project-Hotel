package com.hsbi.hotelverwaltung.belegung;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Fenster mit den Eckdaten einer Buchung (UC06 E2): Gast, Zimmer, Zeitraum, Gesamtpreis, Status. */
class BuchungInfoDialog extends Dialog {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    BuchungInfoDialog(Buchung buchung) {
        setHeaderTitle("Buchung Nr. " + buchung.getBuchungsNr());

        Gast gast = buchung.getGast();
        add(new Paragraph("Gast: " + gast.getVorname() + " " + gast.getNachname()));
        add(new Paragraph("Zimmer: " + buchung.getZimmer().getZimmerNr()));
        add(new Paragraph("Zeitraum: " + DATUM.format(buchung.getAnreise())
                + " bis " + DATUM.format(buchung.getAbreise())));
        String preis = buchung.getGesamtpreis() == null ? "–"
                : NumberFormat.getCurrencyInstance(Locale.GERMANY).format(buchung.getGesamtpreis());
        add(new Paragraph("Gesamtpreis: " + preis));
        add(new Paragraph("Status: " + buchung.getStatus().getBezeichnung()));

        getFooter().add(new Button("Schließen", klick -> close()));
    }
}
