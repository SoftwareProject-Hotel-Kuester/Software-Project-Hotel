package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests für ZimmerService. Gehört zu UC01 LegeZimmerAn.
// Prüft: Anlegen, doppelte Nummer (E1), ungültige Eingaben.
//
// @SpringBootTest: startet die App mit echter Datenbank. Docker Desktop muss laufen.
// @Transactional: jeder Test wird danach zurückgerollt → Datenbank bleibt sauber.
// Nummern 998/999: nicht in den Testdaten → keine Überschneidung.
// assertEquals(erwartet, tatsächlich): Test schlägt fehl, wenn beides ungleich ist.
// assertThrows(Fehlerart, () -> aufruf): Test schlägt fehl, wenn der Aufruf KEINEN Fehler wirft.
//   "() -> ..." = der Aufruf wird erst in assertThrows ausgeführt, damit JUnit den Fehler abfangen kann.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class ZimmerServiceTest {

    // @Autowired: Spring übergibt den fertigen ZimmerService.
    @Autowired
    private ZimmerService zimmerService;

    // @Test: Methode = ein Testfall.
    // Anlegen klappt: gespeichert, Status NICHT_BELEGT + ZU_REINIGEN (UC01 Schritt 5).
    @Test
    void zimmerAnlegenSpeichertZimmerMitAnfangsstatus() {
        // Zimmer 999 anlegen.
        Zimmer zimmer = zimmerService.zimmerAnlegen(999, 9, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);

        // Prüfen: Zimmer ist gespeichert und hat den Anfangsstatus.
        assertTrue(zimmerService.zimmerNummerIstVergeben(999));
        assertEquals(Zimmerstatus.NICHT_BELEGT, zimmer.getZimmerstatus());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
    }

    // Doppelte Nummer → Fehler (UC01 E1).
    @Test
    void doppelteZimmerNummerWirdAbgelehnt() {
        // Zimmer 998 einmal anlegen.
        zimmerService.zimmerAnlegen(998, 9, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);

        // Zweites Mal 998 → muss Fehler werfen.
        assertThrows(IllegalArgumentException.class,
                () -> zimmerService.zimmerAnlegen(998, 9, Belegungsart.EINZEL, Zimmerklasse.ECONOMY));
    }

    // Nummer 0 → Fehler (Nummer muss > 0 sein).
    @Test
    void zimmerNummerNullWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> zimmerService.zimmerAnlegen(0, 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY));
    }

    // Etage −1 → Fehler (kein Keller).
    @Test
    void negativeEtageWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> zimmerService.zimmerAnlegen(999, -1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY));
    }

    // Belegungsart fehlt → Fehler (Pflichtfeld).
    @Test
    void fehlendeBelegungsartWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> zimmerService.zimmerAnlegen(999, 1, null, Zimmerklasse.ECONOMY));
    }
}