package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Anschrift eines Benutzers. Eigene Tabelle wie im Relationenmodell.
 */
@Entity
public class Adresse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adressId;

    private String strasse;
    private String hausnummer;
    private String plz;
    private String ort;
    private String land;

    /** Für JPA. */
    protected Adresse() {
    }

    public Adresse(String strasse, String hausnummer, String plz, String ort, String land) {
        this.strasse = strasse;
        this.hausnummer = hausnummer;
        this.plz = plz;
        this.ort = ort;
        this.land = land;
    }

    /** Einzeilige Darstellung, z. B. "Hauptstraße 1, 33602 Bielefeld". */
    public String alsText() {
        return strasse + " " + hausnummer + ", " + plz + " " + ort;
    }

    public Long getAdressId() {
        return adressId;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getHausnummer() {
        return hausnummer;
    }

    public void setHausnummer(String hausnummer) {
        this.hausnummer = hausnummer;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }
}
