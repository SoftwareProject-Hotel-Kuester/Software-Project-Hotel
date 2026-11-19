package com.hsbi.hotelverwaltung.belegung;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.Zimmer;

import java.time.LocalDate;
import java.util.Map;

/**
 * Eine Zeile des Belegungskalenders: ein Zimmer und seine Buchungen je Tag.
 * Ein Tag ist belegt, wenn die Nacht von diesem Tag auf den nächsten gebucht ist.
 * Der Abreisetag gehört deshalb nicht mehr zur Buchung.
 *
 * @param zimmer    das Zimmer dieser Zeile
 * @param buchungen Buchung je belegtem Tag (Tage ohne Buchung fehlen)
 */
public record BelegungsZeile(Zimmer zimmer, Map<LocalDate, Buchung> buchungen) {

    /** Gebucht, wenn eine Buchung den Tag belegt, sonst gesperrt, wenn das Zimmer gesperrt ist, sonst frei. */
    public Zellenart art(LocalDate tag) {
        if (buchungen.containsKey(tag)) {
            return Zellenart.GEBUCHT;
        }
        return zimmer.istGesperrt() ? Zellenart.GESPERRT : Zellenart.FREI;
    }
}
