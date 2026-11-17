package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.LocalDate;

/**
 * UC18 Schritt 4: Details einer Buchung. Die Knöpfe für Ändern (UC19) und
 * Stornieren (UC20) sind schon da, aber nur aktiv, wenn die Buchung noch
 * geändert werden darf.
 */
class BuchungDetailsDialog extends Dialog {

    BuchungDetailsDialog(Buchung buchung, LocalDate heute) {
        setHeaderTitle("Buchung Nr. " + buchung.getBuchungsNr());

        Zimmer zimmer = buchung.getZimmer();
        VerticalLayout inhalt = new VerticalLayout(
                zeile("Zimmer", zimmer.getZimmerNr() + ", " + zimmer.getEtage() + ". Etage"),
                zeile("Klasse", zimmer.getZimmerklasse().getBezeichnung()),
                zeile("Belegungsart", zimmer.getBelegungsart().getBezeichnung()),
                zeile("Zeitraum", Anzeige.datum(buchung.getAnreise()) + " bis " + Anzeige.datum(buchung.getAbreise())),
                zeile("Nächte", String.valueOf(buchung.anzahlNaechte())),
                zeile("Gesamtpreis", Anzeige.preis(buchung.getGesamtpreis())),
                zeile("Status", buchung.getStatus().getBezeichnung()),
                zeile("Gebucht am", Anzeige.datum(buchung.getErstelltAm())));
        inhalt.setPadding(false);
        inhalt.setSpacing(false);
        add(inhalt);

        boolean aenderbar = buchung.kannVomGastGeaendertWerden(heute);

        Button aendern = new Button("Ändern");      // TODO UC19 ÄndereEigeneBuchung
        aendern.setEnabled(false);
        Button stornieren = new Button("Stornieren"); // TODO UC20 StorniereEigeneBuchung
        stornieren.setEnabled(false);
        if (!aenderbar) {
            aendern.setTooltipText("Nur anstehende Buchungen mit Status \"gebucht\" können geändert werden.");
            stornieren.setTooltipText("Nur anstehende Buchungen mit Status \"gebucht\" können storniert werden.");
        }

        Button schliessen = new Button("Schließen", e -> close());
        getFooter().add(aendern, stornieren, schliessen);
    }

    private static Span zeile(String bezeichnung, String wert) {
        return new Span(bezeichnung + ": " + wert);
    }
}
