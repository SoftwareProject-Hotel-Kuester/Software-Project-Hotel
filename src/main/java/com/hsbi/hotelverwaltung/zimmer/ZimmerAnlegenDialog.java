package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;

// Fenster "Zimmer anlegen". Gehört zu UC01 LegeZimmerAn.
// Die Hotelleitung gibt hier Zimmernummer, Etage, Belegungsart und Klasse ein
// und bestätigt. Das Speichern selbst übernimmt der ZimmerService.
//
// extends Dialog: ein Fenster, das sich über die aktuelle Seite legt.
// Es ist an keine bestimmte Seite gebunden. Jede Seite kann es mit
// new ZimmerAnlegenDialog(...).open() öffnen.
public class ZimmerAnlegenDialog extends Dialog {

    // Über den Service wird das Zimmer geprüft und gespeichert (View → Service → Datenbank).
    private final ZimmerService zimmerService;

    // Wird nach dem erfolgreichen Speichern aufgerufen, z. B. damit die Seite ihre Tabelle neu lädt.
    // Runnable ist eine Methode ohne Parameter und ohne Rückgabewert, die man weitergeben kann.
    private final Runnable nachSpeichern;

    // Eingabefelder der Maske (UC01, Schritt 3).
    // IntegerField: Eingabefeld, das nur ganze Zahlen annimmt.
    private final IntegerField zimmerNr = new IntegerField("Zimmernummer");
    private final IntegerField etage = new IntegerField("Etage");
    // ComboBox: Auswahlliste, aus der man genau einen Wert wählt.
    private final ComboBox<Belegungsart> belegungsart = new ComboBox<>("Belegungsart");
    private final ComboBox<Zimmerklasse> zimmerklasse = new ComboBox<>("Zimmerklasse");

    // Baut das Fenster mit allen Feldern und Buttons auf (UC01, Schritt 2: Maske öffnen).
    // Bekommt: den ZimmerService und die Methode, die nach dem Speichern laufen soll.
    // Gibt nichts zurück. Geöffnet wird das Fenster erst mit open().
    public ZimmerAnlegenDialog(ZimmerService zimmerService, Runnable nachSpeichern) {
        this.zimmerService = zimmerService;
        this.nachSpeichern = nachSpeichern;

        setHeaderTitle("Zimmer anlegen");

        // Schritt 1: Zahlenfelder vorbereiten.
        // Das Sternchen zeigt der Hotelleitung, dass alle Felder Pflicht sind.
        // Die Mindestwerte passen zu den Regeln im ZimmerService (Nummer > 0, Etage >= 0).
        zimmerNr.setRequiredIndicatorVisible(true);
        zimmerNr.setMin(1);
        etage.setRequiredIndicatorVisible(true);
        etage.setMin(0);
        etage.setHelperText("0 = Erdgeschoss");

        // Schritt 2: Auswahllisten mit allen Belegungsarten und Klassen füllen.
        // setItemLabelGenerator legt fest, welcher Text in der Liste steht:
        // "Doppelzimmer" statt des Enum-Namens "DOPPEL".
        belegungsart.setItems(Belegungsart.values());
        belegungsart.setItemLabelGenerator(Belegungsart::getBezeichnung);
        belegungsart.setRequiredIndicatorVisible(true);
        zimmerklasse.setItems(Zimmerklasse.values());
        zimmerklasse.setItemLabelGenerator(Zimmerklasse::getBezeichnung);
        zimmerklasse.setRequiredIndicatorVisible(true);

        // Schritt 3: Felder in ein Formular setzen.
        // FormLayout ordnet die Felder ordentlich in Spalten an.
        FormLayout formular = new FormLayout(zimmerNr, etage, belegungsart, zimmerklasse);
        add(formular);

        // Schritt 4: Buttons unten im Fenster.
        // "Abbrechen" schließt das Fenster, ohne etwas zu speichern.
        Button abbrechen = new Button("Abbrechen", klick -> close());
        // "Zimmer anlegen" bestätigt die Eingabe (UC01, Ende von Schritt 3).
        Button anlegen = new Button("Zimmer anlegen", klick -> zimmerAnlegen());
        anlegen.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(abbrechen, anlegen);
    }

    // Wird beim Klick auf "Zimmer anlegen" aufgerufen (UC01, Schritte 3 bis 5 und E1).
    // Liest die Eingaben, lässt das Zimmer vom ZimmerService prüfen und speichern
    // und zeigt eine Erfolgs- oder Fehlermeldung.
    // Bekommt: nichts (die Werte stehen in den Feldern). Gibt nichts zurück.
    private void zimmerAnlegen() {
        // Schritt 1: Sind alle Felder ausgefüllt?
        // Ein leeres Feld liefert null. Ohne diese Prüfung würde das Programm
        // beim Umwandeln von null in eine Zahl abstürzen.
        if (zimmerNr.getValue() == null || etage.getValue() == null
                || belegungsart.getValue() == null || zimmerklasse.getValue() == null) {
            Notification fehlt = Notification.show("Bitte alle Felder ausfüllen.");
            fehlt.addThemeVariants(NotificationVariant.ERROR);
            return;
        }

        try {
            // Schritt 2: Der Service prüft die Eingaben und ob die Nummer frei ist (UC01, Schritt 4),
            // und speichert das Zimmer mit dem Status "zu reinigen" (UC01, Schritt 5).
            Zimmer neuesZimmer = zimmerService.zimmerAnlegen(
                    zimmerNr.getValue(), etage.getValue(),
                    belegungsart.getValue(), zimmerklasse.getValue());

            // Schritt 3: Erfolgsmeldung anzeigen, Fenster schließen
            // und der Seite Bescheid geben, z. B. damit sie ihre Tabelle neu lädt.
            Notification erfolg = Notification.show("Zimmer " + neuesZimmer.getZimmerNr() + " wurde angelegt.");
            erfolg.addThemeVariants(NotificationVariant.SUCCESS);
            close();
            nachSpeichern.run();
        } catch (IllegalArgumentException fehler) {
            // E1 (Nummer vergeben) oder ungültige Eingabe, z. B. Etage -1:
            // Meldung des Service anzeigen. Das Fenster bleibt offen und
            // die Eingaben bleiben stehen, damit die Hotelleitung sie korrigieren kann.
            Notification meldung = Notification.show(fehler.getMessage());
            meldung.addThemeVariants(NotificationVariant.ERROR);
        }
    }
}
