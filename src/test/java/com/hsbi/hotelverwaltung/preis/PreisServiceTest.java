package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Tests für PreisService. Gehört zu UC03 PflegePreise.
// Prüft: neuen Preis anlegen, alten Preis am Vortag beenden (E1), ungültige Eingaben, Folgepreis.
//
// @SpringBootTest: startet die App mit echter Datenbank. Docker Desktop muss laufen.
// @Transactional: Jeder Test wird danach zurückgerollt, die Datenbank bleibt sauber.
// Die Tests nutzen Daten im Jahr 2030. Dort gibt es in den Testdaten noch keine Preisänderung,
// nur den Economy-Einzelzimmer-Preis ab 01.01.2026, der unbegrenzt gilt.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class PreisServiceTest {

    // Feste Werte für alle Tests, damit die Tests gut lesbar bleiben.
    private static final Zimmerklasse KLASSE = Zimmerklasse.ECONOMY;
    private static final Belegungsart ART = Belegungsart.EINZEL;
    private static final LocalDate NEUER_START = LocalDate.of(2030, 3, 1);

    // @Autowired: Spring übergibt den fertigen PreisService.
    @Autowired
    private PreisService preisService;

    // Neuer Preis beendet den alten am Vortag (UC03, E1 Schritt 7).
    @Test
    void neuerPreisBeendetAltenPreisAmVortag() {
        // Vorher: Am 28.02.2030 gilt der alte Preis aus den Testdaten.
        Preis alterPreis = preisService.preisAmTag(KLASSE, ART, NEUER_START.minusDays(1));
        assertNotNull(alterPreis);

        // Neuen Preis ab 01.03.2030 anlegen.
        preisService.preisAnlegen(KLASSE, ART, new BigDecimal("95.00"), NEUER_START);

        // Prüfen: Der alte Preis endet am 28.02.2030, ab 01.03.2030 gilt der neue.
        assertEquals(NEUER_START.minusDays(1), alterPreis.getGueltigBis());
        Preis neuerPreis = preisService.preisAmTag(KLASSE, ART, NEUER_START);
        assertEquals(0, new BigDecimal("95.00").compareTo(neuerPreis.getNachtpreis()));
    }

    // Zweiter Preis am selben Starttag wird abgelehnt (sonst gälten zwei Preise am selben Tag).
    @Test
    void gleicherStarttagWirdAbgelehnt() {
        preisService.preisAnlegen(KLASSE, ART, new BigDecimal("95.00"), NEUER_START);

        assertThrows(IllegalArgumentException.class,
                () -> preisService.preisAnlegen(KLASSE, ART, new BigDecimal("99.00"), NEUER_START));
    }

    // Ein Preis, der vor einem schon eingetragenen späteren Preis beginnt, wird abgelehnt.
    @Test
    void starttagVorSpaeteremPreisWirdAbgelehnt() {
        preisService.preisAnlegen(KLASSE, ART, new BigDecimal("95.00"), NEUER_START);

        assertThrows(IllegalArgumentException.class,
                () -> preisService.preisAnlegen(KLASSE, ART, new BigDecimal("90.00"), NEUER_START.minusMonths(1)));
    }

    // Nachtpreis 0 € wird abgelehnt (Zimmer werden nicht verschenkt).
    @Test
    void nachtpreisNullWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> preisService.preisAnlegen(KLASSE, ART, BigDecimal.ZERO, NEUER_START));
    }

    // Der Folgepreis ist der Preis, der am Tag nach "gültig bis" beginnt.
    @Test
    void folgePreisWirdGefunden() {
        Preis alterPreis = preisService.preisAmTag(KLASSE, ART, NEUER_START.minusDays(1));
        preisService.preisAnlegen(KLASSE, ART, new BigDecimal("95.00"), NEUER_START);

        Preis folgePreis = preisService.folgePreis(alterPreis);

        assertEquals(NEUER_START, folgePreis.getGueltigAb());
        // Der neue Preis gilt unbegrenzt, hat also selbst keinen Folgepreis.
        assertNull(preisService.folgePreis(folgePreis));
    }

    // Ein Preis kann nicht vor seinem eigenen Start enden (Regel in Preis.beenden).
    @Test
    void preisKannNichtVorSeinemStartEnden() {
        Preis preis = new Preis(KLASSE, ART, new BigDecimal("95.00"), NEUER_START);

        assertThrows(IllegalArgumentException.class, () -> preis.beenden(NEUER_START.minusDays(1)));
    }
}
