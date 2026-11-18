package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.format.DateTimeFormatter;
import java.util.List;

// Fenster "Zimmer bearbeiten". Gehört zu UC02 ÄndereZimmer.
// Es sieht aus wie das Fenster "Zimmer anlegen", nur sind die Werte des Zimmers schon eingetragen.
// Laut UC02 darf die Hotelleitung nur Belegungsart, Klasse und Ausstattung ändern.
// Zimmernummer und Etage werden deshalb nur angezeigt.
public class ZimmerBearbeitenDialog extends Dialog {

    // Datumsformat für die Liste der Buchungen, z. B. "18.11.2026".
    private static final DateTimeFormatter DATUMSFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Über den Service wird das Zimmer geprüft und gespeichert (View → Service → Datenbank).
    private final ZimmerService zimmerService;

    // Das Zimmer, das bearbeitet wird. Daraus kommen die Werte, die schon eingetragen sind.
    private final Zimmer zimmer;

    // Wird nach dem erfolgreichen Speichern aufgerufen, damit die Seite ihre Tabelle neu lädt.
    private final Runnable nachSpeichern;

    // Eingabefelder der Maske (UC02, Schritt 4).
    private final IntegerField zimmerNr = new IntegerField("Zimmernummer");
    private final IntegerField etage = new IntegerField("Etage");
    private final ComboBox<Belegungsart> belegungsart = new ComboBox<>("Belegungsart");
    private final ComboBox<Zimmerklasse> zimmerklasse = new ComboBox<>("Zimmerklasse");
    // TextArea: Eingabefeld für längeren Text über mehrere Zeilen.
    private final TextArea ausstattung = new TextArea("Ausstattung");

    // Baut das Fenster auf und trägt die aktuellen Werte des Zimmers ein (UC02, Schritt 4).
    // Bekommt: den ZimmerService, das ausgewählte Zimmer und die Methode, die nach dem Speichern laufen soll.
    // Gibt nichts zurück. Geöffnet wird das Fenster erst mit open().
    public ZimmerBearbeitenDialog(ZimmerService zimmerService, Zimmer zimmer, Runnable nachSpeichern) {
        this.zimmerService = zimmerService;
        this.zimmer = zimmer;
        this.nachSpeichern = nachSpeichern;

        setHeaderTitle("Zimmer " + zimmer.getZimmerNr() + " bearbeiten");

        // Schritt 1: Zimmernummer und Etage nur anzeigen.
        // setReadOnly(true): Das Feld zeigt den Wert, man kann ihn aber nicht ändern.
        // Die Nummer ist der Schlüssel, an dem die Buchungen hängen, und UC02 erlaubt hier keine Änderung.
        zimmerNr.setValue(zimmer.getZimmerNr());
        zimmerNr.setReadOnly(true);
        etage.setValue(zimmer.getEtage());
        etage.setReadOnly(true);

        // Schritt 2: Auswahllisten füllen und den aktuellen Wert vorauswählen.
        belegungsart.setItems(Belegungsart.values());
        belegungsart.setItemLabelGenerator(Belegungsart::getBezeichnung);
        belegungsart.setRequiredIndicatorVisible(true);
        belegungsart.setValue(zimmer.getBelegungsart());
        zimmerklasse.setItems(Zimmerklasse.values());
        zimmerklasse.setItemLabelGenerator(Zimmerklasse::getBezeichnung);
        zimmerklasse.setRequiredIndicatorVisible(true);
        zimmerklasse.setValue(zimmer.getZimmerklasse());

        // Schritt 3: Ausstattung eintragen. Sie ist freiwillig und kann leer sein.
        // Ein Textfeld darf nicht auf null gesetzt werden, deshalb nur setzen, wenn es einen Text gibt.
        ausstattung.setPlaceholder("z. B. Balkon, Minibar");
        if (zimmer.getAusstattung() != null) {
            ausstattung.setValue(zimmer.getAusstattung());
        }

        // Schritt 4: Felder in ein Formular setzen.
        FormLayout formular = new FormLayout(zimmerNr, etage, belegungsart, zimmerklasse, ausstattung);
        // Die Ausstattung soll über beide Spalten gehen, weil sie längeren Text enthält.
        formular.setColspan(ausstattung, 2);
        add(formular);

        // Schritt 5: Buttons unten im Fenster.
        Button abbrechen = new Button("Abbrechen", klick -> close());
        // "Bestätigen" schließt UC02, Schritt 5 ab.
        Button bestaetigen = new Button("Bestätigen", klick -> bestaetigen());
        bestaetigen.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(abbrechen, bestaetigen);
    }

    // Wird beim Klick auf "Bestätigen" aufgerufen (UC02, Schritte 5 und 6).
    // Prüft die Pflichtauswahl und ob es künftige Buchungen gibt.
    // Gibt es keine, wird sofort gespeichert. Sonst kommt erst eine Rückfrage (E1/E2).
    // Bekommt: nichts (die Werte stehen in den Feldern). Gibt nichts zurück.
    private void bestaetigen() {
        // Schritt 1: Sind Belegungsart und Klasse ausgewählt?
        // Ohne sie kann später kein Preis berechnet werden (UC05).
        if (belegungsart.getValue() == null || zimmerklasse.getValue() == null) {
            Notification fehlt = Notification.show("Bitte Belegungsart und Zimmerklasse auswählen.");
            fehlt.addThemeVariants(NotificationVariant.ERROR);
            return;
        }

        try {
            // Schritt 2: Künftige Buchungen des Zimmers holen (UC02, Schritt 6).
            List<Buchung> kuenftigeBuchungen = zimmerService.kuenftigeBuchungen(zimmer.getZimmerNr());

            // Schritt 3: Keine künftigen Buchungen → direkt speichern (UC02, Schritt 7).
            // Sonst die Buchungen zeigen und nachfragen (UC02, E1 Schritt 7).
            if (kuenftigeBuchungen.isEmpty()) {
                speichern();
            } else {
                zeigeRueckfrage(kuenftigeBuchungen);
            }
        } catch (IllegalArgumentException fehler) {
            // Zum Beispiel: Das Zimmer wurde inzwischen gelöscht.
            Notification meldung = Notification.show(fehler.getMessage());
            meldung.addThemeVariants(NotificationVariant.ERROR);
        }
    }

    // Zeigt ein zweites Fenster mit den künftigen Buchungen und fragt,
    // ob die Änderung trotzdem gelten soll (UC02, E1 und E2).
    // Bekommt: die Liste der künftigen Buchungen. Gibt nichts zurück.
    private void zeigeRueckfrage(List<Buchung> kuenftigeBuchungen) {
        Dialog rueckfrage = new Dialog();
        rueckfrage.setHeaderTitle("Künftige Buchungen vorhanden");

        // Schritt 1: Erklärung für die Hotelleitung.
        // Bestehende Buchungen behalten ihren Preis, weil jede Buchung ihren Gesamtpreis selbst speichert.
        rueckfrage.add(new Paragraph("Für Zimmer " + zimmer.getZimmerNr() + " gibt es künftige Buchungen. "
                + "Diese behalten ihren bisherigen Preis. Soll die Änderung trotzdem gelten?"));

        // Schritt 2: Die Buchungen als kleine Tabelle auflisten (UC02, E1 Schritt 7).
        Grid<Buchung> buchungsTabelle = new Grid<>();
        buchungsTabelle.addColumn(buchung -> buchung.getBuchungsNr()).setHeader("Buchungs-Nr.");
        buchungsTabelle.addColumn(buchung -> buchung.getGast().getVollerName()).setHeader("Gast");
        buchungsTabelle.addColumn(buchung -> buchung.getAnreise().format(DATUMSFORMAT)).setHeader("Anreise");
        buchungsTabelle.addColumn(buchung -> buchung.getAbreise().format(DATUMSFORMAT)).setHeader("Abreise");
        buchungsTabelle.setItems(kuenftigeBuchungen);
        buchungsTabelle.setAllRowsVisible(true);
        rueckfrage.add(buchungsTabelle);

        // Schritt 3: Buttons.
        // E2: Abbrechen verwirft die Eingaben. Beide Fenster schließen, das Zimmer bleibt unverändert.
        Button abbrechen = new Button("Abbrechen", klick -> {
            rueckfrage.close();
            close();
        });
        // E1: Die Hotelleitung bestätigt, die Änderung wird gespeichert.
        Button trotzdemAendern = new Button("Trotzdem ändern", klick -> {
            rueckfrage.close();
            speichern();
        });
        trotzdemAendern.addThemeVariants(ButtonVariant.PRIMARY);
        rueckfrage.getFooter().add(abbrechen, trotzdemAendern);

        rueckfrage.open();
    }

    // Speichert die Änderungen über den ZimmerService (UC02, Schritt 7 bzw. E1 Schritt 9).
    // Zeigt eine Erfolgsmeldung, schließt das Fenster und lässt die Seite ihre Tabelle neu laden.
    // Bekommt: nichts (die Werte stehen in den Feldern). Gibt nichts zurück.
    private void speichern() {
        try {
            Zimmer geaendertesZimmer = zimmerService.zimmerAendern(
                    zimmer.getZimmerNr(), belegungsart.getValue(),
                    zimmerklasse.getValue(), ausstattung.getValue());

            Notification erfolg = Notification.show("Zimmer " + geaendertesZimmer.getZimmerNr() + " wurde geändert.");
            erfolg.addThemeVariants(NotificationVariant.SUCCESS);
            close();
            nachSpeichern.run();
        } catch (IllegalArgumentException fehler) {
            // Fehlermeldung des Service anzeigen. Das Fenster bleibt offen.
            Notification meldung = Notification.show(fehler.getMessage());
            meldung.addThemeVariants(NotificationVariant.ERROR);
        }
    }
}
