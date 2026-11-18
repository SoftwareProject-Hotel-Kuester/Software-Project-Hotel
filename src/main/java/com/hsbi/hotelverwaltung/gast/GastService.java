package com.hsbi.hotelverwaltung.gast;

import com.hsbi.hotelverwaltung.domain.Adresse;
import com.hsbi.hotelverwaltung.domain.Gast;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Fachregeln rund um Gästeprofile. Gehört zu UC11 LegeGastprofilAn.
// Ablauf: View → GastService → GastprofilRepository → Datenbank.
// Die View spricht nie direkt mit der Datenbank.
//
// @Service: Spring erzeugt beim Start genau ein Objekt dieser Klasse
// und gibt es jeder Klasse, die es im Konstruktor verlangt.
@Service
public class GastService {

    // Steht im Feld "Land" nichts, nehmen wir Deutschland an.
    private static final String STANDARD_LAND = "Deutschland";

    // So lang darf ein Text in der Datenbank höchstens sein (Standardlänge einer Textspalte).
    private static final int MAX_LAENGE = 255;

    // Über das Repository lesen und speichern wir Gäste in der Datenbank.
    private final GastprofilRepository gastprofilRepository;

    // Konstruktor: Spring übergibt das Repository automatisch, weil es hier als Parameter steht.
    // Bekommt: das GastprofilRepository. Gibt nichts zurück.
    public GastService(GastprofilRepository gastprofilRepository) {
        this.gastprofilRepository = gastprofilRepository;
    }

    // Legt ein neues Gästeprofil an und speichert es in der Datenbank (UC11).
    // Bekommt: die Eingaben aus der Maske und ob der Mitarbeiter trotz Dublette anlegen will.
    //          trotzDublette = true, wenn er im Dialog "Trotzdem neu anlegen" gewählt hat (E1).
    // Gibt zurück: den gespeicherten Gast. Seine Gastnummer ist danach vergeben.
    // Wirft eine IllegalArgumentException, wenn eine Eingabe ungültig ist (E2),
    // die E-Mail schon vergeben ist oder es eine Dublette gibt und trotzDublette = false ist (E1).
    //
    // @Transactional: Gast und Adresse werden zusammen gespeichert oder gar nicht.
    @Transactional
    public Gast gastprofilAnlegen(GastDaten daten, boolean trotzDublette) {
        // Schritt 2: Eingaben prüfen.
        // Wir prüfen hier noch einmal, auch wenn die View das schon getan hat.
        // So kann auch eine fehlerhafte View keine ungültigen Daten speichern.
        pruefeEingaben(daten);

        // Schritt 3: Gibt es die E-Mail schon, darf NIE ein zweites Profil entstehen.
        // Die E-Mail ist zugleich der Anmeldename und in der Datenbank eindeutig.
        if (emailIstVergeben(daten.email())) {
            throw new IllegalArgumentException(
                    "Es gibt bereits ein Gästeprofil mit der E-Mail-Adresse " + bereinigeEmail(daten.email()) + ".");
        }

        // Schritt 3: Gleicher Name und Geburtsdatum → nur anlegen, wenn der Mitarbeiter es so will (E1).
        if (!trotzDublette && !findeDubletten(daten).isEmpty()) {
            throw new IllegalArgumentException(
                    "Es gibt bereits einen Gast mit diesem Namen und Geburtsdatum.");
        }

        // Schritt 4: Gast erzeugen. Der Konstruktor braucht E-Mail, Vorname, Nachname, Geburtsdatum.
        Gast neuerGast = new Gast(
                bereinigeEmail(daten.email()),
                daten.vorname().trim(),
                daten.nachname().trim(),
                daten.geburtsdatum());

        // Optionale Angaben: Leere Felder speichern wir als null, nicht als "".
        neuerGast.setTelefon(leerWirdNull(daten.telefon()));
        neuerGast.setPraeferenzen(leerWirdNull(daten.praeferenzen()));

        // Ein Mitarbeiter legt das Profil an, der Gast hat also kein Konto im Gästeportal
        // und kein Passwort. Er kann sich deshalb nicht selbst anmelden.
        neuerGast.setHatKonto(false);

        // Die Anschrift ist eine eigene Tabelle. Sie wird mit dem Gast zusammen gespeichert.
        neuerGast.setAdresse(erstelleAdresse(daten));

        // Gast in der Datenbank speichern und zurückgeben.
        // Erst beim Speichern vergibt die Datenbank die Gastnummer.
        return gastprofilRepository.save(neuerGast);
    }

    // Prüft, ob alle Eingaben für ein Gästeprofil gültig sind (UC11, Schritt 2).
    // Öffentlich, damit die View vor der Dublettensuche prüfen kann.
    // Bekommt: die Eingaben aus der Maske.
    // Gibt nichts zurück. Wirft eine IllegalArgumentException mit einer Meldung
    // für den Mitarbeiter, sobald die erste Eingabe ungültig ist (E2).
    public void pruefeEingaben(GastDaten daten) {
        // Name: Ohne Namen kann der Mitarbeiter den Gast später nicht wiederfinden.
        pruefePflichtfeld(daten.vorname(), "Bitte einen Vornamen eingeben.");
        pruefePflichtfeld(daten.nachname(), "Bitte einen Nachnamen eingeben.");

        // Geburtsdatum: Pflicht, weil wir damit gleichnamige Gäste unterscheiden.
        if (daten.geburtsdatum() == null) {
            throw new IllegalArgumentException("Bitte ein Geburtsdatum eingeben.");
        }
        if (daten.geburtsdatum().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Das Geburtsdatum darf nicht in der Zukunft liegen.");
        }

        // E-Mail: Pflicht, weil sie in der Datenbank nicht leer sein darf.
        pruefePflichtfeld(daten.email(), "Bitte eine E-Mail-Adresse eingeben.");
        if (!istGueltigeEmail(daten.email())) {
            throw new IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben, z. B. max@beispiel.de.");
        }

        // Anschrift: Laut US04 gibt der Mitarbeiter die Anschrift ein.
        pruefePflichtfeld(daten.strasse(), "Bitte eine Straße eingeben.");
        pruefePflichtfeld(daten.hausnummer(), "Bitte eine Hausnummer eingeben.");
        pruefePflichtfeld(daten.plz(), "Bitte eine Postleitzahl eingeben.");
        pruefePflichtfeld(daten.ort(), "Bitte einen Ort eingeben.");

        // Präferenzen sind Freitext. Mehr als 255 Zeichen passen nicht in die Datenbank.
        if (daten.praeferenzen() != null && daten.praeferenzen().length() > MAX_LAENGE) {
            throw new IllegalArgumentException("Die Präferenzen dürfen höchstens " + MAX_LAENGE + " Zeichen lang sein.");
        }
    }

    // Sucht Gäste, die vermutlich dieselbe Person sind (UC11, Schritt 3).
    // Eine Dublette ist ein Gast mit gleicher E-Mail-Adresse
    // oder mit gleichem Vornamen, Nachnamen und Geburtsdatum.
    // Bekommt: die Eingaben aus der Maske (vorher mit pruefeEingaben geprüft).
    // Gibt zurück: Liste der passenden Gäste, jeder nur einmal (leer = keine Dublette).
    public List<Gast> findeDubletten(GastDaten daten) {
        List<Gast> dubletten = new ArrayList<>();

        // Treffer 1: gleiche E-Mail-Adresse.
        Optional<Gast> gleicheEmail = gastprofilRepository.findByEmailIgnoreCase(bereinigeEmail(daten.email()));
        if (gleicheEmail.isPresent()) {
            dubletten.add(gleicheEmail.get());
        }

        // Treffer 2: gleicher Name und gleiches Geburtsdatum.
        List<Gast> gleicherName = gastprofilRepository.findByVornameIgnoreCaseAndNachnameIgnoreCaseAndGeburtsdatum(
                daten.vorname().trim(), daten.nachname().trim(), daten.geburtsdatum());
        for (Gast gast : gleicherName) {
            // Derselbe Gast kann bei beiden Suchen auftauchen. Dann nur einmal in die Liste.
            if (!istSchonInListe(gast, dubletten)) {
                dubletten.add(gast);
            }
        }

        return dubletten;
    }

    // Prüft, ob es schon einen Gast mit dieser E-Mail-Adresse gibt.
    // Bekommt: die E-Mail-Adresse (Groß-/Kleinschreibung und Leerzeichen egal).
    // Gibt zurück: true, wenn die E-Mail schon vergeben ist, sonst false.
    public boolean emailIstVergeben(String email) {
        return gastprofilRepository.findByEmailIgnoreCase(bereinigeEmail(email)).isPresent();
    }

    // ---------- Hilfsmethoden (nur innerhalb dieser Klasse) ----------

    // Wirft einen Fehler, wenn ein Pflichtfeld leer ist.
    // Bekommt: den eingegebenen Text und die Fehlermeldung für den Mitarbeiter.
    // Gibt nichts zurück.
    private void pruefePflichtfeld(String eingabe, String fehlermeldung) {
        // isBlank() ist true bei "" und bei Text, der nur aus Leerzeichen besteht.
        if (eingabe == null || eingabe.isBlank()) {
            throw new IllegalArgumentException(fehlermeldung);
        }
    }

    // Einfache Formatprüfung: genau ein @, davor etwas, danach eine Domain mit Punkt.
    // Ob es die Adresse wirklich gibt, kann man so nicht prüfen. Das reicht für Tippfehler.
    // Bekommt: die E-Mail-Adresse. Gibt zurück: true, wenn das Format passt.
    private boolean istGueltigeEmail(String email) {
        // [^@\\s]+ heißt: ein oder mehr Zeichen, die kein @ und kein Leerzeichen sind.
        return bereinigeEmail(email).matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    // Entfernt Leerzeichen am Rand und schreibt alles klein: " Max@Web.DE " → "max@web.de".
    // So gilt dieselbe Adresse auch bei anderer Schreibweise als gleich.
    // Bekommt: die E-Mail-Adresse. Gibt zurück: die bereinigte Adresse.
    private String bereinigeEmail(String email) {
        return email.trim().toLowerCase();
    }

    // Macht aus einem leeren Feld null, sonst den Text ohne Leerzeichen am Rand.
    // Bekommt: den eingegebenen Text. Gibt zurück: den Text oder null.
    private String leerWirdNull(String eingabe) {
        if (eingabe == null || eingabe.isBlank()) {
            return null;
        }
        return eingabe.trim();
    }

    // Baut aus den Eingaben eine Adresse. Ist kein Land angegeben, wird "Deutschland" eingetragen.
    // Bekommt: die Eingaben aus der Maske. Gibt zurück: die neue Adresse (noch nicht gespeichert).
    private Adresse erstelleAdresse(GastDaten daten) {
        String land = leerWirdNull(daten.land());
        if (land == null) {
            land = STANDARD_LAND;
        }
        return new Adresse(
                daten.strasse().trim(),
                daten.hausnummer().trim(),
                daten.plz().trim(),
                daten.ort().trim(),
                land);
    }

    // Prüft, ob ein Gast schon in der Liste steht (Vergleich über die Gastnummer).
    // Bekommt: den Gast und die Liste. Gibt zurück: true, wenn er schon drin ist.
    private boolean istSchonInListe(Gast gast, List<Gast> liste) {
        for (Gast vorhandener : liste) {
            if (vorhandener.istDerselbeWie(gast)) {
                return true;
            }
        }
        return false;
    }
}
