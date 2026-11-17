package com.hsbi.hotelverwaltung.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.zimmerstatus.Statusfilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC24 E1: Statusfilter der Zimmerübersicht.
 */
class StatusfilterTest {

    @Test
    void belegtesZimmerErscheintUnterBelegtUndUnterSeinemReinigungsstatus() {
        Zimmer zimmer = zimmer(Reinigungsstatus.ZU_REINIGEN);
        belegen(zimmer);

        Assertions.assertTrue(Statusfilter.BELEGT.passt(zimmer));
        assertTrue(Statusfilter.ZU_REINIGEN.passt(zimmer));
        assertFalse(Statusfilter.GEREINIGT.passt(zimmer));
        assertFalse(Statusfilter.GESPERRT.passt(zimmer));
    }

    @Test
    void gesperrtesZimmerErscheintNurUnterGesperrt() {
        Zimmer zimmer = zimmer(Reinigungsstatus.ZU_REINIGEN);
        zimmer.sperren("Fernseher defekt");

        assertTrue(Statusfilter.GESPERRT.passt(zimmer));
        assertFalse(Statusfilter.ZU_REINIGEN.passt(zimmer));
        assertFalse(Statusfilter.BELEGT.passt(zimmer));
    }

    @Test
    void alleZeigtJedesZimmer() {
        Zimmer gesperrt = zimmer(Reinigungsstatus.GEREINIGT);
        gesperrt.sperren("Fernseher defekt");

        assertTrue(Statusfilter.ALLE.passt(gesperrt));
        assertTrue(Statusfilter.ALLE.passt(zimmer(Reinigungsstatus.NICHT_REINIGEN)));
    }

    @Test
    void freiesGereinigtesZimmerErscheintUnterGereinigt() {
        Zimmer zimmer = zimmer(Reinigungsstatus.GEREINIGT);

        assertTrue(Statusfilter.GEREINIGT.passt(zimmer));
        assertFalse(Statusfilter.BELEGT.passt(zimmer));
    }

    private static Zimmer zimmer(Reinigungsstatus status) {
        Zimmer zimmer = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);
        zimmer.reinigungsstatusAendern(status);
        return zimmer;
    }

    /** Zimmer belegen wie beim Check-in (geht nur bei gereinigtem Zimmer), Reinigungsstatus bleibt. */
    static void belegen(Zimmer zimmer) {
        Reinigungsstatus vorher = zimmer.getReinigungsstatus();
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        zimmer.einchecken();
        zimmer.reinigungsstatusAendern(vorher);
    }
}
