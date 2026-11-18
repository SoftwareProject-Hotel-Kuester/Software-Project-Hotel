package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;

import java.time.LocalDate;

/**
 * Mitarbeiter des Hotels, erbt von {@link Benutzer} (laut Domänen-Klassendiagramm).
 * Die Hotelleitung ist ein Mitarbeiter mit der Rolle HOTELLEITUNG.
 *
 * Die Personalnummer ist die Benutzer-ID, genau wie die Gastnummer beim Gast.
 */
@Entity
public class Mitarbeiter extends Benutzer {

    /**
     * STRING speichert den Namen ("HOTELLEITUNG") statt der Position im Enum.
     * Sonst würde ein später eingefügter Wert alle bestehenden Rollen verschieben.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rolle rolle;

    private LocalDate eintrittsdatum;

    /** Leer, solange die Person im Hotel arbeitet. */
    private LocalDate austrittsdatum;

    /** Für JPA. */
    protected Mitarbeiter() {
    }

    public Mitarbeiter(String email, String vorname, String nachname,
                       Rolle rolle, LocalDate eintrittsdatum) {
        super(email, vorname, nachname);
        this.rolle = rolle;
        this.eintrittsdatum = eintrittsdatum;
    }

    /** Personalnummer, z. B. für die Anzeige in der Mitarbeiterliste. */
    public Integer getPersonalNr() {
        return getBenutzerId();
    }

    public boolean istHotelleitung() {
        return rolle == Rolle.HOTELLEITUNG;
    }

    public Rolle getRolle() {
        return rolle;
    }

    public void setRolle(Rolle rolle) {
        this.rolle = rolle;
    }

    public LocalDate getEintrittsdatum() {
        return eintrittsdatum;
    }

    public void setEintrittsdatum(LocalDate eintrittsdatum) {
        this.eintrittsdatum = eintrittsdatum;
    }

    public LocalDate getAustrittsdatum() {
        return austrittsdatum;
    }

    public void setAustrittsdatum(LocalDate austrittsdatum) {
        this.austrittsdatum = austrittsdatum;
    }
}