package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;

import java.time.LocalDate;

/**
 * Lesesicht auf ein Zimmer für die Oberfläche: Zimmerdaten, Sperrgrund und wer den
 * Defekt gemeldet hat (aus der offenen Defektmeldung). Die Oberfläche bekommt nur diese Kopie, nie die Entity.
 * So gibt es keine Probleme mit nachgeladenen Daten außerhalb einer Transaktion.
 *
 * @param sperrgrund  Sperrgrund des Zimmers oder {@code null}
 * @param gemeldetAm  Datum der offenen Defektmeldung oder {@code null}
 * @param gemeldetVon wer die offene Defektmeldung angelegt hat oder {@code null}
 */
public record ZimmerInfo(
        int zimmerNr,
        int etage,
        Zimmerklasse zimmerklasse,
        Belegungsart belegungsart,
        Zimmerstatus zimmerstatus,
        Reinigungsstatus reinigungsstatus,
        String sperrgrund,
        LocalDate gemeldetAm,
        Defektmeldung.Quelle gemeldetVon) {

    /** Baut die Lesesicht aus einem Zimmer und seiner neuesten offenen Meldung. */
    public static ZimmerInfo von(Zimmer zimmer, Defektmeldung offeneMeldung) {
        return new ZimmerInfo(
                zimmer.getZimmerNr(),
                zimmer.getEtage(),
                zimmer.getZimmerklasse(),
                zimmer.getBelegungsart(),
                zimmer.getZimmerstatus(),
                zimmer.getReinigungsstatus(),
                zimmer.getSperrgrund(),
                offeneMeldung == null ? null : offeneMeldung.getGemeldetAm(),
                offeneMeldung == null ? null : offeneMeldung.getQuelle());
    }

    public boolean istGesperrt() {
        return zimmerstatus == Zimmerstatus.GESPERRT;
    }

    public boolean istBelegt() {
        return zimmerstatus == Zimmerstatus.BELEGT;
    }

    /** Sperrgrund für die Anzeige, nur bei gesperrten Zimmern, sonst leer. */
    public String sperrgrundFuerAnzeige() {
        return istGesperrt() && sperrgrund != null ? sperrgrund : "";
    }
}
