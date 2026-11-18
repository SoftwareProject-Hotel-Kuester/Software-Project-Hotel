package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.zimmerstatus.FacilityService;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Ein geöffnetes Zimmer mit allen Daten. Bei gesperrten Zimmern steht der Sperrgrund
 * darunter (UC26, Schritt 7). Von hier aus geht es zum Sperren (UC26) und
 * Entsperren (UC27).
 */
class ZimmerDetailDialog extends Dialog {

    ZimmerDetailDialog(ZimmerInfo zimmer, FacilityService facilityService, Runnable nachAenderung) {
        setHeaderTitle("Zimmer " + zimmer.zimmerNr());
        setWidth("min(32rem, 95vw)");

        VerticalLayout daten = new VerticalLayout(
                zeile("Etage", new Span(zimmer.etage() + ". Etage")),
                zeile("Klasse", new Span(zimmer.zimmerklasse().getBezeichnung())),
                zeile("Belegungsart", new Span(zimmer.belegungsart().getBezeichnung())),
                zeile("Belegung", Anzeige.status(zimmer.zimmerstatus())),
                zeile("Reinigung", Anzeige.status(zimmer.reinigungsstatus())));
        daten.setPadding(false);
        daten.setSpacing(false);
        daten.getStyle().set("gap", "0.4rem");
        add(daten);

        if (zimmer.istGesperrt()) {
            Div sperrgrund = Anzeige.sperrgrund(zimmer);
            sperrgrund.getStyle().set("margin-top", "0.75rem");
            add(sperrgrund);
        }

        Button schliessen = new Button("Schließen", event -> close());
        schliessen.addThemeVariants(ButtonVariant.TERTIARY);

        Button sperren = new Button(zimmer.istGesperrt() ? "Sperrgrund ändern" : "Zimmer sperren", event -> {
            close();
            new SperrenDialog(zimmer.zimmerNr(), facilityService, nachAenderung).open();
        });

        if (zimmer.istGesperrt()) {
            Button entsperren = new Button("Sperre aufheben", event -> {
                close();
                SperreAufhebenDialog.bestaetigen(zimmer, facilityService, nachAenderung);
            });
            entsperren.addThemeVariants(ButtonVariant.PRIMARY);
            getFooter().add(schliessen, sperren, entsperren);
        } else {
            sperren.addThemeVariants(ButtonVariant.PRIMARY);
            getFooter().add(schliessen, sperren);
        }
    }

    private static HorizontalLayout zeile(String bezeichnung, Component wert) {
        Span label = new Span(bezeichnung);
        label.getStyle().set("min-width", "8rem").set("color", Anzeige.GRAU);
        HorizontalLayout zeile = new HorizontalLayout(label, wert);
        zeile.setAlignItems(FlexComponent.Alignment.BASELINE);
        return zeile;
    }
}
