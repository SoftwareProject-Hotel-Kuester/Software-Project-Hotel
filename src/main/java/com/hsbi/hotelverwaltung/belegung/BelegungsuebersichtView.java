package com.hsbi.hotelverwaltung.belegung;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * Gehört zu UC06 (ZeigeBelegungsübersicht).
 *
 * Belegungskalender für Mitarbeiter: Zimmer als Zeilen, Tage als Spalten.
 * Gebucht, frei und gesperrt haben verschiedene Farben. Beim ersten Öffnen
 * erscheint die aktuelle Woche.
 */
@Route(value = "belegung", layout = MitarbeiterLayout.class)
@PageTitle("Belegungsübersicht")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class BelegungsuebersichtView extends VerticalLayout {

    private static final DateTimeFormatter KOPF = DateTimeFormatter.ofPattern("EE dd.MM.", Locale.GERMAN);
    private static final String GEBUCHT = "background:#e3ebff;color:#1e4fd8;";
    private static final String FREI = "background:#e3f6e9;color:#1b7a3a;";
    private static final String GESPERRT = "background:#fde7e7;color:#b3261e;";
    private static final int MAX_TAGE = 60;

    private final BelegungService belegungService;

    private final DatePicker von = new DatePicker("Von");
    private final DatePicker bis = new DatePicker("Bis");
    private final ComboBox<Belegungsart> belegungsart = new ComboBox<>("Belegungsart");
    private final ComboBox<Zimmerklasse> zimmerklasse = new ComboBox<>("Zimmerklasse");
    private final Paragraph hinweis = new Paragraph();
    private Grid<BelegungsZeile> kalender;

    public BelegungsuebersichtView(BelegungService belegungService) {
        this.belegungService = belegungService;

        belegungsart.setItems(Belegungsart.values());
        belegungsart.setItemLabelGenerator(Belegungsart::getBezeichnung);
        belegungsart.setClearButtonVisible(true);
        zimmerklasse.setItems(Zimmerklasse.values());
        zimmerklasse.setItemLabelGenerator(Zimmerklasse::getBezeichnung);
        zimmerklasse.setClearButtonVisible(true);

        // Vorbelegung: aktuelle Woche, Montag bis Sonntag
        LocalDate heute = LocalDate.now();
        von.setValue(heute.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        bis.setValue(heute.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));

        Button anzeigen = new Button("Anzeigen", klick -> aktualisieren());
        anzeigen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout filter = new HorizontalLayout(von, bis, belegungsart, zimmerklasse, anzeigen);
        filter.setDefaultVerticalComponentAlignment(Alignment.END);

        add(new H2("Belegungsübersicht"), filter, legende(), hinweis);
        aktualisieren();
    }

    /** Lädt den Kalender neu (UC06, Schritte 2 bis 4). Bei Fehler oder leerem Ergebnis bleibt die alte Ansicht (E1). */
    private void aktualisieren() {
        try {
            if (von.getValue() != null && bis.getValue() != null
                    && von.getValue().plusDays(MAX_TAGE).isBefore(bis.getValue())) {
                hinweis.setText("Bitte höchstens " + MAX_TAGE + " Tage auf einmal anzeigen.");
                return;
            }
            List<BelegungsZeile> zeilen = belegungService.erstelleKalender(
                    von.getValue(), bis.getValue(), belegungsart.getValue(), zimmerklasse.getValue());

            if (zeilen.isEmpty()) {
                // UC06 E1: Hinweis, bisherige Ansicht bleibt stehen.
                hinweis.setText("Kein Zimmer passt zum gewählten Filter.");
                return;
            }
            hinweis.setText("");
            zeigeKalender(zeilen);
        } catch (IllegalArgumentException fehler) {
            hinweis.setText(fehler.getMessage());
        }
    }

    private void zeigeKalender(List<BelegungsZeile> zeilen) {
        if (kalender != null) {
            remove(kalender);
        }
        kalender = new Grid<>();
        kalender.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        kalender.setAllRowsVisible(true);
        kalender.addColumn(zeile -> zeile.zimmer().getZimmerNr() + " · "
                        + zeile.zimmer().getBelegungsart().getBezeichnung() + ", "
                        + zeile.zimmer().getZimmerklasse().getBezeichnung())
                .setHeader("Zimmer").setAutoWidth(true).setFrozen(true);

        for (LocalDate tag = von.getValue(); !tag.isAfter(bis.getValue()); tag = tag.plusDays(1)) {
            final LocalDate spaltenTag = tag;
            kalender.addComponentColumn(zeile -> zelle(zeile, spaltenTag))
                    .setHeader(KOPF.format(tag)).setAutoWidth(true).setFlexGrow(0);
        }
        kalender.setItems(zeilen);
        add(kalender);
    }

    /** Eine farbige Zelle. Eine gebuchte Zelle zeigt den Nachnamen und öffnet beim Klick die Buchung (E2). */
    private Div zelle(BelegungsZeile zeile, LocalDate tag) {
        Zellenart art = zeile.art(tag);
        Div zelle = new Div();
        String farbe;
        switch (art) {
            case GEBUCHT -> {
                Buchung buchung = zeile.buchungen().get(tag);
                zelle.setText(buchung.getGast().getNachname());
                zelle.addClickListener(klick -> new BuchungInfoDialog(buchung).open());
                farbe = GEBUCHT + "cursor:pointer;";
            }
            case GESPERRT -> {
                zelle.setText("gesperrt");
                farbe = GESPERRT;
            }
            default -> {
                zelle.setText("frei");
                farbe = FREI;
            }
        }
        zelle.getElement().setAttribute("style",
                farbe + "padding:4px 8px;border-radius:4px;min-width:70px;text-align:center;");
        return zelle;
    }

    private HorizontalLayout legende() {
        return new HorizontalLayout(kaestchen("gebucht", GEBUCHT), kaestchen("frei", FREI),
                kaestchen("gesperrt", GESPERRT));
    }

    private Span kaestchen(String text, String farbe) {
        Span span = new Span(text);
        span.getElement().setAttribute("style", farbe + "padding:2px 10px;border-radius:4px;");
        return span;
    }
}
