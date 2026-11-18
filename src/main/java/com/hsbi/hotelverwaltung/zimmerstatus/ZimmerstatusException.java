package com.hsbi.hotelverwaltung.zimmerstatus;

/**
 * Fachlicher Fehler beim Zimmerstatus, z. B. "Der Sperrgrund ist ein Pflichtfeld."
 * Der Text ist für die Benutzer gedacht und wird in der Oberfläche angezeigt.
 */
public class ZimmerstatusException extends RuntimeException {

    public ZimmerstatusException(String meldung) {
        super(meldung);
    }
}
