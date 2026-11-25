package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.zimmerstatus.FacilityService;
import com.hsbi.hotelverwaltung.zimmerstatus.ReinigungService;
import com.hsbi.hotelverwaltung.zimmerstatus.Statusfilter;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerInfo;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerstatusException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
//import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.RolesAllowed;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;


@Route(value = "zimmerstatus", layout = MitarbeiterLayout.class)
@PageTitle("Zimmerstatus | Hotel Sparrenhof")
//@AnonymousAllowed // TODO nach dem Login: @RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class ZimmerstatusView extends VerticalLayout {

    private final FacilityService facilityService;
    private final ReinigungService reinigungService;

    private final Select<Statusfilter> statusfilter = new Select<>();
    private final Span kennzahlen = new Span();
    private final Span reinigungHinweis = new Span();
    private final Grid<ZimmerInfo> tabelle = new Grid<>();

    public ZimmerstatusView(FacilityService facilityService, ReinigungService reinigungService) {
        this.facilityService = facilityService;
        this.reinigungService = reinigungService;

        kennzahlen.getStyle().set("color", Anzeige.GRAU);
        add(new H2("Zimmerstatus"), erstelleWerkzeugleiste(), reinigungHinweis, kennzahlen, erstelleTabelle());
        aktualisieren();
    }

    private Component erstelleWerkzeugleiste() {
        statusfilter.setLabel("Status");
        statusfilter.setItems(Statusfilter.values());
        statusfilter.setItemLabelGenerator(Statusfilter::getBezeichnung);
        statusfilter.setValue(Statusfilter.ALLE);
        statusfilter.addValueChangeListener(event -> aktualisieren());

        Button sperreAufheben = new Button("Sperre aufheben",
                event -> new SperreAufhebenDialog(facilityService, this::aktualisieren).open());
        // UC25 sofort ausführen, statt auf 11 und 12 Uhr zu warten (zum Ausprobieren)
        Button reinigungJetzt = new Button("Reinigung jetzt ausführen", event -> reinigungAusfuehren());
        Button neuLaden = new Button("Aktualisieren", event -> aktualisieren());
        neuLaden.addThemeVariants(ButtonVariant.TERTIARY);

        HorizontalLayout leiste = new HorizontalLayout(statusfilter, sperreAufheben, reinigungJetzt, neuLaden);
        leiste.setAlignItems(FlexComponent.Alignment.BASELINE);
        leiste.getStyle().set("flex-wrap", "wrap");
        return leiste;
    }

    private Grid<ZimmerInfo> erstelleTabelle() {
        tabelle.addColumn(ZimmerInfo::zimmerNr).setHeader("Zimmer").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        tabelle.addColumn(ZimmerInfo::etage).setHeader("Etage").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        tabelle.addColumn(z -> z.zimmerklasse().getBezeichnung()).setHeader("Klasse").setAutoWidth(true);
        tabelle.addColumn(z -> z.belegungsart().getBezeichnung()).setHeader("Belegungsart").setAutoWidth(true);
        tabelle.addComponentColumn(z -> Anzeige.status(z.zimmerstatus())).setHeader("Belegung").setAutoWidth(true);
        tabelle.addComponentColumn(z -> Anzeige.status(z.reinigungsstatus())).setHeader("Reinigung").setAutoWidth(true);
        tabelle.addColumn(ZimmerInfo::sperrgrundFuerAnzeige).setHeader("Sperrgrund").setFlexGrow(1);

        // UC26: Zimmer öffnen, Sperrgrund ansehen, sperren oder entsperren
        tabelle.addItemClickListener(event ->
                new ZimmerDetailDialog(event.getItem(), facilityService, this::aktualisieren).open());
        tabelle.setEmptyStateText("Kein Zimmer passt zum gewählten Status.");
        tabelle.addThemeVariants(GridVariant.ROW_STRIPES);
        tabelle.setAllRowsVisible(true);
        tabelle.setWidthFull();
        return tabelle;
    }

    private void reinigungAusfuehren() {
        try {
            ReinigungService.Reinigungsergebnis ergebnis = reinigungService.jetztAusfuehren();
            Anzeige.erfolg("Reinigung abgeschlossen: " + ergebnis.anzahlGereinigt() + " gereinigt, "
                    + ergebnis.anzahlNeuZuReinigen() + " wieder „zu reinigen“.");
        } catch (ZimmerstatusException e) {
            Anzeige.fehler(e.getMessage());
        }
        aktualisieren();
    }

    /** Lädt Tabelle, Kennzahlen und den Hinweis zur letzten Reinigung neu. */
    private void aktualisieren() {
        reinigungHinweis.setText(reinigungService.ladeLetzteReinigung()
                .map(Anzeige::reinigungstext)
                .orElse("Bisher wurde noch keine Reinigung durchgeführt."));

        tabelle.setItems(facilityService.ladeAlleZimmer(statusfilter.getValue()));

        FacilityService.Kennzahlen zahlen = facilityService.ladeKennzahlen();
        kennzahlen.setText(zahlen.zimmer() + " Zimmer · " + zahlen.belegt() + " belegt · "
                + zahlen.zuReinigen() + " zu reinigen · " + zahlen.gesperrt() + " gesperrt");
    }
}
