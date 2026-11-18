package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Fachregeln rund um Zimmer. Gehört zu UC01 LegeZimmerAn, UC02 ÄndereZimmer und UC34 LöscheZimmer.
// Ablauf: View → ZimmerService → ZimmerRepository → Datenbank.
// Die View spricht nie direkt mit der Datenbank.
//
// @Service: Spring erzeugt beim Start genau ein Objekt dieser Klasse
// und gibt es jeder Klasse, die es im Konstruktor verlangt.
@Service
public class ZimmerService {

    // Über das Repository lesen und speichern wir Zimmer in der Datenbank.
    private final ZimmerRepository zimmerRepository;

    // Über das Repository lesen wir die Buchungen eines Zimmers (UC02, Schritt 6, und Löschen).
    private final BuchungRepository buchungRepository;

    // Konstruktor: Spring übergibt die Repositories automatisch, weil sie hier als Parameter stehen.
    // Bekommt: das ZimmerRepository und das BuchungRepository. Gibt nichts zurück.
    public ZimmerService(ZimmerRepository zimmerRepository, BuchungRepository buchungRepository) {
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
    }

    // Prüft, ob es schon ein Zimmer mit dieser Nummer gibt (UC01, Schritt 4).
    // Eine Zimmernummer darf nur einmal vorkommen, sonst wären Buchungen nicht eindeutig.
    // Bekommt: die Zimmernummer.
    // Gibt zurück: true, wenn die Nummer schon vergeben ist, sonst false.
    public boolean zimmerNummerIstVergeben(int zimmerNr) {
        return zimmerRepository.existsById(zimmerNr);
    }

    // Legt ein neues Zimmer an und speichert es in der Datenbank (UC01).
    // Bekommt: Zimmernummer, Etage, Belegungsart und Zimmerklasse aus der Eingabemaske.
    // Gibt zurück: das gespeicherte Zimmer.
    // Wirft eine IllegalArgumentException, wenn die Zimmernummer schon vergeben ist (UC01, E1).
    public Zimmer zimmerAnlegen(int zimmerNr, int etage, Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        // Eingaben aus Schritt 3 prüfen (Nummer, Etage, Pflichtfelder).
        pruefePflichtfelder(zimmerNr, etage, belegungsart, zimmerklasse);

        // Schritt 4: Prüfen, ob die Zimmernummer schon vergeben ist.
        // Jede Nummer darf nur einmal vorkommen, sonst wären Buchungen nicht eindeutig.
        if (zimmerNummerIstVergeben(zimmerNr)) {
            // E1: Abbrechen mit einer Meldung, die die Oberfläche dem Benutzer anzeigt.
            throw new IllegalArgumentException("Die Zimmernummer " + zimmerNr + " ist bereits vergeben.");
        }

        // Schritt 5: Die Nummer ist frei, also das Zimmer erzeugen.
        // Der Konstruktor setzt den Status automatisch auf "nicht belegt" und "zu reinigen".
        Zimmer neuesZimmer = new Zimmer(zimmerNr, etage, belegungsart, zimmerklasse);

        // Das neue Zimmer in der Datenbank speichern und zurückgeben.
        return zimmerRepository.save(neuesZimmer);
    }

    // Prüft, ob alle Eingaben für ein neues Zimmer gültig sind (UC01, Schritt 3).
    // Bekommt: Zimmernummer, Etage, Belegungsart und Zimmerklasse.
    // Gibt nichts zurück. Wirft eine IllegalArgumentException, wenn eine Eingabe ungültig ist.
    private void pruefePflichtfelder(int zimmerNr, int etage, Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        // Eine Zimmernummer 0 oder kleiner gibt es im Hotel nicht.
        if (zimmerNr <= 0) {
            throw new IllegalArgumentException("Die Zimmernummer muss größer als 0 sein.");
        }
        // Etage 0 ist das Erdgeschoss. Kellerzimmer (negative Etagen) gibt es nicht.
        if (etage < 0) {
            throw new IllegalArgumentException("Die Etage darf nicht kleiner als 0 sein.");
        }
        // Belegungsart und Zimmerklasse muss die Hotelleitung laut UC01 auswählen.
        // Ohne sie kann später kein Preis berechnet werden (UC05).
        if (belegungsart == null) {
            throw new IllegalArgumentException("Bitte eine Belegungsart auswählen.");
        }
        if (zimmerklasse == null) {
            throw new IllegalArgumentException("Bitte eine Zimmerklasse auswählen.");
        }
    }

    // Lädt alle Zimmer aus der Datenbank, z. B. für die Tabelle in der View (UC01).
    // Bekommt: nichts.
    // Gibt zurück: Liste aller Zimmer, sortiert nach Zimmernummer (leer, wenn es noch keine gibt).
    public List<Zimmer> alleZimmer() {
        // Sort.by("zimmerNr"): Die Datenbank liefert die Zimmer aufsteigend nach Nummer.
        // Ohne Sortierung wäre die Reihenfolge zufällig, z. B. nach jeder Änderung anders.
        return zimmerRepository.findAll(Sort.by("zimmerNr"));
    }

    // Ändert Belegungsart, Klasse und Ausstattung eines bestehenden Zimmers (UC02 ÄndereZimmer).
    // Zimmernummer und Etage werden nicht geändert: Laut UC02 darf die Hotelleitung nur
    // diese drei Werte ändern. Die Nummer ist außerdem der Schlüssel, an dem die Buchungen hängen.
    // Bekommt: die Nummer des Zimmers und die neuen Werte aus der Eingabemaske.
    // Gibt zurück: das geänderte und gespeicherte Zimmer.
    // Wirft eine IllegalArgumentException, wenn es das Zimmer nicht gibt oder eine Auswahl fehlt.
    public Zimmer zimmerAendern(int zimmerNr, Belegungsart belegungsart, Zimmerklasse zimmerklasse,
                                String ausstattung) {
        // Schritt 1: Pflichtauswahl prüfen (UC02, Schritt 5).
        // Ohne Belegungsart und Klasse kann später kein Preis berechnet werden (UC05).
        if (belegungsart == null) {
            throw new IllegalArgumentException("Bitte eine Belegungsart auswählen.");
        }
        if (zimmerklasse == null) {
            throw new IllegalArgumentException("Bitte eine Zimmerklasse auswählen.");
        }

        // Schritt 2: Das Zimmer aus der Datenbank laden.
        Zimmer zimmer = ladeZimmer(zimmerNr);

        // Schritt 3: Die neuen Werte setzen.
        // Bestehende Buchungen bleiben unverändert, weil jede Buchung ihren
        // eigenen Gesamtpreis gespeichert hat (Nachbedingung UC02).
        zimmer.setBelegungsart(belegungsart);
        zimmer.setZimmerklasse(zimmerklasse);
        zimmer.setAusstattung(ausstattung);

        // Schritt 4: Speichern und das geänderte Zimmer zurückgeben (UC02, Schritt 7).
        return zimmerRepository.save(zimmer);
    }

    // Sucht alle künftigen Buchungen eines Zimmers (UC02, Schritt 6).
    // "Künftig" heißt: Die Buchung ist gebucht oder eingecheckt, und die Abreise ist heute oder später.
    // Stornierte und ausgecheckte Buchungen zählen nicht, weil sie vorbei sind.
    // Bekommt: die Zimmernummer.
    // Gibt zurück: Liste der künftigen Buchungen (leer, wenn es keine gibt).
    // Wirft eine IllegalArgumentException, wenn es das Zimmer nicht gibt.
    public List<Buchung> kuenftigeBuchungen(int zimmerNr) {
        Zimmer zimmer = ladeZimmer(zimmerNr);
        List<Buchung> alleBuchungen = buchungRepository.findByZimmer(zimmer);
        List<Buchung> kuenftigeBuchungen = new ArrayList<>();
        LocalDate heute = LocalDate.now();

        // Jede Buchung des Zimmers einzeln prüfen.
        for (Buchung buchung : alleBuchungen) {
            boolean istGebuchtOderEingecheckt = buchung.getStatus() == Buchungsstatus.GEBUCHT
                    || buchung.getStatus() == Buchungsstatus.EINGECHECKT;
            if (istGebuchtOderEingecheckt && buchung.istAnstehend(heute)) {
                kuenftigeBuchungen.add(buchung);
            }
        }
        return kuenftigeBuchungen;
    }

    // Löscht ein Zimmer aus der Datenbank (UC34 LöscheZimmer, Schritte 6 und 7).
    // Regel: Ein Zimmer mit Buchungen (auch alten oder stornierten) darf nicht gelöscht werden (UC34, E1).
    // Sonst gingen Buchungen und Rechnungsdaten verloren. Solche Zimmer sperrt man stattdessen (UC26).
    // Bekommt: die Zimmernummer. Gibt nichts zurück.
    // Wirft eine IllegalArgumentException, wenn das Zimmer nicht gelöscht werden darf.
    public void zimmerLoeschen(int zimmerNr) {
        // Schritt 1: Das Zimmer laden.
        Zimmer zimmer = ladeZimmer(zimmerNr);

        // Schritt 2: Prüfen, ob es Buchungen zu diesem Zimmer gibt.
        List<Buchung> buchungen = buchungRepository.findByZimmer(zimmer);
        if (!buchungen.isEmpty()) {
            throw new IllegalArgumentException("Zimmer " + zimmerNr + " hat Buchungen und kann nicht gelöscht werden. "
                    + "Bitte das Zimmer stattdessen sperren.");
        }

        // Schritt 3: Löschen.
        // Hängen noch andere Daten am Zimmer (z. B. Defektmeldungen), verhindert die Datenbank
        // das Löschen und meldet eine DataIntegrityViolationException. Die fangen wir hier ab
        // und geben eine verständliche Meldung weiter.
        try {
            zimmerRepository.delete(zimmer);
        } catch (DataIntegrityViolationException fehler) {
            throw new IllegalArgumentException("Zimmer " + zimmerNr + " kann nicht gelöscht werden, "
                    + "weil noch andere Daten (z. B. Defektmeldungen) daran hängen.");
        }
    }

    // Lädt ein Zimmer anhand seiner Nummer aus der Datenbank.
    // Bekommt: die Zimmernummer.
    // Gibt zurück: das Zimmer.
    // Wirft eine IllegalArgumentException, wenn es das Zimmer nicht gibt,
    // z. B. weil jemand anderes es inzwischen gelöscht hat.
    private Zimmer ladeZimmer(int zimmerNr) {
        // findById liefert eine "Optional"-Hülle, weil es das Zimmer vielleicht nicht gibt.
        // orElse(null) packt das Zimmer aus. Gibt es kein Zimmer, kommt null heraus.
        Zimmer zimmer = zimmerRepository.findById(zimmerNr).orElse(null);
        if (zimmer == null) {
            throw new IllegalArgumentException("Das Zimmer " + zimmerNr + " gibt es nicht mehr.");
        }
        return zimmer;
    }
}