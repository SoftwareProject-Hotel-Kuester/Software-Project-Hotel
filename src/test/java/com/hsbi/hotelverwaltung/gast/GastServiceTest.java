package com.hsbi.hotelverwaltung.gast;

import com.hsbi.hotelverwaltung.domain.Gast;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests für GastService. Gehört zu UC11 LegeGastprofilAn.
// Prüft: Anlegen, Dubletten (E1), ungültige Eingaben (E2).
//
// @SpringBootTest: startet die App mit echter Datenbank. Docker Desktop muss laufen.
// @Transactional: jeder Test wird danach zurückgerollt → Datenbank bleibt sauber.
// Namen und E-Mails ("Testine Prüfgast", "...@uc11-test.de") kommen in den Testdaten
// nicht vor → keine Überschneidung mit Gästen, die beim Start angelegt werden.
// assertEquals(erwartet, tatsächlich): Test schlägt fehl, wenn beides ungleich ist.
// assertThrows(Fehlerart, () -> aufruf): Test schlägt fehl, wenn der Aufruf KEINEN Fehler wirft.
//   "() -> ..." = der Aufruf wird erst in assertThrows ausgeführt, damit JUnit den Fehler abfangen kann.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class GastServiceTest {

    // Geburtsdatum aller Testgäste. Eine feste Zahl, damit die Tests jedes Mal gleich laufen.
    private static final LocalDate GEBURTSDATUM = LocalDate.of(1990, 1, 15);

    // @Autowired: Spring übergibt den fertigen GastService.
    @Autowired
    private GastService gastService;

    // Anlegen klappt: Gastnummer vergeben, kein Konto, E-Mail bereinigt, Land vorbelegt (UC11 Schritt 4).
    @Test
    void gastprofilAnlegenSpeichertGastOhneKonto() {
        // E-Mail mit Großbuchstaben und Leerzeichen, Land leer gelassen.
        GastDaten daten = beispielDaten("Testine", "Prüfgast", " Testine.Pruefgast@UC11-Test.de ");

        Gast gast = gastService.gastprofilAnlegen(daten, false);

        // Prüfen: Die Datenbank hat eine Gastnummer vergeben.
        assertNotNull(gast.getGastNr());
        // Prüfen: Von der Rezeption angelegt → kein Konto im Gästeportal.
        assertFalse(gast.isHatKonto());
        // Prüfen: E-Mail klein und ohne Leerzeichen gespeichert.
        assertEquals("testine.pruefgast@uc11-test.de", gast.getEmail());
        // Prüfen: Land war leer → "Deutschland".
        assertEquals("Deutschland", gast.getAdresse().getLand());
        // Prüfen: Die E-Mail gilt jetzt als vergeben.
        assertTrue(gastService.emailIstVergeben("testine.pruefgast@uc11-test.de"));
    }

    // Gleiche E-Mail → immer Fehler, auch mit "trotzdem anlegen" (UC11 E1).
    // Die E-Mail ist in der Datenbank eindeutig, ein zweites Profil darf es nicht geben.
    @Test
    void gleicheEmailWirdImmerAbgelehnt() {
        gastService.gastprofilAnlegen(beispielDaten("Testine", "Prüfgast", "doppelt@uc11-test.de"), false);

        // Anderer Name, aber dieselbe E-Mail (diesmal groß geschrieben).
        GastDaten zweiterGast = beispielDaten("Anderer", "Name", "DOPPELT@uc11-test.de");

        assertThrows(IllegalArgumentException.class,
                () -> gastService.gastprofilAnlegen(zweiterGast, true));
    }

    // Gleicher Name und Geburtsdatum, andere E-Mail → wird als Dublette gefunden (UC11 Schritt 3).
    @Test
    void gleicherNameUndGeburtsdatumWirdAlsDubletteGefunden() {
        Gast vorhandener = gastService.gastprofilAnlegen(
                beispielDaten("Testine", "Prüfgast", "erste@uc11-test.de"), false);

        // Gleicher Name (andere Schreibweise), gleiches Geburtsdatum, andere E-Mail.
        GastDaten neueEingabe = beispielDaten("TESTINE", "prüfgast", "zweite@uc11-test.de");
        List<Gast> dubletten = gastService.findeDubletten(neueEingabe);

        // Prüfen: genau ein Treffer, und zwar der vorhandene Gast.
        assertEquals(1, dubletten.size());
        assertEquals(vorhandener.getGastNr(), dubletten.get(0).getGastNr());
    }

    // Dublette und Mitarbeiter hat NICHT "trotzdem anlegen" gewählt → Fehler (UC11 E1).
    @Test
    void dubletteOhneBestaetigungWirdAbgelehnt() {
        gastService.gastprofilAnlegen(beispielDaten("Testine", "Prüfgast", "erste@uc11-test.de"), false);

        GastDaten neueEingabe = beispielDaten("Testine", "Prüfgast", "zweite@uc11-test.de");

        assertThrows(IllegalArgumentException.class,
                () -> gastService.gastprofilAnlegen(neueEingabe, false));
    }

    // Dublette und Mitarbeiter hat "trotzdem anlegen" gewählt → zweites Profil (UC11 E1).
    // Beispiel: zwei verschiedene Personen, die zufällig gleich heißen und am selben Tag geboren sind.
    @Test
    void dubletteMitBestaetigungWirdAngelegt() {
        Gast ersterGast = gastService.gastprofilAnlegen(
                beispielDaten("Testine", "Prüfgast", "erste@uc11-test.de"), false);

        Gast zweiterGast = gastService.gastprofilAnlegen(
                beispielDaten("Testine", "Prüfgast", "zweite@uc11-test.de"), true);

        // Prüfen: zwei verschiedene Gastnummern.
        assertNotEquals(ersterGast.getGastNr(), zweiterGast.getGastNr());
    }

    // Kein Vorname → Fehler (Pflichtfeld, UC11 E2).
    @Test
    void leererVornameWirdAbgelehnt() {
        GastDaten daten = beispielDaten("   ", "Prüfgast", "leer@uc11-test.de");

        assertThrows(IllegalArgumentException.class,
                () -> gastService.gastprofilAnlegen(daten, false));
    }

    // Geburtsdatum morgen → Fehler (UC11 E2).
    @Test
    void geburtsdatumInDerZukunftWirdAbgelehnt() {
        GastDaten daten = new GastDaten("Testine", "Prüfgast", LocalDate.now().plusDays(1),
                "zukunft@uc11-test.de", null,
                "Teststraße", "1", "33602", "Bielefeld", null, null);

        assertThrows(IllegalArgumentException.class,
                () -> gastService.gastprofilAnlegen(daten, false));
    }

    // E-Mail ohne @ → Fehler (UC11 E2).
    @Test
    void ungueltigeEmailWirdAbgelehnt() {
        GastDaten daten = beispielDaten("Testine", "Prüfgast", "keine-email.de");

        assertThrows(IllegalArgumentException.class,
                () -> gastService.gastprofilAnlegen(daten, false));
    }

    // ---------- Hilfsmethode ----------

    // Baut gültige Eingaben für einen Testgast, damit nicht jeder Test alle elf Felder ausschreibt.
    // Bekommt: Vorname, Nachname und E-Mail (das, was sich zwischen den Tests unterscheidet).
    // Gibt zurück: GastDaten mit festem Geburtsdatum und Bielefelder Testadresse, Land leer.
    private GastDaten beispielDaten(String vorname, String nachname, String email) {
        return new GastDaten(vorname, nachname, GEBURTSDATUM, email, "0521 123456",
                "Teststraße", "1", "33602", "Bielefeld", null, "ruhiges Zimmer");
    }
}
