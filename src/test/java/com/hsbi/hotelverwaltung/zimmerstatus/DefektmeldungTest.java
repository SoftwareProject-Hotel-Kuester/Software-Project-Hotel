package com.hsbi.hotelverwaltung.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.zimmerstatus.Defektmeldung;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefektmeldungTest {

    private final Zimmer zimmer = new Zimmer(105, 1, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);

    @Test
    void beschreibungIstPflicht() {
        assertThrows(IllegalArgumentException.class, () -> meldung("   "));
    }

    @Test
    void beschreibungWirdOhneLeerzeichenAmRandGespeichert() {
        Assertions.assertEquals("Fernseher defekt", meldung("  Fernseher defekt ").getBeschreibung());
    }

    @Test
    void erledigtMitVermerk() {
        Defektmeldung meldung = meldung("Fernseher defekt");

        meldung.alsErledigtMarkieren("  Fernseher getauscht ");

        assertTrue(meldung.isErledigt());
        assertEquals("Fernseher getauscht", meldung.getVermerkBehebung());
    }

    @Test
    void erledigtOhneVermerk() {
        Defektmeldung meldung = meldung("Fernseher defekt");

        meldung.alsErledigtMarkieren(" ");

        assertTrue(meldung.isErledigt());
        assertNull(meldung.getVermerkBehebung());
    }

    private Defektmeldung meldung(String beschreibung) {
        return new Defektmeldung(zimmer, beschreibung, LocalDate.of(2026, 11, 16), Defektmeldung.Quelle.GAST);
    }
}
