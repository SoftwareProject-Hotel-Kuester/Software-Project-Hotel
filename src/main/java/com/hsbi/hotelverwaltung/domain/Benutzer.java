package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Abstrakte Oberklasse für alle Konten im System (laut Domänen-Klassendiagramm).
 * Gast und Mitarbeiter erben von Benutzer.
 *
 * JOINED bedeutet: Die gemeinsamen Daten liegen in der Tabelle "benutzer",
 * die zusätzlichen Daten in "gast" bzw. "mitarbeiter". Beide Tabellen haben
 * dieselbe ID (benutzer_id), die zugleich Fremdschlüssel auf "benutzer" ist.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer benutzerId;

    /** Zugleich der Anmeldename, deshalb eindeutig. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Nur als Hash speichern (macht der Login, Gruppe 6). */
    private String passwort;

    private String vorname;
    private String nachname;
    private String telefon;
    private boolean aktiv;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "adress_id")
    private Adresse adresse;

    /** Für JPA. */
    protected Benutzer() {
    }

    protected Benutzer(String email, String vorname, String nachname) {
        this.email = email;
        this.vorname = vorname;
        this.nachname = nachname;
    }

    public String getVollerName() {
        return vorname + " " + nachname;
    }

    public Integer getBenutzerId() {
        return benutzerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswort() {
        return passwort;
    }

    public void setPasswort(String passwort) {
        this.passwort = passwort;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }
}
