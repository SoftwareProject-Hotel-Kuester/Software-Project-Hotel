package com.hsbi.hotelverwaltung.gast;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Diese Klasse legt beim Start der App Beispielgäste an.
// Jeder im Team hat seine eigene lokale Datenbank. Damit alle dieselben Gäste
// sehen und testen können, stehen die Testdaten hier im Code.
// Gehört zu UC11 LegeGastprofilAn.
//
// Die Gäste wurden "an der Rezeption angelegt", haben also kein Konto im Gästeportal.
// Gäste MIT Konto legt das Gästeportal selbst an (GaesteportalTestdaten).
//
// Zum Ausprobieren der Dublettenprüfung (UC11 E1) in der Maske:
//   - E-Mail anna.schmidt@example.com eingeben         → "E-Mail schon vergeben"
//   - Anna Schmidt, 04.03.1992 mit anderer E-Mail       → "Bestehendes verwenden oder trotzdem anlegen?"
//
// @Component sagt Spring: "Erzeuge von dieser Klasse beim Start ein Objekt."
// CommandLineRunner bedeutet: Spring ruft die Methode run(...) genau einmal auf,
// sobald die App fertig gestartet ist.
@Component
public class GastTestdaten implements CommandLineRunner {

    // An dieser E-Mail erkennen wir, ob die Testgäste schon angelegt wurden.
    private static final String ERSTE_TEST_EMAIL = "anna.schmidt@example.com";

    // Über den Service anlegen, damit auch hier die Regeln aus UC11 gelten.
    private final GastService gastService;

    // Konstruktor: Spring übergibt den Service automatisch.
    // Bekommt: den GastService. Gibt nichts zurück.
    public GastTestdaten(GastService gastService) {
        this.gastService = gastService;
    }

    // Wird einmal nach dem Start der App aufgerufen und legt die Beispielgäste an.
    // Bekommt: die Startparameter der App (brauchen wir hier nicht).
    // Gibt nichts zurück.
    @Override
    public void run(String... args) {
        // Schritt 1: Gibt es Anna schon, wurden die Testgäste bei einem früheren Start angelegt.
        // Wir prüfen bewusst NICHT "gibt es überhaupt Gäste?", weil auch das Gästeportal
        // eigene Testgäste anlegt. Dann würden unsere sonst nie angelegt.
        if (gastService.emailIstVergeben(ERSTE_TEST_EMAIL)) {
            return;
        }

        // Schritt 2: Beispielgäste anlegen. false = keine Dublette erlaubt.
        // Unterschiedliche Fälle: mit/ohne Telefon, mit/ohne Präferenzen, Gast aus dem Ausland.
        gastService.gastprofilAnlegen(new GastDaten(
                "Anna", "Schmidt", LocalDate.of(1992, 3, 4),
                ERSTE_TEST_EMAIL, "0521 123456",
                "Hauptstraße", "12", "33602", "Bielefeld", null,
                "ruhiges Zimmer"), false);

        gastService.gastprofilAnlegen(new GastDaten(
                "Thomas", "Becker", LocalDate.of(1978, 7, 21),
                "thomas.becker@example.com", null,
                "Bahnhofstraße", "5a", "33330", "Gütersloh", null,
                null), false);

        gastService.gastprofilAnlegen(new GastDaten(
                "Lena", "Hoffmann", LocalDate.of(2000, 11, 11),
                "lena.hoffmann@example.com", "0170 9876543",
                "Am Markt", "3", "32052", "Herford", null,
                "hohe Etage, Zimmer mit Balkon"), false);

        gastService.gastprofilAnlegen(new GastDaten(
                "Jan", "de Vries", LocalDate.of(1965, 2, 28),
                "jan.devries@example.com", "+31 6 12345678",
                "Hengelosestraat", "100", "7514 AE", "Enschede", "Niederlande",
                null), false);
    }
}
