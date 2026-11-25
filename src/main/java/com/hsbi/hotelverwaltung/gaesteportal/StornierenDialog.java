package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

// Sicherheitsabfrage vor dem Stornieren (UC20 Schritt 2 und 3).
// Zeigt die wichtigsten Daten der Buchung und fragt, ob der Gast wirklich stornieren will.
//
// Der Dialog storniert nicht selbst. Er bekommt von der Seite "Meine Buchungen"
// mit, was beim Bestätigen passieren soll (Parameter beimBestaetigen).
// Runnable: ein Stück Code, das man weitergeben und später mit run() ausführen kann.
class StornierenDialog extends Dialog {

    // Bekommt: die Buchung und das, was beim Klick auf "Ja, stornieren" passieren soll.
    StornierenDialog(Buchung buchung, Runnable beimBestaetigen) {
        setHeaderTitle("Buchung Nr. " + buchung.getBuchungsNr() + " stornieren?");

        add(new Paragraph("Zimmer: " + buchung.getZimmer().getZimmerNr()));
        add(new Paragraph("Zeitraum: " + DatumUndPreisFormat.datum(buchung.getAnreise())
                + " bis " + DatumUndPreisFormat.datum(buchung.getAbreise())));
        add(new Paragraph("Gesamtpreis: " + DatumUndPreisFormat.preis(buchung.getGesamtpreis())));
        add(new Paragraph("Möchtest du diese Buchung wirklich stornieren?"));

        // Schritt 3: Gast bestätigt. Fenster schließen und Stornierung ausführen.
        Button ja = new Button("Ja, stornieren", klick -> {
            close();
            beimBestaetigen.run();
        });

        // UC20 E2: Gast bricht ab. Es wird nichts geändert.
        Button abbrechen = new Button("Abbrechen", klick -> close());

        getFooter().add(abbrechen, ja);
    }
}
