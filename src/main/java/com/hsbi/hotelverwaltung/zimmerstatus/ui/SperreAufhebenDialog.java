package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.zimmerstatus.Defektmeldung;
import com.hsbi.hotelverwaltung.zimmerstatus.FacilityService;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerInfo;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerstatusException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.List;

/**
 * UC27 HebeZimmersperreAuf: zeigt alle gesperrten Zimmer mit Grund (Schritt 2).
 * Nach "Entsperren" und Bestätigung mit optionalem Vermerk (Schritte 4 und 5)
 * ist die Sperre beendet und das Zimmer steht auf "zu reinigen" (Schritt 6).
 */
class SperreAufhebenDialog extends Dialog {

    private final FacilityService facilityService;
    private final Runnable nachAenderung;
    private final VerticalLayout liste = new VerticalLayout();

    SperreAufhebenDialog(FacilityService facilityService, Runnable nachAenderung) {
        this.facilityService = facilityService;
        this.nachAenderung = nachAenderung;

        setHeaderTitle("Sperre aufheben");
        setWidth("min(40rem, 95vw)");
        liste.setPadding(false);
        add(liste);

        Button schliessen = new Button("Schließen", event -> close());
        getFooter().add(schliessen);

        ladeListe();
    }

    private void ladeListe() {
        liste.removeAll();
        List<ZimmerInfo> gesperrt = facilityService.ladeGesperrteZimmer();
        if (gesperrt.isEmpty()) {
            // E1: kein Zimmer gesperrt
            liste.add(new Paragraph("Zurzeit ist kein Zimmer gesperrt."));
            return;
        }
        for (ZimmerInfo zimmer : gesperrt) {
            Button entsperren = new Button("Entsperren", event -> bestaetigen(zimmer, facilityService, () -> {
                ladeListe(); // Zimmer aus der Liste ausblenden
                nachAenderung.run();
            }));

            Div sperrgrund = Anzeige.sperrgrund(zimmer);
            VerticalLayout text = new VerticalLayout(new H3("Zimmer " + zimmer.zimmerNr()), sperrgrund);
            text.setPadding(false);
            text.setSpacing(false);

            HorizontalLayout zeile = new HorizontalLayout(text, entsperren);
            zeile.setWidthFull();
            zeile.setAlignItems(FlexComponent.Alignment.CENTER);
            zeile.expand(text);
            liste.add(zeile);
        }
    }

    /**
     * Schritte 4 bis 6: Bestätigung mit optionalem Vermerk, dann Sperre beenden.
     * Wird auch aus dem geöffneten Zimmer heraus genutzt.
     */
    static void bestaetigen(ZimmerInfo zimmer, FacilityService facilityService, Runnable nachErfolg) {
        Dialog bestaetigung = new Dialog();
        bestaetigung.setHeaderTitle("Zimmer " + zimmer.zimmerNr() + " entsperren?");
        bestaetigung.setWidth("min(30rem, 95vw)");

        Paragraph frage = new Paragraph("Ist der Grund behoben? Danach ist das Zimmer wieder buchbar "
                + "und steht auf „zu reinigen“.");
        TextArea vermerk = new TextArea("Vermerk zur Behebung (optional)");
        vermerk.setPlaceholder("z. B. Fernseher ausgetauscht");
        vermerk.setMaxLength(Defektmeldung.MAX_LAENGE);
        vermerk.setWidthFull();
        bestaetigung.add(frage, vermerk);

        Button abbrechen = new Button("Abbrechen", event -> bestaetigung.close());
        abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
        Button ok = new Button("Sperre aufheben", event -> {
            try {
                facilityService.zimmerFreigeben(zimmer.zimmerNr(), vermerk.getValue());
                Anzeige.erfolg("Zimmer " + zimmer.zimmerNr() + " ist entsperrt und steht auf „zu reinigen“.");
                bestaetigung.close();
                nachErfolg.run();
            } catch (ZimmerstatusException e) {
                Anzeige.fehler(e.getMessage());
            }
        });
        ok.addThemeVariants(ButtonVariant.PRIMARY);
        bestaetigung.getFooter().add(abbrechen, ok);
        bestaetigung.open();
    }
}
