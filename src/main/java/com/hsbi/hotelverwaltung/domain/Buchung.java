package com.hsbi.hotelverwaltung.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Eine Buchung eines Gastes für ein Zimmer und einen Zeitraum.
 * stornieren() kommt aus UC20 und kann auch für UC10 genutzt werden.
 * Weitere fachliche Methoden wie einchecken() ergänzen die Gruppen 3 und 4.
 */
@Entity
public class Buchung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer buchungsNr;

    @Column(nullable = false)
    private LocalDate anreise;

    @Column(nullable = false)
    private LocalDate abreise;

    /** Wird beim Buchen festgeschrieben und ändert sich bei Preisänderungen nicht (UC03). */
    @Column(precision = 10, scale = 2)
    private BigDecimal gesamtpreis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Buchungsstatus status;

    private LocalDate erstelltAm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gast_id", nullable = false)
    private Gast gast;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zimmer_nr", nullable = false)
    private Zimmer zimmer;

    /** Für JPA. */
    protected Buchung() {
    }

    public Buchung(Gast gast, Zimmer zimmer, LocalDate anreise, LocalDate abreise, BigDecimal gesamtpreis) {
        if (!abreise.isAfter(anreise)) {
            throw new IllegalArgumentException("Die Abreise muss nach der Anreise liegen.");
        }
        this.gast = gast;
        this.zimmer = zimmer;
        this.anreise = anreise;
        this.abreise = abreise;
        this.gesamtpreis = gesamtpreis;
        this.status = Buchungsstatus.GEBUCHT;
        this.erstelltAm = LocalDate.now();
    }

    /** Anzahl der Übernachtungen, z. B. 15.06. bis 18.06. = 3 Nächte. */
    public long anzahlNaechte() {
        return ChronoUnit.DAYS.between(anreise, abreise);
    }

    /** Anstehend oder laufend: Die Abreise ist heute oder später. */
    public boolean istAnstehend(LocalDate heute) {
        return !abreise.isBefore(heute);
    }

    /**
     * Darf der Gast die Buchung selbst ändern oder stornieren (UC19, UC20)?
     * Nur im Status "gebucht" und wenn die Anreise noch in der Zukunft liegt.
     */
    public boolean kannVomGastGeaendertWerden(LocalDate heute) {
        return status == Buchungsstatus.GEBUCHT && anreise.isAfter(heute);
    }

    /**
     * Storniert die Buchung (UC20, später auch UC10 durch Mitarbeiter).
     * Nur eine Buchung im Status "gebucht" kann storniert werden.
     * Das Zimmer ist danach automatisch wieder frei, weil die
     * Verfügbarkeitsprüfung stornierte Buchungen nicht mitzählt.
     */
    public void stornieren() {
        if (status != Buchungsstatus.GEBUCHT) {
            throw new IllegalStateException("Nur gebuchte Buchungen können storniert werden.");
        }
        status = Buchungsstatus.STORNIERT;
    }

    public Integer getBuchungsNr() {
        return buchungsNr;
    }

    public LocalDate getAnreise() {
        return anreise;
    }

    public void setAnreise(LocalDate anreise) {
        this.anreise = anreise;
    }

    public LocalDate getAbreise() {
        return abreise;
    }

    public void setAbreise(LocalDate abreise) {
        this.abreise = abreise;
    }

    public BigDecimal getGesamtpreis() {
        return gesamtpreis;
    }

    public void setGesamtpreis(BigDecimal gesamtpreis) {
        this.gesamtpreis = gesamtpreis;
    }

    public Buchungsstatus getStatus() {
        return status;
    }

    public void setStatus(Buchungsstatus status) {
        this.status = status;
    }

    public LocalDate getErstelltAm() {
        return erstelltAm;
    }

    public Gast getGast() {
        return gast;
    }

    public Zimmer getZimmer() {
        return zimmer;
    }

    public void setZimmer(Zimmer zimmer) {
        this.zimmer = zimmer;
    }
}
