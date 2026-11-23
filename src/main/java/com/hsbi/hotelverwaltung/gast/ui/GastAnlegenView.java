package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.gast.GastDaten;
import com.hsbi.hotelverwaltung.gast.GastService;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

// Maske "Gästeprofil anlegen". Gehört zu UC11 LegeGastprofilAn.
// Ablauf: Mitarbeiter füllt die Felder aus → speichern() → GastService.
// Die View prüft selbst keine Regeln. Sie liest die Felder aus, fragt den Service
// und zeigt dessen Antwort (Erfolg oder Fehlermeldung) an.
// Die Eingabefelder selbst stehen in GastFormular (gemeinsam mit UC12).
//
// @Route: Die Seite ist im Browser unter /gaeste/neu erreichbar, im Rahmen des Mitarbeiterportals.
// @RolesAllowed: Nur Mitarbeiter und Hotelleitung dürfen Gäste anlegen.
@Route(value = "gaeste/neu", layout = MitarbeiterLayout.class)
@PageTitle("Gästeprofil anlegen | Hotel Sparrenhof")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class GastAnlegenView extends VerticalLayout {

    // Der Service mit den Regeln aus UC11.
    private final GastService gastService;

    // Die Eingabefelder (UC11 Schritt 1).
    private final GastFormular formular = new GastFormular();

    // Konstruktor: baut die Seite auf. Spring übergibt den GastService automatisch.
    // Bekommt: den GastService. Gibt nichts zurück.
    public GastAnlegenView(GastService gastService) {
        this.gastService = gastService;

        // Knöpfe unter dem Formular.
        Button anlegen = new Button("Gästeprofil anlegen", event -> speichern());
        anlegen.addThemeVariants(ButtonVariant.PRIMARY);
        Button leeren = new Button("Eingaben leeren", event -> formular.leeren());
        leeren.addThemeVariants(ButtonVariant.TERTIARY);

        // Alles untereinander auf die Seite legen.
        add(new H2("Gästeprofil anlegen"), formular, new HorizontalLayout(anlegen, leeren));
    }

    // Wird beim Klick auf "Gästeprofil anlegen" aufgerufen (UC11 Schritte 2 bis 4).
    // Bekommt nichts. Gibt nichts zurück.
    private void speichern() {
        GastDaten daten = formular.leseEingaben();

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
            formular.leeren();
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
        formular.leeren();
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
