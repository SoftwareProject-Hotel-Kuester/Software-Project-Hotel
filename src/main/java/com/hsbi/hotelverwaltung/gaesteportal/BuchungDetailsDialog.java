package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

// Fenster mit den Details einer Buchung (UC18 Schritt 4).
// extends Dialog: ein Fenster, das sich über die Seite legt.
//
// Die Buttons "Ändern" (UC19) und "Stornieren" (UC20) sind schon da,
// funktionieren aber erst, wenn diese Use Cases umgesetzt sind.
class BuchungDetailsDialog extends Dialog {

    // Bekommt: die Buchung, deren Details angezeigt werden sollen.
    BuchungDetailsDialog(Buchung buchung) {
        setHeaderTitle("Buchung Nr. " + buchung.getBuchungsNr());

        Zimmer zimmer = buchung.getZimmer();
        add(new Paragraph("Zimmer: " + zimmer.getZimmerNr() + ", " + zimmer.getEtage() + ". Etage"));
        add(new Paragraph("Klasse: " + zimmer.getZimmerklasse().getBezeichnung()));
        add(new Paragraph("Belegungsart: " + zimmer.getBelegungsart().getBezeichnung()));
        add(new Paragraph("Zeitraum: " + DatumUndPreisFormat.datum(buchung.getAnreise())
                + " bis " + DatumUndPreisFormat.datum(buchung.getAbreise())));
        add(new Paragraph("Nächte: " + buchung.anzahlNaechte()));
        add(new Paragraph("Gesamtpreis: " + DatumUndPreisFormat.preis(buchung.getGesamtpreis())));
        add(new Paragraph("Status: " + buchung.getStatus().getBezeichnung()));
        add(new Paragraph("Gebucht am: " + DatumUndPreisFormat.datum(buchung.getErstelltAm())));

        // TODO UC19 und UC20: Klick-Aktionen ergänzen. Aktivieren nur, wenn
        // buchung.kannVomGastGeaendertWerden(LocalDate.now()) true liefert
        // (anstehende Buchung mit Status "gebucht"). Die Regel steht in der Klasse Buchung.
        Button aendern = new Button("Ändern");
        Button stornieren = new Button("Stornieren");
        aendern.setEnabled(false);
        stornieren.setEnabled(false);

        Button schliessen = new Button("Schließen", klick -> close());
        getFooter().add(aendern, stornieren, schliessen);
    }
}
