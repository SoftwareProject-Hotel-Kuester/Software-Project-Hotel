package com.hsbi.hotelverwaltung.preis;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Fenster "Neuen Preis festlegen". Gehört zu UC03 PflegePreise.
// Die Hotelleitung hat vorher in der Preisliste einen Eintrag gewählt (UC03, Schritt 3).
// Zimmerklasse und Belegungsart kommen aus diesem Eintrag. Eingegeben werden nur
// der neue Nachtpreis und der Tag, ab dem er gilt.
public class PreisAnlegenDialog extends Dialog {

    // Datumsformat für die Meldungen, z. B. "01.03.2027".
    private static final DateTimeFormatter DATUMSFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Geldformat für Deutschland, z. B. "120,00 €".
    private static final NumberFormat GELDFORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    // Über den Service wird der Preis geprüft und gespeichert (View → Service → Datenbank).
    private final PreisService preisService;

    // Der Eintrag, den die Hotelleitung in der Preisliste gewählt hat.
    // Daraus kommen Zimmerklasse und Belegungsart für den neuen Preis.
    private final Preis ausgewaehlterPreis;

    // Wird nach dem erfolgreichen Speichern aufgerufen, damit die Seite ihre Tabelle neu lädt.
    private final Runnable nachSpeichern;

    // Eingabefelder der Maske (UC03, Schritt 3).
    // TextField: einfaches Textfeld. Hier nur zur Anzeige, deshalb später schreibgeschützt.
    private final TextField zimmerklasse = new TextField("Zimmerklasse");
    private final TextField belegungsart = new TextField("Belegungsart");
    // BigDecimalField: Zahlenfeld für Geldbeträge, liefert direkt einen BigDecimal.
    private final BigDecimalField nachtpreis = new BigDecimalField("Nachtpreis (€)");
    // DatePicker: Datumsfeld, das beim Anklicken einen Kalender öffnet.
    private final DatePicker gueltigAb = new DatePicker("Gültig ab");

    // Baut das Fenster auf (UC03, Schritt 3).
    // Bekommt: den PreisService, den gewählten Eintrag und die Methode, die nach dem Speichern laufen soll.
    // Gibt nichts zurück. Geöffnet wird das Fenster erst mit open().
    public PreisAnlegenDialog(PreisService preisService, Preis ausgewaehlterPreis, Runnable nachSpeichern) {
        this.preisService = preisService;
        this.ausgewaehlterPreis = ausgewaehlterPreis;
        this.nachSpeichern = nachSpeichern;

        setHeaderTitle("Neuen Preis festlegen");

        // Schritt 1: Klasse und Belegungsart aus dem gewählten Eintrag nur anzeigen.
        // setReadOnly(true): Der Wert ist sichtbar, kann aber nicht geändert werden.
        zimmerklasse.setValue(ausgewaehlterPreis.getZimmerklasse().getBezeichnung());
        zimmerklasse.setReadOnly(true);
        belegungsart.setValue(ausgewaehlterPreis.getBelegungsart().getBezeichnung());
        belegungsart.setReadOnly(true);

        // Schritt 2: Nachtpreis vorbereiten. Das Sternchen zeigt: Pflichtfeld.
        // Als Hilfe steht der bisherige Preis darunter.
        nachtpreis.setRequiredIndicatorVisible(true);
        nachtpreis.setHelperText("Bisher: " + GELDFORMAT.format(ausgewaehlterPreis.getNachtpreis()));

        // Schritt 3: Datumsfeld vorbereiten.
        // setLocale(Locale.GERMANY): Datum im deutschen Format, z. B. 01.03.2027.
        // setMin(heute): Im Kalender ist kein Tag vor heute wählbar,
        // weil ein Preis für die Vergangenheit keinen Sinn ergibt.
        gueltigAb.setLocale(Locale.GERMANY);
        gueltigAb.setMin(LocalDate.now());
        gueltigAb.setRequiredIndicatorVisible(true);

        // Schritt 4: Felder in ein Formular setzen.
        FormLayout formular = new FormLayout(zimmerklasse, belegungsart, nachtpreis, gueltigAb);
        add(formular);

        // Schritt 5: Buttons unten im Fenster.
        // "Abbrechen" schließt das Fenster, ohne etwas zu speichern.
        Button abbrechen = new Button("Abbrechen", klick -> close());
        // "Bestätigen" schließt UC03, Schritt 3 ab.
        Button bestaetigen = new Button("Bestätigen", klick -> bestaetigen());
        bestaetigen.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(abbrechen, bestaetigen);
    }

    // Wird beim Klick auf "Bestätigen" aufgerufen (UC03, Schritte 3 und 4).
    // Prüft die Eingaben und ob am neuen Startdatum schon ein alter Preis gilt.
    // Gibt es keinen, wird sofort gespeichert. Sonst kommt erst die Rückfrage aus E1.
    // Bekommt: nichts (die Werte stehen in den Feldern). Gibt nichts zurück.
    private void bestaetigen() {
        // Schritt 1: Sind Nachtpreis und Datum ausgefüllt?
        // Ein leeres Feld liefert null. Ohne diese Prüfung gäbe es später einen Absturz.
        if (nachtpreis.getValue() == null || gueltigAb.getValue() == null) {
            zeigeFehler("Bitte alle Felder ausfüllen.");
            return;
        }

        // Schritt 2: Liegt das Datum in der Vergangenheit?
        // Im Kalender sind diese Tage ausgegraut. Man kann das Datum aber auch eintippen,
        // oder das Fenster war über Nacht offen. Deshalb prüfen wir hier noch einmal.
        if (gueltigAb.getValue().isBefore(LocalDate.now())) {
            zeigeFehler("Der neue Preis kann frühestens ab heute gelten.");
            return;
        }

        // Schritt 3: Darf der neue Preis an diesem Tag beginnen (UC03, Schritt 4)?
        // Beginnt schon ein Preis am selben Tag oder später, kommt sofort die Fehlermeldung.
        // Die Zusammenfassung (Rückfrage) erscheint dann gar nicht erst.
        try {
            preisService.pruefeNeuenStart(ausgewaehlterPreis.getZimmerklasse(),
                    ausgewaehlterPreis.getBelegungsart(), gueltigAb.getValue());
        } catch (IllegalArgumentException fehler) {
            zeigeFehler(fehler.getMessage());
            return;
        }

        // Schritt 4: Gilt am neuen Startdatum schon ein alter Preis?
        Preis alterPreis = preisService.preisAmTag(ausgewaehlterPreis.getZimmerklasse(),
                ausgewaehlterPreis.getBelegungsart(), gueltigAb.getValue());

        // Schritt 5: Kein alter Preis → direkt speichern (UC03, Schritt 5).
        // Alter Preis vorhanden → erst nachfragen, ob er am Vortag enden soll (UC03, E1 Schritt 5).
        if (alterPreis == null) {
            speichern();
        } else {
            zeigeRueckfrage(alterPreis);
        }
    }

    // Zeigt ein zweites Fenster mit dem bisherigen Preis und fragt, ob er am Vortag
    // des neuen Preises enden soll (UC03, E1 Schritte 5 und 6).
    // Bekommt: den alten Preis, der am neuen Startdatum noch gilt. Gibt nichts zurück.
    private void zeigeRueckfrage(Preis alterPreis) {
        Dialog rueckfrage = new Dialog();
        rueckfrage.setHeaderTitle("Bisherigen Preis beenden?");

        // Schritt 1: Den bisherigen Preis und den Vorschlag anzeigen (E1, Schritt 5).
        // Der alte Preis endet am Vortag, damit an jedem Tag genau ein Preis gilt.
        LocalDate letzterTag = gueltigAb.getValue().minusDays(1);
        rueckfrage.add(new Paragraph("Für " + alterPreis.getZimmerklasse().getBezeichnung() + " / "
                + alterPreis.getBelegungsart().getBezeichnung() + " gilt bisher "
                + GELDFORMAT.format(alterPreis.getNachtpreis()) + " ab "
                + alterPreis.getGueltigAb().format(DATUMSFORMAT) + "."));
        rueckfrage.add(new Paragraph("Dieser Preis endet am " + letzterTag.format(DATUMSFORMAT)
                + ". Der neue Preis gilt ab " + gueltigAb.getValue().format(DATUMSFORMAT) + ". Fortfahren?"));

        // Schritt 2: Buttons.
        // "Abbrechen" schließt nur die Rückfrage. Die Eingaben im Fenster darunter bleiben stehen.
        Button abbrechen = new Button("Abbrechen", klick -> rueckfrage.close());
        // "Bestätigen": Die Hotelleitung nimmt den Vorschlag an (E1, Schritt 6).
        Button bestaetigen = new Button("Bestätigen", klick -> {
            rueckfrage.close();
            speichern();
        });
        bestaetigen.addThemeVariants(ButtonVariant.PRIMARY);
        rueckfrage.getFooter().add(abbrechen, bestaetigen);

        rueckfrage.open();
    }

    // Speichert den neuen Preis über den PreisService (UC03, Schritt 5 bzw. E1 Schritt 7).
    // Der Service beendet dabei einen alten Preis automatisch am Vortag.
    // Bekommt: nichts (die Werte stehen in den Feldern). Gibt nichts zurück.
    private void speichern() {
        try {
            Preis neuerPreis = preisService.preisAnlegen(ausgewaehlterPreis.getZimmerklasse(),
                    ausgewaehlterPreis.getBelegungsart(), nachtpreis.getValue(), gueltigAb.getValue());

            // Erfolgsmeldung, Fenster schließen und der Seite Bescheid geben, damit sie die Tabelle neu lädt.
            Notification erfolg = Notification.show("Neuer Preis " + GELDFORMAT.format(neuerPreis.getNachtpreis())
                    + " ab " + neuerPreis.getGueltigAb().format(DATUMSFORMAT) + " gespeichert.");
            erfolg.addThemeVariants(NotificationVariant.SUCCESS);
            close();
            nachSpeichern.run();
        } catch (IllegalArgumentException fehler) {
            // Zum Beispiel: Es gibt schon einen Preis ab diesem Tag.
            // Das Fenster bleibt offen, damit die Hotelleitung das Datum ändern kann.
            zeigeFehler(fehler.getMessage());
        }
    }

    // Zeigt eine rote Fehlermeldung in der Mitte des Bildschirms, also direkt über dem Fenster.
    // So sieht die Hotelleitung den Fehler sofort und nicht unten in der Ecke.
    // Bekommt: den Text der Meldung. Gibt nichts zurück.
    private void zeigeFehler(String text) {
        // 5000 = die Meldung bleibt 5 Sekunden stehen. Position.MIDDLE = in der Mitte.
        Notification meldung = Notification.show(text, 5000, Notification.Position.MIDDLE);
        meldung.addThemeVariants(NotificationVariant.ERROR);
    }
}
