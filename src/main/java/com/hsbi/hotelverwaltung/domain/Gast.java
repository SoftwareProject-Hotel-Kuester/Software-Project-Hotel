package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Gästeprofil, erbt von {@link Benutzer} (laut Domänen-Klassendiagramm).
 * Name, E-Mail, Telefon und Adresse kommen aus Benutzer.
 *
 * Die Gastnummer ist die Benutzer-ID. Eine zweite, eigene Nummer bräuchte
 * eine extra Zählung in der Datenbank und brächte keinen Vorteil.
 */
@Entity
public class Gast extends Benutzer {

    private LocalDate geburtsdatum;

    /** false, wenn ein Mitarbeiter das Profil ohne Portal-Konto angelegt hat (UC11). */
    private boolean hatKonto;

    /** Freitext, z. B. "ruhiges Zimmer, hohe Etage". */
    private String praeferenzen;

    /** Für JPA. */
    protected Gast() {
    }

    public Gast(String email, String vorname, String nachname, LocalDate geburtsdatum) {
        super(email, vorname, nachname);
        this.geburtsdatum = geburtsdatum;
    }

    /** Gastnummer, z. B. für die Suche durch Mitarbeiter (UC12, UC13). */
    public Long getGastNr() {
        return getBenutzerId();
    }

    /** Vergleich über die Gastnummer, z. B. für die Prüfung "gehört die Buchung mir?". */
    public boolean istDerselbeWie(Gast anderer) {
        return anderer != null && getGastNr() != null && Objects.equals(getGastNr(), anderer.getGastNr());
    }

    public LocalDate getGeburtsdatum() {
        return geburtsdatum;
    }

    public void setGeburtsdatum(LocalDate geburtsdatum) {
        this.geburtsdatum = geburtsdatum;
    }

    public boolean isHatKonto() {
        return hatKonto;
    }

    public void setHatKonto(boolean hatKonto) {
        this.hatKonto = hatKonto;
    }

    public String getPraeferenzen() {
        return praeferenzen;
    }

    public void setPraeferenzen(String praeferenzen) {
        this.praeferenzen = praeferenzen;
    }
}
