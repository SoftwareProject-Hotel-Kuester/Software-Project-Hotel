package com.hsbi.hotelverwaltung.verfuegbarkeit;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;

/**
 * Gehört zu UC04 (PrüfeVerfügbarkeit).
 *
 * Maske für Mitarbeiter: Zeitraum und optionale Filter eingeben, dann werden
 * alle freien Zimmer in einer Tabelle angezeigt.
 */
// @Route: Die Seite ist im Browser unter /verfuegbarkeit erreichbar.
// Rahmen des Mitarbeiterportals (MitarbeiterLayout).
// @RolesAllowed: Nur Mitarbeiter und Hotelleitung dürfen die Verfügbarkeit prüfen (UC04).
@Route(value = "verfuegbarkeit", layout = MitarbeiterLayout.class)
@PageTitle("Verfügbarkeit prüfen")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class VerfuegbarkeitView extends VerticalLayout {

    private final VerfuegbarkeitService verfuegbarkeitService;

    private final DatePicker anreise = new DatePicker("Anreise");
    private final DatePicker abreise = new DatePicker("Abreise");
    private final ComboBox<Belegungsart> belegungsart = new ComboBox<>("Belegungsart");
    private final ComboBox<Zimmerklasse> zimmerklasse = new ComboBox<>("Zimmerklasse");
    private final Grid<Zimmer> tabelle = new Grid<>();
    private final Paragraph hinweis = new Paragraph();

    public VerfuegbarkeitView(VerfuegbarkeitService verfuegbarkeitService) {
        this.verfuegbarkeitService = verfuegbarkeitService;

        // Auswahllisten füllen. Leer lassen bedeutet "kein Filter".
        belegungsart.setItems(Belegungsart.values());
        belegungsart.setItemLabelGenerator(Belegungsart::getBezeichnung);
        belegungsart.setClearButtonVisible(true);
        zimmerklasse.setItems(Zimmerklasse.values());
        zimmerklasse.setItemLabelGenerator(Zimmerklasse::getBezeichnung);
        zimmerklasse.setClearButtonVisible(true);

        // Vorbelegung: Anreise heute, Abreise morgen
        anreise.setValue(LocalDate.now());
        abreise.setValue(LocalDate.now().plusDays(1));

        Button suchen = new Button("Verfügbarkeit prüfen", klick -> suchen());
        suchen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formular = new HorizontalLayout(anreise, abreise, belegungsart, zimmerklasse, suchen);
        formular.setDefaultVerticalComponentAlignment(Alignment.END);

        // Spalten der Ergebnistabelle
        tabelle.addColumn(Zimmer::getZimmerNr).setHeader("Zimmer");
        tabelle.addColumn(zimmer -> zimmer.getBelegungsart().getBezeichnung()).setHeader("Belegungsart");
        tabelle.addColumn(zimmer -> zimmer.getZimmerklasse().getBezeichnung()).setHeader("Zimmerklasse");
        // Der Nachtpreis kommt aus UC03/UC05 (Preise). Bis es die Preise gibt, bleibt die Spalte leer.
        tabelle.addColumn(zimmer -> "–").setHeader("Nachtpreis");
        tabelle.setVisible(false);

        add(new H2("Verfügbarkeit prüfen"), formular, hinweis, tabelle);
    }

    /** Wird beim Klick auf den Button aufgerufen (UC04, Schritte 3 bis 5). */
    private void suchen() {
        try {
            List<Zimmer> freieZimmer = verfuegbarkeitService.findeFreieZimmer(
                    anreise.getValue(), abreise.getValue(),
                    belegungsart.getValue(), zimmerklasse.getValue());

            if (freieZimmer.isEmpty()) {
                // UC04 E1: Kein Zimmer frei. Der Mitarbeiter kann den Filter ändern.
                tabelle.setVisible(false);
                hinweis.setText("Im gewählten Zeitraum ist kein Zimmer frei. "
                        + "Bitte Zeitraum oder Filter ändern.");
            } else {
                tabelle.setItems(freieZimmer);
                tabelle.setVisible(true);
                hinweis.setText(freieZimmer.size() + " freie(s) Zimmer gefunden.");
            }
        } catch (IllegalArgumentException fehler) {
            // Ungültige Eingabe, z. B. Abreise vor Anreise: Meldung anzeigen
            tabelle.setVisible(false);
            hinweis.setText(fehler.getMessage());
        }
    }
}