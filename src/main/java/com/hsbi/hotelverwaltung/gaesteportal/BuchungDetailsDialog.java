package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

import java.time.LocalDate;

// Fenster mit den Details einer Buchung (UC18 Schritt 4).
// extends Dialog: ein Fenster, das sich über die Seite legt.
//
// Von hier aus kann der Gast seine Buchung stornieren (UC20).
// Der Button "Ändern" (UC19) ist schon da, funktioniert aber erst, wenn UC19 umgesetzt ist.
class BuchungDetailsDialog extends Dialog {

    // Kontaktdaten für UC20 E1. Platzhalter, die echten Daten stimmen wir im Team ab.
    private static final String HOTEL_TELEFON = "0521 12345-0";
    private static final String HOTEL_EMAIL = "rezeption@sparrenhof.test";

    // Bekommt: die Buchung, deren Details angezeigt werden sollen,
    // und das, was beim Stornieren passieren soll (kommt von MeineBuchungenView).
    BuchungDetailsDialog(Buchung buchung, Runnable beimStornieren) {
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

        // TODO UC19: Klick-Aktion für "Ändern" ergänzen.
        Button aendern = new Button("Ändern");
        aendern.setEnabled(false);

        // UC20: Stornieren geht, wenn die Buchung "gebucht" ist und die Anreise in der Zukunft liegt.
        // Bei eingecheckten Buchungen ist der Button auch aktiv, zeigt aber die Hotelkontakte (E1).
        // Bei stornierten oder vergangenen Buchungen ist er ausgegraut.
        boolean darfStornieren = buchung.kannVomGastGeaendertWerden(LocalDate.now());
        boolean istEingecheckt = buchung.getStatus() == Buchungsstatus.EINGECHECKT;

        Button stornieren = new Button("Stornieren", klick -> {
            if (istEingecheckt) {
                zeigeHotelKontakt();
            } else {
                close();
                new StornierenDialog(buchung, beimStornieren).open();
            }
        });
        stornieren.setEnabled(darfStornieren || istEingecheckt);

        Button schliessen = new Button("Schließen", klick -> close());
        getFooter().add(aendern, stornieren, schliessen);
    }

    // UC20 E1: Der Gast ist schon eingecheckt. Online stornieren geht nicht mehr,
    // deshalb zeigen wir ihm, wie er das Hotel erreicht.
    private void zeigeHotelKontakt() {
        Dialog kontakt = new Dialog();
        kontakt.setHeaderTitle("Stornieren nicht möglich");
        kontakt.add(new Paragraph("Du bist bereits eingecheckt. Bitte wende dich direkt an die Rezeption."));
        kontakt.add(new Paragraph("Telefon: " + HOTEL_TELEFON));
        kontakt.add(new Paragraph("E-Mail: " + HOTEL_EMAIL));
        kontakt.getFooter().add(new Button("OK", klick -> kontakt.close()));
        kontakt.open();
    }
}
