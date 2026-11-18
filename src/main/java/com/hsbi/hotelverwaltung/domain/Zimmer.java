package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * Ein Hotelzimmer (laut Domänen-Klassendiagramm).
 *
 * Ein Zimmer hat zwei voneinander unabhängige Status:
 * - zimmerstatus: belegt, nicht belegt oder gesperrt
 * - reinigungsstatus: gereinigt, zu reinigen oder nicht reinigen
 *
 * Die Status werden nicht über freie Setter geändert, sondern nur über
 * Fachmethoden wie einchecken() oder sperren(). So können keine
 * widersprüchlichen Zustände entstehen, z. B. "belegt", obwohl niemand
 * eingecheckt ist.
 */
// @Entity: Spring/Hibernate legt für diese Klasse eine Tabelle "zimmer" an.
// Jedes Zimmer-Objekt entspricht einer Zeile in dieser Tabelle.
@Entity
public class Zimmer {

    // @Id: Diese Spalte ist der Primärschlüssel. Die Zimmernummer vergibt die
    // Hotelleitung selbst (UC01), deshalb wird sie nicht automatisch erzeugt.
    @Id
    private int zimmerNr;

    private int etage;

    // @Enumerated(EnumType.STRING): Der Enum-Wert wird als Text gespeichert
    // (z. B. "DOPPEL") statt als Zahl. So bleibt die Datenbank lesbar und
    // nichts geht kaputt, wenn sich die Reihenfolge der Enum-Werte ändert.
    @Enumerated(EnumType.STRING)
    private Belegungsart belegungsart;

    @Enumerated(EnumType.STRING)
    private Zimmerklasse zimmerklasse;

    @Enumerated(EnumType.STRING)
    private Zimmerstatus zimmerstatus;

    @Enumerated(EnumType.STRING)
    private Reinigungsstatus reinigungsstatus;

    /** Grund der Sperre, z. B. "Fernseher ist defekt" (UC26). Nur gesetzt, solange das Zimmer gesperrt ist. */
    private String sperrgrund;

    /** Für JPA. */
    protected Zimmer() {
    }

    /**
     * Legt ein neues Zimmer an (UC01). Ein neues Zimmer ist nicht belegt und
     * muss vor der ersten Vermietung gereinigt werden (UC01, Schritt 5).
     */
    public Zimmer(int zimmerNr, int etage, Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        this.zimmerNr = zimmerNr;
        this.etage = etage;
        this.belegungsart = belegungsart;
        this.zimmerklasse = zimmerklasse;
        this.zimmerstatus = Zimmerstatus.NICHT_BELEGT;
        this.reinigungsstatus = Reinigungsstatus.ZU_REINIGEN;
    }

    /**
     * Ändert den Reinigungsstatus, z. B. durch die tägliche Reinigung (UC25).
     *
     * @param neu der neue Reinigungsstatus, darf nicht null sein
     */
    public void reinigungsstatusAendern(Reinigungsstatus neu) {
        if (neu == null) {
            throw new IllegalArgumentException("Der Reinigungsstatus darf nicht leer sein.");
        }
        this.reinigungsstatus = neu;
    }

    /**
     * Sperrt das Zimmer wegen eines Defekts (UC26). Ist das Zimmer schon
     * gesperrt, wird nur der Sperrgrund geändert (UC26 E1).
     * Auch ein belegtes Zimmer darf gesperrt werden: Der Gast zieht dann in
     * ein Ersatzzimmer um (UC09 E2).
     * Die Verknüpfung mit einer Defektmeldung kommt, sobald es die Klasse gibt.
     *
     * @param sperrgrund Pflichtfeld (UC26, Akzeptanzkriterium 2)
     */
    public void sperren(String sperrgrund) {
        if (sperrgrund == null || sperrgrund.isBlank()) {
            throw new IllegalArgumentException("Der Sperrgrund ist ein Pflichtfeld.");
        }
        this.zimmerstatus = Zimmerstatus.GESPERRT;
        this.sperrgrund = sperrgrund.trim();
    }

    /**
     * Hebt die Sperre auf (UC27). Danach ist das Zimmer wieder buchbar, muss
     * aber erst gereinigt werden (UC27, Schritt 6).
     *
     * @throws IllegalStateException wenn das Zimmer nicht gesperrt ist
     */
    public void sperreAufheben() {
        if (zimmerstatus != Zimmerstatus.GESPERRT) {
            throw new IllegalStateException("Zimmer " + zimmerNr + " ist nicht gesperrt.");
        }
        this.zimmerstatus = Zimmerstatus.NICHT_BELEGT;
        this.reinigungsstatus = Reinigungsstatus.ZU_REINIGEN;
        this.sperrgrund = null;
    }

    /**
     * Markiert das Zimmer beim Check-in als belegt (UC22). Nur hier wird
     * "belegt" gesetzt, damit der Wert immer zu einer eingecheckten Buchung passt.
     *
     * Regeln aus UC22: Das Zimmer muss gereinigt sein (Vorbedingung 3, sonst E1).
     * Ein gesperrtes oder schon belegtes Zimmer kann niemand beziehen.
     *
     * @throws IllegalStateException wenn das Zimmer gesperrt, belegt oder nicht gereinigt ist
     */
    public void einchecken() {
        if (zimmerstatus == Zimmerstatus.GESPERRT) {
            throw new IllegalStateException("Zimmer " + zimmerNr + " ist gesperrt: " + sperrgrund);
        }
        if (zimmerstatus == Zimmerstatus.BELEGT) {
            throw new IllegalStateException("Zimmer " + zimmerNr + " ist bereits belegt.");
        }
        if (reinigungsstatus != Reinigungsstatus.GEREINIGT) {
            throw new IllegalStateException("Zimmer " + zimmerNr + " ist noch nicht gereinigt.");
        }
        this.zimmerstatus = Zimmerstatus.BELEGT;
    }

    /**
     * Gibt das Zimmer beim Check-out frei (UC23). Danach ist es nicht belegt
     * und muss gereinigt werden (UC23, Schritt 5).
     *
     * @throws IllegalStateException wenn das Zimmer nicht belegt ist
     */
    public void auschecken() {
        if (zimmerstatus != Zimmerstatus.BELEGT) {
            throw new IllegalStateException("Zimmer " + zimmerNr + " ist nicht belegt.");
        }
        this.zimmerstatus = Zimmerstatus.NICHT_BELEGT;
        this.reinigungsstatus = Reinigungsstatus.ZU_REINIGEN;
    }

    /** @return true, wenn gerade ein Gast eingecheckt ist */
    public boolean istBelegt() {
        return zimmerstatus == Zimmerstatus.BELEGT;
    }

    /** @return true, wenn das Zimmer gesperrt ist und deshalb nicht gebucht werden darf (UC26, Akzeptanzkriterium 4) */
    public boolean istGesperrt() {
        return zimmerstatus == Zimmerstatus.GESPERRT;
    }

    /** Kurzbeschreibung für Listen, z. B. "304 (Standard, Doppelzimmer)". */
    public String getKurzbeschreibung() {
        return zimmerNr + " (" + zimmerklasse.getBezeichnung() + ", " + belegungsart.getBezeichnung() + ")";
    }

    public int getZimmerNr() {
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

    public Zimmerstatus getZimmerstatus() {
        return zimmerstatus;
    }

    public Reinigungsstatus getReinigungsstatus() {
        return reinigungsstatus;
    }

    public String getSperrgrund() {
        return sperrgrund;
    }
}
