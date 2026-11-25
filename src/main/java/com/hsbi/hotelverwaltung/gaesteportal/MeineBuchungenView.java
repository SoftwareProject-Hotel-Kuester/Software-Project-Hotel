package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Seite "Meine Buchungen" (UC18 ZeigeEigeneBuchungen).
// Der Gast sieht seine Buchungen mit Zeitraum, Zimmer, Preis und Status.
// Ein Klick auf eine Zeile öffnet die Details.
//
// @Route: Die Seite ist unter http://localhost:8080/meine-buchungen erreichbar.
//   layout = GaesteportalLayout.class: Die Seite erscheint im Rahmen des Gästeportals
//   (Kopfzeile mit Abmelden oben, Menü links).
// @PageTitle: Text im Browser-Tab.
// @RolesAllowed("GAST"): Nur angemeldete Gäste dürfen die Seite öffnen.
//   Wer nicht angemeldet ist, landet automatisch auf der Login-Seite.
// extends VerticalLayout: Die Seite ordnet ihre Bestandteile untereinander an.
@Route(value = "meine-buchungen", layout = GaesteportalLayout.class)
@PageTitle("Meine Buchungen | Hotel Sparrenhof")
@RolesAllowed("GAST")
public class MeineBuchungenView extends VerticalLayout {

    private final GaesteportalService gaesteportalService;

    // Konstruktor: Wird jedes Mal aufgerufen, wenn ein angemeldeter Gast die Seite öffnet.
    // Spring übergibt AktuellerGast und GaesteportalService automatisch (Dependency Injection).
    public MeineBuchungenView(AktuellerGast aktuellerGast, GaesteportalService gaesteportalService) {
        this.gaesteportalService = gaesteportalService;

        add(new H2("Meine Buchungen"));

        // Schritt 1: Wer ist angemeldet? AktuellerGast fragt dafür den Login (Spring Security).
        // Leer ist das Ergebnis nur, wenn es zum angemeldeten Konto kein Gästeprofil gibt.
        // Dann zeigen wir einen Hinweis, statt dass die Seite abstürzt.
        Optional<Gast> gast = aktuellerGast.get();
        if (gast.isEmpty()) {
            add(new Paragraph("Bitte melde dich an, um deine Buchungen zu sehen."));
            return;
        }

        // Schritt 2: Buchungen über den Service laden. Er liefert nur die Buchungen dieses Gastes,
        // fertig sortiert (anstehende zuerst).
        List<Buchung> buchungen = gaesteportalService.eigeneBuchungen(gast.get(), LocalDate.now());

        // Schritt 3: Je nach Ergebnis Hinweis oder Tabelle anzeigen.
        if (buchungen.isEmpty()) {
            zeigeHinweisKeineBuchung();
        } else {
            zeigeTabelle(gast.get(), buchungen);
        }
    }

    // UC18 E1: Der Gast hat noch nichts gebucht. Er bekommt einen Button zur Zimmersuche.
    // Die Zimmersuche (UC16) baut Gruppe 2. Bis es sie gibt, führt der Button noch ins Leere.
    private void zeigeHinweisKeineBuchung() {
        Button zurSuche = new Button("Zimmer suchen",
                klick -> UI.getCurrent().navigate(GaesteportalLayout.ROUTE_ZIMMERSUCHE));
        add(new Paragraph("Du hast noch keine Buchung."), zurSuche);
    }

    // UC18 Schritt 2 und 3: Tabelle mit allen Buchungen des Gastes.
    private void zeigeTabelle(Gast gast, List<Buchung> buchungen) {
        // Grid = Tabelle. false: Spalten werden nicht automatisch erzeugt, wir legen sie selbst fest.
        Grid<Buchung> tabelle = new Grid<>(Buchung.class, false);

        // Jede Spalte bekommt eine Buchung und gibt den Text für die Zelle zurück.
        tabelle.addColumn(buchung -> buchung.getBuchungsNr()).setHeader("Nr.");
        tabelle.addColumn(buchung -> buchung.getZimmer().getKurzbeschreibung()).setHeader("Zimmer");
        tabelle.addColumn(buchung -> DatumUndPreisFormat.datum(buchung.getAnreise())).setHeader("Anreise");
        tabelle.addColumn(buchung -> DatumUndPreisFormat.datum(buchung.getAbreise())).setHeader("Abreise");
        tabelle.addColumn(buchung -> buchung.anzahlNaechte()).setHeader("Nächte");
        tabelle.addColumn(buchung -> DatumUndPreisFormat.preis(buchung.getGesamtpreis())).setHeader("Gesamtpreis");
        tabelle.addColumn(buchung -> buchung.getStatus().getBezeichnung()).setHeader("Status");

        tabelle.setItems(buchungen);
        tabelle.setAllRowsVisible(true);

        // UC18 Schritt 4: Klick auf eine Zeile öffnet die Details.
        // Die Buchung wird dafür über den Service neu geladen. Der prüft, ob sie dem Gast gehört.
        tabelle.addItemClickListener(klick -> {
            try {
                Buchung buchung = gaesteportalService.eigeneBuchung(gast, klick.getItem().getBuchungsNr());
                new BuchungDetailsDialog(buchung).open();
            } catch (BuchungNichtGefundenException fehler) {
                Notification.show(fehler.getMessage());
            }
        });

        add(new Paragraph("Klicke auf eine Buchung, um die Details zu sehen."), tabelle);
    }
}
