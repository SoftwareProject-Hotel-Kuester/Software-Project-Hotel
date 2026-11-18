package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.zimmerstatus.Defektmeldung;
import com.hsbi.hotelverwaltung.zimmerstatus.FacilityService;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerInfo;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerstatusException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextArea;

/**
 * UC26 SperreZimmer: Sperrgrund eingeben und Zimmer sperren. Ist das Zimmer schon
 * gesperrt (E1), steht der gespeicherte Sperrgrund zum Bearbeiten im Feld. Hat ein
 * Gast einen Defekt gemeldet, ist das Feld mit seiner Beschreibung vorbelegt.
 */
class SperrenDialog extends Dialog {

    private final int zimmerNr;
    private final boolean schonGesperrt;
    private final FacilityService facilityService;
    private final Runnable nachAenderung;
    private final TextArea sperrgrund = new TextArea("Sperrgrund");

    SperrenDialog(int zimmerNr, FacilityService facilityService, Runnable nachAenderung) {
        this.zimmerNr = zimmerNr;
        this.facilityService = facilityService;
        this.nachAenderung = nachAenderung;

        // Schritt 4: aktuellen Stand laden, nicht den evtl. veralteten Stand der Tabelle
        ZimmerInfo daten = facilityService.ladeSperrdaten(zimmerNr);
        this.schonGesperrt = daten.istGesperrt();

        setHeaderTitle(schonGesperrt ? "Sperrgrund ändern – Zimmer " + zimmerNr : "Zimmer " + zimmerNr + " sperren");
        setWidth("min(32rem, 95vw)");

        sperrgrund.setRequiredIndicatorVisible(true);
        sperrgrund.setErrorMessage("Der Sperrgrund ist ein Pflichtfeld.");
        sperrgrund.setMaxLength(Defektmeldung.MAX_LAENGE);
        sperrgrund.setPlaceholder("z. B. Fernseher ist defekt");
        sperrgrund.setWidthFull();
        sperrgrund.setMinHeight("8rem");
        if (daten.sperrgrund() != null) {
            sperrgrund.setValue(daten.sperrgrund());
        }
        add(sperrgrund);

        if (!schonGesperrt && daten.istBelegt()) {
            Paragraph warnung = new Paragraph("Achtung: Das Zimmer ist belegt. Bitte dem Gast über "
                    + "„Buchung ändern“ einen Zimmerwechsel wegen Defekt anbieten.");
            warnung.getStyle().set("color", "#a35200");
            add(warnung);
        }

        Button abbrechen = new Button("Abbrechen", event -> close());
        abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
        Button speichern = new Button(schonGesperrt ? "Änderung speichern" : "Zimmer sperren", event -> speichern());
        speichern.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(abbrechen, speichern);
    }

    /** Schritte 5 bis 7: Pflichtfeld prüfen, speichern, bestätigen. */
    private void speichern() {
        if (sperrgrund.getValue().isBlank()) {
            sperrgrund.setInvalid(true);
            return;
        }
        try {
            facilityService.zimmerSperren(zimmerNr, sperrgrund.getValue());
            Anzeige.erfolg(schonGesperrt
                    ? "Sperrgrund von Zimmer " + zimmerNr + " geändert."
                    : "Zimmer " + zimmerNr + " ist gesperrt.");
            close();
            nachAenderung.run();
        } catch (ZimmerstatusException e) {
            Anzeige.fehler(e.getMessage());
        }
    }
}
