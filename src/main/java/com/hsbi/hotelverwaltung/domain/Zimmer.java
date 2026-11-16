package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * Ein Hotelzimmer. Die Zimmernummer ist der Schlüssel (z. B. "304").
 * Fachliche Methoden wie sperren() ergänzen die Gruppen 1 und 7.
 */
@Entity
public class Zimmer {

    @Id
    private String zimmerNr;

    private int etage;

    @Enumerated(EnumType.STRING)
    private Belegungsart belegungsart;

    @Enumerated(EnumType.STRING)
    private Zimmerklasse zimmerklasse;

    private String ausstattung;

    @Enumerated(EnumType.STRING)
    private Zimmerstatus zimmerstatus;

    private String sperrgrund;

    /** Für JPA. */
    protected Zimmer() {
    }

    /** Neues Zimmer hat den Status "zu reinigen" (UC01). */
    public Zimmer(String zimmerNr, int etage, Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        this.zimmerNr = zimmerNr;
        this.etage = etage;
        this.belegungsart = belegungsart;
        this.zimmerklasse = zimmerklasse;
        this.zimmerstatus = Zimmerstatus.ZU_REINIGEN;
    }

    /** Kurzbeschreibung für Listen, z. B. "304 (Standard, Doppelzimmer)". */
    public String getKurzbeschreibung() {
        return zimmerNr + " (" + zimmerklasse.getBezeichnung() + ", " + belegungsart.getBezeichnung() + ")";
    }

    public String getZimmerNr() {
        return zimmerNr;
    }

    public int getEtage() {
        return etage;
    }

    public void setEtage(int etage) {
        this.etage = etage;
    }

    public Belegungsart getBelegungsart() {
        return belegungsart;
    }

    public void setBelegungsart(Belegungsart belegungsart) {
        this.belegungsart = belegungsart;
    }

    public Zimmerklasse getZimmerklasse() {
        return zimmerklasse;
    }

    public void setZimmerklasse(Zimmerklasse zimmerklasse) {
        this.zimmerklasse = zimmerklasse;
    }

    public String getAusstattung() {
        return ausstattung;
    }

    public void setAusstattung(String ausstattung) {
        this.ausstattung = ausstattung;
    }

    public Zimmerstatus getZimmerstatus() {
        return zimmerstatus;
    }

    public void setZimmerstatus(Zimmerstatus zimmerstatus) {
        this.zimmerstatus = zimmerstatus;
    }

    public String getSperrgrund() {
        return sperrgrund;
    }

    public void setSperrgrund(String sperrgrund) {
        this.sperrgrund = sperrgrund;
    }
}
