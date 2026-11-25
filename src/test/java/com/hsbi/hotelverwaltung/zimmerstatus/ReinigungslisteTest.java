package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinigungslisteTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 11, 25);

    @Test
    void zimmerWirdNurEinmalAufgenommen() {
        Zimmer zimmer = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        Reinigungsliste liste = new Reinigungsliste(HEUTE);

        liste.zimmerHinzufuegen(zimmer);
        liste.zimmerHinzufuegen(zimmer);

        assertEquals(1, liste.getZimmer().size());
    }

    @Test
    void statusAktualisierenSetztNurZuReinigendeAufGereinigt() {
        Zimmer zuReinigen = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        Zimmer schonGereinigt = new Zimmer(102, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.zimmerHinzufuegen(zuReinigen);
        liste.zimmerHinzufuegen(schonGereinigt);
        schonGereinigt.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);

        assertEquals(1, liste.statusAktualisieren());
        assertEquals(Reinigungsstatus.GEREINIGT, zuReinigen.getReinigungsstatus());
    }

    @Test
    void abgeschlosseneListeLaesstSichNichtMehrAendern() {
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.abschliessen(0, HEUTE.atTime(12, 0));

        assertTrue(liste.istAbgeschlossen());
        assertThrows(IllegalStateException.class, liste::statusAktualisieren);
        assertThrows(IllegalStateException.class, () -> liste.zimmerHinzufuegen(
                new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD)));
    }

    @Test
    void ohneDatumGehtEsNicht() {
        assertThrows(NullPointerException.class, () -> new Reinigungsliste(null));
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        assertThrows(NullPointerException.class, () -> liste.abschliessen(0, (LocalDateTime) null));
    }
}
