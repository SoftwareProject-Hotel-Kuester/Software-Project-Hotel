package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Zimmer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;


@Entity
@Table(name = "defektmeldung")
public class Defektmeldung {

    /** Höchstlänge, passend zur Spalte SPERRGRUND im Zimmer (255 Zeichen). */
    public static final int MAX_LAENGE = 255;

    /** Wer hat die Meldung angelegt? */
    public enum Quelle {
        GAST("Gast"),
        MITARBEITER("Mitarbeiter");

        private final String bezeichnung;

        Quelle(String bezeichnung) {
            this.bezeichnung = bezeichnung;
        }

        public String getBezeichnung() {
            return bezeichnung;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meldungs_nr")
    private Integer meldungNr;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zimmer_nr", nullable = false)
    private Zimmer zimmer;

    @Column(nullable = false, length = MAX_LAENGE)
    private String beschreibung;

    @Column(name = "gemeldet_am", nullable = false)
    private LocalDate gemeldetAm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Quelle quelle;

    @Column(nullable = false)
    private boolean erledigt;

    @Column(name = "vermerk_behebung", length = MAX_LAENGE)
    private String vermerkBehebung;


    protected Defektmeldung() {
    }

    public Defektmeldung(Zimmer zimmer, String beschreibung, LocalDate gemeldetAm, Quelle quelle) {
        this.zimmer = Objects.requireNonNull(zimmer, "zimmer");
        this.beschreibung = pruefeText(beschreibung);
        this.gemeldetAm = Objects.requireNonNull(gemeldetAm, "gemeldetAm");
        this.quelle = Objects.requireNonNull(quelle, "quelle");
    }

    public void alsErledigtMarkieren(String vermerk) {
        this.erledigt = true;
        if (vermerk == null || vermerk.isBlank()) {
            this.vermerkBehebung = null;
        } else {
            String text = vermerk.strip();
            this.vermerkBehebung = text.length() > MAX_LAENGE ? text.substring(0, MAX_LAENGE) : text;
        }
    }

    /** UC27 ohne Vermerk (wie im Klassendiagramm). */
    public void alsErledigtMarkieren() {
        alsErledigtMarkieren(null);
    }

    private static String pruefeText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Die Beschreibung darf nicht leer sein.");
        }
        String bereinigt = text.strip();
        if (bereinigt.length() > MAX_LAENGE) {
            throw new IllegalArgumentException("Die Beschreibung darf höchstens " + MAX_LAENGE + " Zeichen lang sein.");
        }
        return bereinigt;
    }

    public Integer getMeldungNr() {
        return meldungNr;
    }

    public Zimmer getZimmer() {
        return zimmer;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public LocalDate getGemeldetAm() {
        return gemeldetAm;
    }

    public Quelle getQuelle() {
        return quelle;
    }

    public boolean isErledigt() {
        return erledigt;
    }

    public String getVermerkBehebung() {
        return vermerkBehebung;
    }
}
