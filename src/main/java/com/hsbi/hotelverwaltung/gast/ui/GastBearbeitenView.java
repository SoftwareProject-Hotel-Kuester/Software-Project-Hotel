package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.gast.GastDaten;
import com.hsbi.hotelverwaltung.gast.GastService;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// Seite "Gästeprofil bearbeiten". Gehört zu UC12 BearbeiteGastprofil.
// Ablauf: Gast suchen → in der Trefferliste anklicken → Daten ändern → speichern.
// Wie bei UC11 prüft die View selbst keine Regeln, sondern fragt den GastService.
//
// Aufbau der Seite von oben nach unten:
//   1. Suchfeld + Knopf "Suchen"         (Schritt 2 und 3)
//   2. Trefferliste                       (Schritt 3)
//   3. Bearbeitungsbereich mit Formular   (Schritt 4 bis 7), erst sichtbar, wenn ein Gast gewählt ist
//
// @Route: Die Seite ist unter /gaeste/bearbeiten erreichbar, im Rahmen des Mitarbeiterportals.
// @RolesAllowed: Nur Mitarbeiter und Hotelleitung dürfen Gästeprofile bearbeiten.
@Route(value = "gaeste/bearbeiten", layout = MitarbeiterLayout.class)
@PageTitle("Gästeprofil bearbeiten | Hotel Sparrenhof")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class GastBearbeitenView extends VerticalLayout {

    // Deutsches Datumsformat für die Tabelle, z. B. 04.03.1992.
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Der Service mit den Regeln aus UC11 und UC12.
    private final GastService gastService;

    // ---------- 1. Suche ----------
    private final TextField suchfeld = new TextField();

    // ---------- 2. Trefferliste ----------
    private final Grid<Gast> trefferliste = new Grid<>();

    // ---------- 3. Bearbeitungsbereich ----------
    private final VerticalLayout bearbeitungsbereich = new VerticalLayout();
    private final H3 ueberschrift = new H3();
    private final GastFormular formular = new GastFormular();
    private final Paragraph kontoHinweis = new Paragraph(
            "Achtung: Dieser Gast hat ein Konto im Gästeportal. Die E-Mail-Adresse ist sein Login-Name. "
                    + "Bitte den Gast informieren, dass er sich künftig mit der neuen Adresse anmeldet.");

    // Der Gast, der gerade bearbeitet wird (null = noch keiner gewählt).
    private Gast ausgewaehlterGast;

    // Konstruktor: baut die Seite auf. Spring übergibt den GastService automatisch.
    // Bekommt: den GastService. Gibt nichts zurück.
    public GastBearbeitenView(GastService gastService) {
        this.gastService = gastService;

        add(new H2("Gästeprofil bearbeiten"), erstelleSuchleiste(), erstelleTrefferliste(), erstelleBearbeitungsbereich());
    }

    // ---------- Aufbau der Seite ----------

    // Suchfeld und Knopf nebeneinander. Enter im Suchfeld löst die Suche ebenfalls aus.
    // Bekommt nichts. Gibt zurück: die fertige Suchleiste.
    private HorizontalLayout erstelleSuchleiste() {
        suchfeld.setLabel("Gast suchen");
        suchfeld.setPlaceholder("Name, E-Mail-Adresse oder Gastnummer");
        suchfeld.setWidth("24rem");

        Button suchen = new Button("Suchen", event -> suchen());
        suchen.addThemeVariants(ButtonVariant.PRIMARY);
        suchen.addClickShortcut(Key.ENTER);

        HorizontalLayout leiste = new HorizontalLayout(suchfeld, suchen);
        leiste.setAlignItems(FlexComponent.Alignment.BASELINE);
        return leiste;
    }

    // Tabelle mit den gefundenen Gästen. Ein Klick auf eine Zeile öffnet den Gast zum Bearbeiten.
    // Bekommt nichts. Gibt zurück: die Tabelle (anfangs versteckt).
    private Grid<Gast> erstelleTrefferliste() {
        trefferliste.addColumn(Gast::getGastNr).setHeader("Gastnr.").setAutoWidth(true).setFlexGrow(0);
        trefferliste.addColumn(Gast::getVollerName).setHeader("Name").setAutoWidth(true);
        trefferliste.addColumn(gast -> datumAlsText(gast)).setHeader("Geburtsdatum").setAutoWidth(true);
        trefferliste.addColumn(Gast::getEmail).setHeader("E-Mail").setAutoWidth(true);
        trefferliste.addColumn(gast -> ortAlsText(gast)).setHeader("Ort").setFlexGrow(1);
        trefferliste.setEmptyStateText("Kein Gast gefunden.");
        trefferliste.addThemeVariants(GridVariant.ROW_STRIPES);
        trefferliste.setAllRowsVisible(true);

        // Schritt 3: Mitarbeiter wählt den Gast aus.
        trefferliste.addItemClickListener(event -> gastAuswaehlen(event.getItem()));

        // Erst nach der ersten Suche anzeigen.
        trefferliste.setVisible(false);
        return trefferliste;
    }

    // Bereich mit Überschrift, Formular, Hinweis und Knöpfen.
    // Bekommt nichts. Gibt zurück: den Bereich (anfangs versteckt).
    private VerticalLayout erstelleBearbeitungsbereich() {
        kontoHinweis.getStyle().set("color", "#a35200");
        kontoHinweis.setVisible(false);

        // Bei jeder Änderung im E-Mail-Feld prüfen, ob der Hinweis gezeigt werden muss.
        formular.beiEmailAenderung(() -> kontoHinweisAktualisieren());

        Button speichern = new Button("Änderungen speichern", event -> speichern());
        speichern.addThemeVariants(ButtonVariant.PRIMARY);
        Button verwerfen = new Button("Verwerfen", event -> verwerfen());
        verwerfen.addThemeVariants(ButtonVariant.TERTIARY);

        bearbeitungsbereich.add(ueberschrift, formular, kontoHinweis, new HorizontalLayout(speichern, verwerfen));
        bearbeitungsbereich.setPadding(false);
        bearbeitungsbereich.setVisible(false);
        return bearbeitungsbereich;
    }

    // ---------- Ablauf ----------

    // Klick auf "Suchen" oder Enter (UC12 Schritt 3).
    // Bekommt nichts. Gibt nichts zurück.
    private void suchen() {
        try {
            List<Gast> treffer = gastService.sucheGaeste(suchfeld.getValue());
            trefferliste.setItems(treffer);
            trefferliste.setVisible(true);
        } catch (IllegalArgumentException fehler) {
            // Leeres Suchfeld → Meldung, die Liste bleibt, wie sie ist.
            zeigeFehler(fehler.getMessage());
        }
    }

    // Der Mitarbeiter hat einen Gast in der Trefferliste angeklickt (UC12 Schritt 4).
    // Der Gast wird neu geladen, damit die Maske den aktuellen Stand zeigt.
    // Bekommt: den angeklickten Gast. Gibt nichts zurück.
    private void gastAuswaehlen(Gast gast) {
        try {
            ausgewaehlterGast = gastService.ladeGast(gast.getGastNr());
        } catch (IllegalArgumentException fehler) {
            // Kann passieren, wenn der Gast inzwischen gelöscht wurde.
            zeigeFehler(fehler.getMessage());
            return;
        }
        maskeMitAusgewaehltemGastFuellen();
        bearbeitungsbereich.setVisible(true);
    }

    // Klick auf "Änderungen speichern" (UC12 Schritte 5 bis 7).
    // Bekommt nichts. Gibt nichts zurück.
    private void speichern() {
        GastDaten daten = formular.leseEingaben();
        int gastNr = ausgewaehlterGast.getGastNr();

        // Schritt 5: Eingaben prüfen (dieselben Regeln wie UC11). Fehler → Meldung, Eingaben bleiben stehen.
        try {
            gastService.pruefeEingaben(daten);
        } catch (IllegalArgumentException fehler) {
            zeigeFehler(fehler.getMessage());
            return;
        }

        // Schritt 6: Gehört die E-Mail einem anderen Benutzer? → E1, Dialog mit dem anderen Profil.
        Optional<Benutzer> andererBenutzer = gastService.findeAnderenBenutzerMitEmail(gastNr, daten.email());
        if (andererBenutzer.isPresent()) {
            EmailVergebenDialog dialog = new EmailVergebenDialog(andererBenutzer.get(), () -> verwerfen());
            dialog.open();
            return;
        }

        // Schritt 7: Speichern, Erfolg melden und die Seite für den nächsten Gast zurücksetzen.
        try {
            Gast gespeichert = gastService.gastprofilBearbeiten(gastNr, daten);
            zeigeErfolg("Änderungen für " + gespeichert.getVollerName()
                    + " (Gastnummer " + gastNr + ") gespeichert.");
            sucheZuruecksetzen();
        } catch (IllegalArgumentException fehler) {
            // Eingaben bleiben stehen, damit der Mitarbeiter sie korrigieren kann.
            zeigeFehler(fehler.getMessage());
        }
    }

    // Klick auf "Verwerfen" oder "Änderung verwerfen" im E1-Dialog.
    // Lädt den gespeicherten Stand neu, alle Eingaben seit dem letzten Speichern gehen verloren.
    // Bekommt nichts. Gibt nichts zurück.
    private void verwerfen() {
        ausgewaehlterGast = gastService.ladeGast(ausgewaehlterGast.getGastNr());
        maskeMitAusgewaehltemGastFuellen();
        Notification.show("Änderungen verworfen.", 3000, Notification.Position.BOTTOM_START);
    }

    // ---------- Hilfsmethoden ----------

    // Zeigt den ausgewählten Gast im Bearbeitungsbereich an.
    // Bekommt nichts. Gibt nichts zurück.
    private void maskeMitAusgewaehltemGastFuellen() {
        String konto = ausgewaehlterGast.isHatKonto() ? "mit Konto im Gästeportal" : "ohne Konto im Gästeportal";
        ueberschrift.setText("Gastnummer " + ausgewaehlterGast.getGastNr() + " · " + konto);
        formular.fuelleMit(ausgewaehlterGast);
        kontoHinweisAktualisieren();
    }

    // Zeigt den Hinweis nur, wenn der Gast ein Konto hat UND die E-Mail gerade geändert wird.
    // Bei Gästen ohne Konto ist die E-Mail nur eine Kontaktadresse, dann gibt es keinen Hinweis.
    // Bekommt nichts. Gibt nichts zurück.
    private void kontoHinweisAktualisieren() {
        if (ausgewaehlterGast == null) {
            kontoHinweis.setVisible(false);
            return;
        }
        boolean emailGeaendert = !formular.getEmail().trim().equalsIgnoreCase(ausgewaehlterGast.getEmail());
        kontoHinweis.setVisible(ausgewaehlterGast.isHatKonto() && emailGeaendert);
    }

    // Setzt die Seite nach dem Speichern auf den Anfang zurück:
    // Suchfeld leer, Trefferliste und Bearbeitungsbereich ausgeblendet, kein Gast ausgewählt.
    // Der Cursor steht danach im Suchfeld, damit der Mitarbeiter direkt den nächsten Gast suchen kann.
    // Bekommt nichts. Gibt nichts zurück.
    private void sucheZuruecksetzen() {
        ausgewaehlterGast = null;
        suchfeld.clear();
        trefferliste.setItems(List.of());
        trefferliste.setVisible(false);
        bearbeitungsbereich.setVisible(false);
        suchfeld.focus();
    }

    // Geburtsdatum als Text für die Tabelle, z. B. "04.03.1992" (oder "–", wenn keins hinterlegt ist).
    private String datumAlsText(Gast gast) {
        if (gast.getGeburtsdatum() == null) {
            return "–";
        }
        return gast.getGeburtsdatum().format(DATUM);
    }

    // Ort aus der Adresse für die Tabelle (oder "–", wenn keine Adresse hinterlegt ist).
    private String ortAlsText(Gast gast) {
        if (gast.getAdresse() == null) {
            return "–";
        }
        return gast.getAdresse().getOrt();
    }

    // Grüne Meldung unten links, verschwindet nach 5 Sekunden.
    private void zeigeErfolg(String text) {
        Notification meldung = Notification.show(text, 5000, Notification.Position.BOTTOM_START);
        meldung.addThemeVariants(NotificationVariant.SUCCESS);
    }

    // Rote Meldung in der Mitte, bleibt etwas länger stehen.
    private void zeigeFehler(String text) {
        Notification meldung = Notification.show(text, 6000, Notification.Position.MIDDLE);
        meldung.addThemeVariants(NotificationVariant.ERROR);
    }
}
