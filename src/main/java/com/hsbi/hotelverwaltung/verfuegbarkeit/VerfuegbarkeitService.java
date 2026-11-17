package com.hsbi.hotelverwaltung.verfuegbarkeit;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gehört zu UC04 (PrüfeVerfügbarkeit).
 *
 * Findet alle Zimmer, die in einem Zeitraum frei sind. UC06, UC16 und UC31
 * verwenden dieselbe Logik und rufen diesen Service auf.
 */
// @Service: Spring erzeugt von dieser Klasse genau ein Objekt und reicht es
// überall dort weiter, wo es im Konstruktor angefordert wird.
@Service
public class VerfuegbarkeitService {

    /** Diese Buchungen belegen ein Zimmer. Stornierte und ausgecheckte Buchungen zählen nicht. */
    private static final List<Buchungsstatus> BELEGENDE_STATUS =
            List.of(Buchungsstatus.GEBUCHT, Buchungsstatus.EINGECHECKT);

    private final ZimmerRepository zimmerRepository;
    private final BuchungRepository buchungRepository;

    public VerfuegbarkeitService(ZimmerRepository zimmerRepository, BuchungRepository buchungRepository) {
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
    }

    /**
     * Sucht alle freien Zimmer im Zeitraum (UC04, Schritte 3 bis 5).
     *
     * @param anreise      erster Tag des Aufenthalts, Pflichtfeld
     * @param abreise      Abreisetag, Pflichtfeld, muss nach der Anreise liegen
     * @param belegungsart optionaler Filter, z. B. nur Doppelzimmer (null = alle)
     * @param zimmerklasse optionaler Filter, z. B. nur Standard (null = alle)
     * @return die freien Zimmer, sortiert nach Zimmernummer. Die Liste ist leer,
     *         wenn nichts frei ist (UC04 E1). Die Oberfläche zeigt dann einen Hinweis.
     * @throws IllegalArgumentException wenn ein Datum fehlt oder die Abreise nicht nach der Anreise liegt
     */
    public List<Zimmer> findeFreieZimmer(LocalDate anreise, LocalDate abreise,
                                         Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        // Schritt 1: Eingaben prüfen
        if (anreise == null || abreise == null) {
            throw new IllegalArgumentException("Bitte Anreise und Abreise angeben.");
        }
        if (!abreise.isAfter(anreise)) {
            throw new IllegalArgumentException("Die Abreise muss nach der Anreise liegen.");
        }

        List<Zimmer> freieZimmer = new ArrayList<>();

        // Schritt 2: Jedes Zimmer einzeln prüfen
        for (Zimmer zimmer : zimmerRepository.findAll()) {

            // Filter aus dem Formular: passt das Zimmer überhaupt zur Suche?
            if (belegungsart != null && zimmer.getBelegungsart() != belegungsart) {
                continue;
            }
            if (zimmerklasse != null && zimmer.getZimmerklasse() != zimmerklasse) {
                continue;
            }

            if (istFrei(zimmer, anreise, abreise)) {
                freieZimmer.add(zimmer);
            }
        }

        // Schritt 3: Nach Zimmernummer sortieren, damit die Liste immer gleich aussieht
        freieZimmer.sort((a, b) -> Integer.compare(a.getZimmerNr(), b.getZimmerNr()));
        return freieZimmer;
    }

    /**
     * Ist ein einzelnes Zimmer im Zeitraum frei?
     *
     * Regel: Ein gesperrtes Zimmer ist nie frei. Das Domänenmodell kennt keinen
     * Sperrzeitraum, deshalb gilt die Sperre für jeden Zeitraum (UC26, Akzeptanzkriterium 4).
     * Außerdem darf keine gültige Buchung den Zeitraum überschneiden.
     */
    public boolean istFrei(Zimmer zimmer, LocalDate anreise, LocalDate abreise) {
        if (zimmer.istGesperrt()) {
            return false;
        }
        boolean hatUeberschneidung = buchungRepository
                .existsByZimmerAndStatusInAndAnreiseBeforeAndAbreiseAfter(
                        zimmer, BELEGENDE_STATUS, abreise, anreise);
        return !hatUeberschneidung;
    }
}