package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Adresse;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.gast.GastDaten;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.util.Locale;

// Die Eingabefelder eines Gästeprofils als eigener Baustein.
// Gehört zu UC11 LegeGastprofilAn und UC12 BearbeiteGastprofil.
//
// Warum eine eigene Klasse? Anlegen und Bearbeiten haben dieselben 11 Felder.
// So stehen sie nur an EINER Stelle, und beide Masken sehen gleich aus.
//
// extends FormLayout: Vaadin-Formular, das die Felder in zwei Spalten anordnet
// und auf schmalen Bildschirmen automatisch untereinander stellt.
public class GastFormular extends FormLayout {

    // ---------- Eingabefelder ----------
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

    // Konstruktor: stellt die Felder ein und ordnet sie an.
    // Bekommt nichts. Gibt nichts zurück.
    public GastFormular() {
        felderEinrichten();

        add(vorname, nachname,
                geburtsdatum, email,
                telefon, land,
                strasse, hausnummer,
                plz, ort,
                praeferenzen);
        // Präferenzen bekommen die volle Breite (zwei Spalten).
        setColspan(praeferenzen, 2);
    }

    // Liest alle Felder aus und packt sie in ein GastDaten-Objekt.
    // Bekommt nichts. Gibt zurück: die Eingaben.
    public GastDaten leseEingaben() {
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

    // Füllt die Felder mit den gespeicherten Daten eines Gastes (UC12, Schritt 4).
    // Bekommt: den Gast. Gibt nichts zurück.
    public void fuelleMit(Gast gast) {
        vorname.setValue(textOderLeer(gast.getVorname()));
        nachname.setValue(textOderLeer(gast.getNachname()));
        geburtsdatum.setValue(gast.getGeburtsdatum());
        email.setValue(textOderLeer(gast.getEmail()));
        telefon.setValue(textOderLeer(gast.getTelefon()));
        praeferenzen.setValue(textOderLeer(gast.getPraeferenzen()));

        // Die Adresse ist ein eigenes Objekt. Hat der Gast keine, bleiben die Felder leer.
        Adresse adresse = gast.getAdresse();
        if (adresse == null) {
            strasse.clear();
            hausnummer.clear();
            plz.clear();
            ort.clear();
            land.clear();
        } else {
            strasse.setValue(textOderLeer(adresse.getStrasse()));
            hausnummer.setValue(textOderLeer(adresse.getHausnummer()));
            plz.setValue(textOderLeer(adresse.getPlz()));
            ort.setValue(textOderLeer(adresse.getOrt()));
            land.setValue(textOderLeer(adresse.getLand()));
        }
    }

    // Leert alle Felder, z. B. nach dem erfolgreichen Anlegen.
    // Bekommt nichts. Gibt nichts zurück.
    public void leeren() {
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

    // Gibt den aktuellen Text im E-Mail-Feld zurück (z. B. für den Hinweis in UC12).
    // Bekommt nichts. Gibt zurück: den eingegebenen Text.
    public String getEmail() {
        return email.getValue();
    }

    // Meldet jede Änderung im E-Mail-Feld an die View (UC12: Hinweis bei Gästen mit Konto).
    // Bekommt: was bei einer Änderung passieren soll. Gibt nichts zurück.
    public void beiEmailAenderung(Runnable aktion) {
        email.addValueChangeListener(event -> aktion.run());
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

    // Vaadin-Textfelder dürfen nicht auf null gesetzt werden. Aus null wird deshalb "".
    // Bekommt: den Text oder null. Gibt zurück: den Text oder "".
    private String textOderLeer(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }
}
