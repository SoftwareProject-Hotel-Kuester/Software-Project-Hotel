package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.stereotype.Service;

import java.util.List;

// Fachregeln rund um Zimmer. Gehört zu UC01 LegeZimmerAn.
// Ablauf: View → ZimmerService → ZimmerRepository → Datenbank.
// Die View spricht nie direkt mit der Datenbank.
//
// @Service: Spring erzeugt beim Start genau ein Objekt dieser Klasse
// und gibt es jeder Klasse, die es im Konstruktor verlangt.
@Service
public class ZimmerService {

    // Über das Repository lesen und speichern wir Zimmer in der Datenbank.
    private final ZimmerRepository zimmerRepository;

    // Konstruktor: Spring übergibt das Repository automatisch, weil es hier als Parameter steht.
    // Bekommt: das ZimmerRepository. Gibt nichts zurück.
    public ZimmerService(ZimmerRepository zimmerRepository) {
        this.zimmerRepository = zimmerRepository;
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
    // Gibt zurück: Liste aller Zimmer (leer, wenn es noch keine gibt).
    public List<Zimmer> alleZimmer() {
        return zimmerRepository.findAll();
    }
}