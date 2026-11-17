package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Diese Klasse legt beim Start der App Beispielzimmer an.
// Jeder im Team hat seine eigene lokale Datenbank. Damit alle dieselben Zimmer
// sehen und testen können, stehen die Testdaten hier im Code.
// Gehört zu UC01 LegeZimmerAn.
//
// @Component sagt Spring: "Erzeuge von dieser Klasse beim Start ein Objekt."
// CommandLineRunner bedeutet: Spring ruft die Methode run(...) genau einmal auf,
// sobald die App fertig gestartet ist.
@Component
public class ZimmerTestdaten implements CommandLineRunner {

    // Über den Service anlegen, damit auch hier die Regeln aus UC01 gelten.
    private final ZimmerService zimmerService;

    // Über das Repository prüfen wir, ob schon Zimmer in der Datenbank sind.
    private final ZimmerRepository zimmerRepository;

    // Konstruktor: Spring übergibt Service und Repository automatisch.
    // Bekommt: den ZimmerService und das ZimmerRepository. Gibt nichts zurück.
    public ZimmerTestdaten(ZimmerService zimmerService, ZimmerRepository zimmerRepository) {
        this.zimmerService = zimmerService;
        this.zimmerRepository = zimmerRepository;
    }

    // Wird einmal nach dem Start der App aufgerufen und legt die Beispielzimmer an.
    // Bekommt: die Startparameter der App (brauchen wir hier nicht).
    // Gibt nichts zurück.
    @Override
    public void run(String... args) {
        // Schritt 1: Gibt es schon Zimmer, machen wir nichts.
        // Sonst würden bei jedem Neustart dieselben Nummern noch einmal angelegt,
        // und zimmerAnlegen würde wegen doppelter Nummer abbrechen.
        if (zimmerRepository.count() > 0) {
            return;
        }

        // Schritt 2: Jede Kombination aus Belegungsart und Zimmerklasse einmal anlegen.
        // So können später auch Preise (UC03) und Preisberechnung (UC05) für alle Fälle getestet werden.

        // Etage 1: Economy
        zimmerService.zimmerAnlegen(101, 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);
        zimmerService.zimmerAnlegen(102, 1, Belegungsart.DOPPEL, Zimmerklasse.ECONOMY);
        zimmerService.zimmerAnlegen(103, 1, Belegungsart.FAMILIE, Zimmerklasse.ECONOMY);

        // Etage 2: Standard
        zimmerService.zimmerAnlegen(201, 2, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        zimmerService.zimmerAnlegen(202, 2, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        zimmerService.zimmerAnlegen(203, 2, Belegungsart.FAMILIE, Zimmerklasse.STANDARD);

        // Etage 3: Premium
        zimmerService.zimmerAnlegen(301, 3, Belegungsart.EINZEL, Zimmerklasse.PREMIUM);
        zimmerService.zimmerAnlegen(302, 3, Belegungsart.DOPPEL, Zimmerklasse.PREMIUM);
        zimmerService.zimmerAnlegen(303, 3, Belegungsart.FAMILIE, Zimmerklasse.PREMIUM);
    }
}