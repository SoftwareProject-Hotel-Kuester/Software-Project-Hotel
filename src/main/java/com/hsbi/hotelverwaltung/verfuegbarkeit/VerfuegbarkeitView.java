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
import com.hsbi.hotelverwaltung.preis.Preis;
import com.hsbi.hotelverwaltung.preis.PreisService;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

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

    // Geldformat für Deutschland, z. B. "120,00 €".
    private static final NumberFormat GELDFORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final VerfuegbarkeitService verfuegbarkeitService;

    // Liefert Nachtpreis und Gesamtpreis für die Tabelle (UC03 und UC05).
    private final PreisService preisService;

    // Der Zeitraum der letzten Suche. Die Preise in der Tabelle werden damit berechnet.
    // Grund: Ändert jemand nach der Suche das Datum, sollen die Preise trotzdem
    // zu den angezeigten Zimmern passen und nicht zum neuen, noch nicht gesuchten Datum.
    private LocalDate gesuchteAnreise;
    private LocalDate gesuchteAbreise;

    private final DatePicker anreise = new DatePicker("Anreise");
    private final DatePicker abreise = new DatePicker("Abreise");
    private final ComboBox<Belegungsart> belegungsart = new ComboBox<>("Belegungsart");
    private final ComboBox<Zimmerklasse> zimmerklasse = new ComboBox<>("Zimmerklasse");
    private final Grid<Zimmer> tabelle = new Grid<>();
    private final Paragraph hinweis = new Paragraph();

    public VerfuegbarkeitView(VerfuegbarkeitService verfuegbarkeitService, PreisService preisService) {
        this.verfuegbarkeitService = verfuegbarkeitService;
        this.preisService = preisService;

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
        // Nachtpreis am Anreisetag (UC03) und Gesamtpreis für den ganzen Aufenthalt (UC05).
        tabelle.addColumn(this::nachtpreisText).setHeader("Nachtpreis");
        tabelle.addColumn(this::gesamtpreisText).setHeader("Gesamtpreis");
        tabelle.setVisible(false);

        add(new H2("Verfügbarkeit prüfen"), formular, hinweis, tabelle);
    }

    /** Wird beim Klick auf den Button aufgerufen (UC04, Schritte 3 bis 5). */
    private void suchen() {
        try {
            List<Zimmer> freieZimmer = verfuegbarkeitService.findeFreieZimmer(
                    anreise.getValue(), abreise.getValue(),
                    belegungsart.getValue(), zimmerklasse.getValue());

            // Zeitraum merken, damit die Preisspalten genau mit diesem Zeitraum rechnen.
            gesuchteAnreise = anreise.getValue();
            gesuchteAbreise = abreise.getValue();

            if (freieZimmer.isEmpty()) {
                // UC04 E1: Kein Zimmer frei. Der Mitarbeiter kann den Filter ändern.
                tabelle.setVisible(false);
                hinweis.setText("Im gewählten Zeitraum ist kein Zimmer frei. "
                        + "Bitte Zeitraum oder Filter ändern.");
            } else {
                tabelle.setItems(freieZimmer);
                tabelle.setVisible(true);
                // UC05, Nachbedingung: Neben Nachtpreis und Gesamtpreis wird auch die Anzahl der Nächte angezeigt.
                // ChronoUnit.DAYS.between zählt die Tage zwischen Anreise und Abreise, das sind genau die Nächte.
                long naechte = ChronoUnit.DAYS.between(gesuchteAnreise, gesuchteAbreise);
                hinweis.setText(freieZimmer.size() + " freie(s) Zimmer für " + naechte + " Nacht/Nächte gefunden.");
            }
        } catch (IllegalArgumentException fehler) {
            // Ungültige Eingabe, z. B. Abreise vor Anreise: Meldung anzeigen
            tabelle.setVisible(false);
            hinweis.setText(fehler.getMessage());
        }
    }

    // Text für die Spalte "Nachtpreis" (UC03).
    // Zeigt den Preis für eine Nacht, der am Anreisetag für die Klasse und Belegungsart des Zimmers gilt.
    // Bekommt: ein Zimmer aus der Tabelle. Gibt zurück: z. B. "120,00 €" oder "kein Preis".
    private String nachtpreisText(Zimmer zimmer) {
        Preis preis = preisService.preisAmTag(zimmer.getZimmerklasse(), zimmer.getBelegungsart(), gesuchteAnreise);

        // Hat die Hotelleitung für diesen Tag keinen Preis eingetragen, gibt es keine Zahl.
        if (preis == null) {
            return "kein Preis";
        }
        return GELDFORMAT.format(preis.getNachtpreis());
    }

    // Text für die Spalte "Gesamtpreis" (UC05 BerechnePreis).
    // Rechnet alle Nächte des gesuchten Zeitraums zusammen. Ändert sich der Preis
    // während des Aufenthalts, zählt für jede Nacht ihr eigener Preis (UC05, E1).
    // Bekommt: ein Zimmer aus der Tabelle. Gibt zurück: z. B. "370,00 €" oder "kein Preis".
    private String gesamtpreisText(Zimmer zimmer) {
        try {
            BigDecimal gesamtpreis = preisService.berechnePreis(zimmer, gesuchteAnreise, gesuchteAbreise);
            return GELDFORMAT.format(gesamtpreis);
        } catch (IllegalArgumentException fehler) {
            // UC05, E2: Für mindestens eine Nacht fehlt der Preis. Dann gibt es keinen Gesamtpreis.
            return "kein Preis";
        }
    }
}