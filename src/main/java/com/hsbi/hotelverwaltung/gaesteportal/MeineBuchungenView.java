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
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Optional;

/**
 * UC18 ZeigeEigeneBuchungen: Der Gast sieht seine Buchungen mit Zeitraum,
 * Zimmer, Preis und Status, anstehende zuerst. Ein Klick auf eine Zeile
 * öffnet die Details.
 */
@Route(value = "meine-buchungen", layout = GaesteportalLayout.class)
@PageTitle("Meine Buchungen | Hotel Sparrenhof")
@AnonymousAllowed // TODO: durch @RolesAllowed("GAST") ersetzen, sobald der Login fertig ist
public class MeineBuchungenView extends VerticalLayout {

    /** Route der Zimmersuche (UC16, Gruppe 2). TODO: mit Gruppe 2 abstimmen. */
    static final String ROUTE_ZIMMERSUCHE = "zimmersuche";

    private final GaesteportalService gaesteportalService;

    public MeineBuchungenView(AktuellerGast aktuellerGast, GaesteportalService gaesteportalService) {
        this.gaesteportalService = gaesteportalService;

        add(new H2("Meine Buchungen"));

        Optional<Gast> gast = aktuellerGast.get();
        if (gast.isEmpty()) {
            add(new Paragraph("Bitte melde dich an, um deine Buchungen zu sehen."));
            return;
        }

        List<Buchung> buchungen = gaesteportalService.eigeneBuchungen(gast.get());
        if (buchungen.isEmpty()) {
            zeigeHinweisKeineBuchung();
        } else {
            zeigeBuchungen(gast.get(), buchungen);
        }
    }

    /** UC18 E1: Noch keine Buchung, Verweis auf die Zimmersuche. */
    private void zeigeHinweisKeineBuchung() {
        Button zurSuche = new Button("Zimmer suchen",
                e -> UI.getCurrent().navigate(ROUTE_ZIMMERSUCHE));
        add(new Paragraph("Du hast noch keine Buchung."), zurSuche);
    }

    private void zeigeBuchungen(Gast gast, List<Buchung> buchungen) {
        Grid<Buchung> grid = new Grid<>(Buchung.class, false);
        grid.addColumn(Buchung::getBuchungsNr).setHeader("Nr.").setAutoWidth(true);
        grid.addColumn(b -> b.getZimmer().getKurzbeschreibung()).setHeader("Zimmer").setAutoWidth(true);
        grid.addColumn(b -> Anzeige.datum(b.getAnreise())).setHeader("Anreise").setAutoWidth(true);
        grid.addColumn(b -> Anzeige.datum(b.getAbreise())).setHeader("Abreise").setAutoWidth(true);
        grid.addColumn(Buchung::anzahlNaechte).setHeader("Nächte").setAutoWidth(true);
        grid.addColumn(b -> Anzeige.preis(b.getGesamtpreis())).setHeader("Gesamtpreis").setAutoWidth(true);
        grid.addColumn(b -> b.getStatus().getBezeichnung()).setHeader("Status").setAutoWidth(true);

        grid.setItems(buchungen);
        grid.setAllRowsVisible(true);

        // UC18 Schritt 3 und 4: Buchung auswählen, Details anzeigen.
        // Die Buchung wird über den Service neu geladen, der prüft, ob sie dem Gast gehört.
        grid.addItemClickListener(klick -> {
            try {
                Buchung buchung = gaesteportalService.eigeneBuchung(gast, klick.getItem().getBuchungsNr());
                new BuchungDetailsDialog(buchung, gaesteportalService.heute()).open();
            } catch (BuchungNichtGefundenException ex) {
                Notification.show(ex.getMessage());
            }
        });

        add(new Paragraph("Klicke auf eine Buchung, um die Details zu sehen."), grid);
    }
}
