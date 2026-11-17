package com.hsbi.hotelverwaltung.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit-Tests für die Statusregeln eines Zimmers (UC01, UC22, UC23, UC25 bis UC27). */
class ZimmerTest {

    private Zimmer zimmer;

    @BeforeEach
    void setUp() {
        zimmer = new Zimmer(304, 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
    }

    @Test
    void neuesZimmerIstNichtBelegtUndZuReinigen() {
        assertEquals(Zimmerstatus.NICHT_BELEGT, zimmer.getZimmerstatus());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
    }

    @Test
    void eincheckenUndAuscheckenEinesGereinigtenZimmers() {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);

        zimmer.einchecken();
        assertTrue(zimmer.istBelegt());

        zimmer.auschecken();
        assertFalse(zimmer.istBelegt());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
    }

    @Test
    void nichtGereinigtesZimmerKannNichtEingechecktWerden() {
        assertThrows(IllegalStateException.class, () -> zimmer.einchecken());
    }

    @Test
    void gesperrtesZimmerKannNichtEingechecktWerden() {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        zimmer.sperren("Fernseher ist defekt");

        assertThrows(IllegalStateException.class, () -> zimmer.einchecken());
    }

    @Test
    void sperrenMerktDenGrundUndAufhebenSetztZuReinigen() {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);

        zimmer.sperren("Fernseher ist defekt");
        assertTrue(zimmer.istGesperrt());
        assertEquals("Fernseher ist defekt", zimmer.getSperrgrund());

        zimmer.sperreAufheben();
        assertEquals(Zimmerstatus.NICHT_BELEGT, zimmer.getZimmerstatus());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
        assertNull(zimmer.getSperrgrund());
    }

    @Test
    void sperrgrundIstPflicht() {
        assertThrows(IllegalArgumentException.class, () -> zimmer.sperren("  "));
    }

    @Test
    void nichtGesperrtesZimmerKannNichtEntsperrtWerden() {
        assertThrows(IllegalStateException.class, () -> zimmer.sperreAufheben());
    }
}
