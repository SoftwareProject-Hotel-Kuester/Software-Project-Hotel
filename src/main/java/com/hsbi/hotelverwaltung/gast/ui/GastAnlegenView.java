package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.gast.GastDaten;
import com.hsbi.hotelverwaltung.gast.GastService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

// Maske "Gästeprofil anlegen". Gehört zu UC11 LegeGastprofilAn.
// Ablauf: Mitarbeiter füllt die Felder aus → speichern() → GastService.
// Die View prüft selbst keine Regeln. Sie liest die Felder aus, fragt den Service
// und zeigt dessen Antwort (Erfolg oder Fehlermeldung) an.
//
// @Route: Die Seite ist im Browser unter /gaeste/neu erreichbar.
// @AnonymousAllowed: vorläufig darf jeder die Seite öffnen, weil es noch keinen Login gibt.

//@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
@Route(value = "gaeste/neu", layout = MitarbeiterLayout.class)
@PageTitle("Gästeprofil anlegen | Hotel Sparrenhof")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})

public class GastAnlegenView extends VerticalLayout {

    // Der Service mit den Regeln aus UC11.
    private final GastService gastService;

    // ---------- Eingabefelder (UC11 Schritt 1) ----------
    private final TextField vorname = new TextField("Vorname");
    private final TextField nachname = new TextField("Nachname");
    private final DatePicker geburtsdatum = new DatePicker("Geburtsdatum");
    private final EmailField email = new EmailField("E-Mail-Adresse");
    private final TextField telefon = new TextField("Telefon");
    private final TextField strasse = new TextField("Straße");
    private final TextField hausnummer = new TextField("Hausnummer");
    private final TextField plz = new TextField("PLZ");
    private final TextField ort = new TextField("Ort");
    private final TextField land = new TextField("Land");
    private final TextArea praeferenzen = new TextArea("Präferenzen");

    // Konstruktor: baut die Seite auf. Spring übergibt den GastService automatisch.
    // Bekommt: den GastService. Gibt nichts zurück.
    public GastAnlegenView(GastService gastService) {
        this.gastService = gastService;

        felderEinrichten();

        // Knöpfe unter dem Formular.
        Button anlegen = new Button("Gästeprofil anlegen", event -> speichern());
        anlegen.addThemeVariants(ButtonVariant.PRIMARY);
        Button leeren = new Button("Eingaben leeren", event -> felderLeeren());
        leeren.addThemeVariants(ButtonVariant.TERTIARY);

        // Alles untereinander auf die Seite legen.
        add(new H2("Gästeprofil anlegen"), erstelleFormular(), new HorizontalLayout(anlegen, leeren));
    }

    // Stellt die Felder ein: Pflichtfelder markieren, Hilfetexte, Datumsformat.
    // Bekommt nichts. Gibt nichts zurück.
    private void felderEinrichten() {
        // Vaadin soll die Felder NICHT selbst prüfen und rot färben (z. B. beim Leeren nach dem Speichern).
        // Alle Regeln prüft der GastService, Fehler zeigen wir nur als Meldung an.
        vorname.setManualValidation(true);
        nachname.setManualValidation(true);
        geburtsdatum.setManualValidation(true);
        email.setManualValidation(true);
        telefon.setManualValidation(true);
        strasse.setManualValidation(true);
        hausnummer.setManualValidation(true);
        plz.setManualValidation(true);
        ort.setManualValidation(true);
        land.setManualValidation(true);
        praeferenzen.setManualValidation(true);

        // Sternchen (*) an den Pflichtfeldern. Nur ein Hinweis, geprüft wird im Service.
        vorname.setRequiredIndicatorVisible(true);
        nachname.setRequiredIndicatorVisible(true);
        geburtsdatum.setRequiredIndicatorVisible(true);
        email.setRequiredIndicatorVisible(true);
        strasse.setRequiredIndicatorVisible(true);
        hausnummer.setRequiredIndicatorVisible(true);
        plz.setRequiredIndicatorVisible(true);
        ort.setRequiredIndicatorVisible(true);

        // Deutsches Datumsformat (TT.MM.JJJJ) und kein Datum in der Zukunft auswählbar.
        geburtsdatum.setLocale(Locale.GERMANY);
        geburtsdatum.setMax(LocalDate.now());

        // Hilfetexte in grau im Feld.
        email.setPlaceholder("z. B. max@beispiel.de");
        land.setPlaceholder("Deutschland");
        praeferenzen.setPlaceholder("z. B. ruhiges Zimmer, hohe Etage");
        praeferenzen.setMaxLength(255);
    }

    // Ordnet die Felder in einem Formular an, das sich der Bildschirmbreite anpasst.
    // Bekommt nichts. Gibt zurück: das fertige Formular.
    private FormLayout erstelleFormular() {
        FormLayout formular = new FormLayout();
        formular.add(vorname, nachname,
                geburtsdatum, email,
                telefon, land,
                strasse, hausnummer,
                plz, ort,
                praeferenzen);
        // Präferenzen bekommen die volle Breite (zwei Spalten).
        formular.setColspan(praeferenzen, 2);
        return formular;
    }

    // Wird beim Klick auf "Gästeprofil anlegen" aufgerufen (UC11 Schritte 2 bis 4).
    // Bekommt nichts. Gibt nichts zurück.
    private void speichern() {
        GastDaten daten = leseEingaben();

        // Schritt 2: Eingaben prüfen. Bei einem Fehler Meldung zeigen, Eingaben bleiben stehen (E2).
        try {
            gastService.pruefeEingaben(daten);
        } catch (IllegalArgumentException fehler) {
            zeigeFehler(fehler.getMessage());
            return;
        }

        // Schritt 3: Gibt es diesen Gast vielleicht schon?
        List<Gast> dubletten = gastService.findeDubletten(daten);

        if (dubletten.isEmpty()) {
            // Keine Dublette → direkt anlegen (Schritt 4).
            anlegen(daten, false);
        } else {
            // E1: Dublette gefunden → Mitarbeiter entscheidet im Dialog.
            // Bei gleicher E-Mail darf er NICHT neu anlegen (E-Mail ist eindeutig).
            boolean neuAnlegenErlaubt = !gastService.emailIstVergeben(daten.email());
            DublettenDialog dialog = new DublettenDialog(
                    dubletten,
                    neuAnlegenErlaubt,
                    gewaehlterGast -> bestehendenGastVerwenden(gewaehlterGast),
                    () -> anlegen(daten, true));
            dialog.open();
        }
    }

    // Legt den Gast über den Service an und zeigt das Ergebnis (UC11 Schritt 4).
    // Bekommt: die Eingaben und ob trotz Dublette angelegt werden soll.
    // Gibt nichts zurück.
    private void anlegen(GastDaten daten, boolean trotzDublette) {
        try {
            Gast neuerGast = gastService.gastprofilAnlegen(daten, trotzDublette);
            zeigeErfolg("Gästeprofil für " + neuerGast.getVollerName()
                    + " angelegt. Gastnummer: " + neuerGast.getGastNr());
            felderLeeren();
        } catch (IllegalArgumentException fehler) {
            // Eingaben bleiben stehen, damit der Mitarbeiter sie korrigieren kann.
            zeigeFehler(fehler.getMessage());
        }
    }

    // Der Mitarbeiter hat im Dublettendialog ein vorhandenes Profil gewählt (UC11 E1).
    // Es wird nichts gespeichert, nur die Gastnummer angezeigt.
    // Später kann die Buchung (Gruppe 3, UC08 E1) hier mit diesem Gast weitermachen.
    // Bekommt: den gewählten Gast. Gibt nichts zurück.
    private void bestehendenGastVerwenden(Gast gast) {
        zeigeErfolg("Bestehendes Gästeprofil wird verwendet: " + gast.getVollerName()
                + ", Gastnummer " + gast.getGastNr());
        felderLeeren();
    }

    // Liest alle Felder aus und packt sie in ein GastDaten-Objekt.
    // Bekommt nichts. Gibt zurück: die Eingaben.
    private GastDaten leseEingaben() {
        return new GastDaten(
                vorname.getValue(),
                nachname.getValue(),
                geburtsdatum.getValue(),   // null, wenn kein Datum gewählt wurde
                email.getValue(),
                telefon.getValue(),
                strasse.getValue(),
                hausnummer.getValue(),
                plz.getValue(),
                ort.getValue(),
                land.getValue(),
                praeferenzen.getValue());
    }

    // Leert alle Felder, z. B. nach dem erfolgreichen Anlegen.
    // Bekommt nichts. Gibt nichts zurück.
    private void felderLeeren() {
        vorname.clear();
        nachname.clear();
        geburtsdatum.clear();
        email.clear();
        telefon.clear();
        strasse.clear();
        hausnummer.clear();
        plz.clear();
        ort.clear();
        land.clear();
        praeferenzen.clear();
    }

    // Grüne Meldung unten links, verschwindet nach 5 Sekunden.
    // Bekommt: den Text. Gibt nichts zurück.
    private void zeigeErfolg(String text) {
        Notification meldung = Notification.show(text, 5000, Notification.Position.BOTTOM_START);
        meldung.addThemeVariants(NotificationVariant.SUCCESS);
    }

    // Rote Meldung in der Mitte, bleibt etwas länger stehen.
    // Bekommt: den Text. Gibt nichts zurück.
    private void zeigeFehler(String text) {
        Notification meldung = Notification.show(text, 6000, Notification.Position.MIDDLE);
        meldung.addThemeVariants(NotificationVariant.ERROR);
    }
}
