package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;

/**
 * Statusfilter der Zimmerübersicht (UC24, Erweiterung E1).
 *
 * <p>Gesperrte Zimmer erscheinen nur unter "gesperrt", weil sie nicht gereinigt
 * und nicht vergeben werden. Belegte Zimmer erscheinen unter "belegt" und
 * zusätzlich unter ihrem Reinigungsstatus.
 */
public enum Statusfilter {

    ALLE("Alle Zimmer"),
    BELEGT("belegt"),
    ZU_REINIGEN("zu reinigen"),
    NICHT_REINIGEN("nicht reinigen"),
    GEREINIGT("gereinigt"),
    GESPERRT("gesperrt");

    private final String bezeichnung;

    Statusfilter(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    /** Prüft, ob ein Zimmer zu diesem Filter passt. */
    public boolean passt(Zimmer zimmer) {
        return switch (this) {
            case ALLE -> true;
            case BELEGT -> zimmer.istBelegt();
            case GESPERRT -> zimmer.istGesperrt();
            case ZU_REINIGEN -> hatReinigungsstatus(zimmer, Reinigungsstatus.ZU_REINIGEN);
            case NICHT_REINIGEN -> hatReinigungsstatus(zimmer, Reinigungsstatus.NICHT_REINIGEN);
            case GEREINIGT -> hatReinigungsstatus(zimmer, Reinigungsstatus.GEREINIGT);
        };
    }

    private static boolean hatReinigungsstatus(Zimmer zimmer, Reinigungsstatus status) {
        return !zimmer.istGesperrt() && zimmer.getReinigungsstatus() == status;
    }
}
