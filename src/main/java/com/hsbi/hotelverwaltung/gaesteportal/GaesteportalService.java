package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.mail.MailService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Fachlogik des Gästeportals (UC17 bis UC21).
// Aktuell: UC18 ZeigeEigeneBuchungen und UC20 StorniereEigeneBuchung.
// Ablauf: View → GaesteportalService → BuchungRepository → Datenbank.
// Die View spricht nie direkt mit der Datenbank.
//
// @Service: Spring erzeugt beim Start genau ein Objekt dieser Klasse
// und gibt es jeder Klasse, die es im Konstruktor verlangt.
@Service
public class GaesteportalService {

    // Über das Repository lesen und speichern wir die Buchungen.
    private final BuchungRepository buchungRepository;

    // Über den MailService verschicken wir die Stornobestätigung (UC20).
    private final MailService mailService;

    // Konstruktor: Spring übergibt Repository und MailService automatisch.
    // Bekommt: das BuchungRepository und den MailService. Gibt nichts zurück.
    public GaesteportalService(BuchungRepository buchungRepository, MailService mailService) {
        this.buchungRepository = buchungRepository;
        this.mailService = mailService;
    }

    // UC18: Lädt alle Buchungen des Gastes und sortiert sie.
    // Zuerst die anstehenden (nächste Anreise oben), dann die vergangenen (jüngste oben).
    // Bekommt: den angemeldeten Gast und das heutige Datum.
    //   Das Datum wird übergeben, damit die Tests mit einem festen Datum arbeiten können.
    // Gibt zurück: die sortierte Liste (leer, wenn der Gast noch nichts gebucht hat).
    public List<Buchung> eigeneBuchungen(Gast gast, LocalDate heute) {
        List<Buchung> anstehend = new ArrayList<>();
        List<Buchung> vergangen = new ArrayList<>();

        // Schritt 1: Buchungen in zwei Listen aufteilen.
        for (Buchung buchung : buchungRepository.findByGast(gast)) {
            if (buchung.istAnstehend(heute)) {
                anstehend.add(buchung);
            } else {
                vergangen.add(buchung);
            }
        }

        // Schritt 2: Beide Listen nach Anreisedatum sortieren.
        anstehend.sort(Comparator.comparing(Buchung::getAnreise));
        vergangen.sort(Comparator.comparing(Buchung::getAnreise).reversed());

        // Schritt 3: Zusammenfügen, anstehende zuerst.
        List<Buchung> ergebnis = new ArrayList<>(anstehend);
        ergebnis.addAll(vergangen);
        return ergebnis;
    }

    // UC18 Schritt 4: Lädt eine einzelne Buchung des Gastes, z. B. für die Detailansicht.
    // Auch UC20 (Stornieren) und später UC19 (Ändern) nutzen diese Methode.
    // Wichtig: Hier wird geprüft, ob die Buchung wirklich diesem Gast gehört.
    // Die Oberfläche allein reicht nicht, weil man Buchungsnummern verändern könnte.
    // Bekommt: den angemeldeten Gast und die Buchungsnummer.
    // Gibt zurück: die Buchung.
    // Wirft eine BuchungNichtGefundenException, wenn es die Buchung nicht gibt
    // oder sie einem anderen Gast gehört.
    public Buchung eigeneBuchung(Gast gast, Integer buchungsNr) {
        Buchung buchung = buchungRepository.findById(buchungsNr).orElse(null);

        if (buchung == null || !buchung.getGast().istDerselbeWie(gast)) {
            throw new BuchungNichtGefundenException(buchungsNr);
        }
        return buchung;
    }

    // UC20: Der Gast storniert eine eigene Buchung.
    // Bekommt: den angemeldeten Gast, die Buchungsnummer und das heutige Datum.
    // Gibt nichts zurück.
    // Wirft eine BuchungNichtGefundenException, wenn die Buchung nicht dem Gast gehört,
    // und eine IllegalStateException, wenn sie nicht mehr storniert werden darf.
    public void eigeneBuchungStornieren(Gast gast, Integer buchungsNr, LocalDate heute) {
        // Schritt 1: Buchung laden. eigeneBuchung(...) prüft, ob sie dem Gast gehört.
        Buchung buchung = eigeneBuchung(gast, buchungsNr);

        // Schritt 2: Vorbedingung aus UC20 prüfen: Status "gebucht" und Anreise in der Zukunft.
        // Die Regel steht in der Klasse Buchung, damit sie überall gleich ist.
        if (!buchung.kannVomGastGeaendertWerden(heute)) {
            throw new IllegalStateException("Diese Buchung kann nicht mehr online storniert werden.");
        }

        // Schritt 3: Stornieren und speichern. Das Zimmer ist danach wieder frei.
        buchung.stornieren();
        buchungRepository.save(buchung);

        // Schritt 4: Bestätigung per E-Mail an den Gast.
        String zeitraum = DatumUndPreisFormat.datum(buchung.getAnreise())
                + " bis " + DatumUndPreisFormat.datum(buchung.getAbreise());
        mailService.sendeStornobestaetigung(gast.getEmail(), gast.getVorname(),
                buchung.getBuchungsNr(), buchung.getZimmer().getZimmerNr(), zeitraum);
    }
}
