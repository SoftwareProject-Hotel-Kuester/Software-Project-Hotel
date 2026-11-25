package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Die Reinigungsliste eines Tages (UC25, laut Domänen-Klassendiagramm).
 *
 * <p>Um 11 Uhr kommen alle Zimmer mit "zu reinigen" auf die Liste und die Liste
 * geht an das Reinigungsteam. Um 12 Uhr setzt {@link #statusAktualisieren()} genau
 * diese Zimmer auf "gereinigt". Pro Tag gibt es höchstens eine Liste.
 */
@Entity
@Table(name = "reinigungsliste")
public class Reinigungsliste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private LocalDate datum;

    @ManyToMany
    @JoinTable(name = "reinigungsliste_zimmer",
            joinColumns = @JoinColumn(name = "reinigungsliste_id"),
            inverseJoinColumns = @JoinColumn(name = "zimmer_nr"))
    @OrderBy("etage ASC, zimmerNr ASC")
    private List<Zimmer> zimmer = new ArrayList<>();

    /** Ob die E-Mail an das Reinigungsteam rausging (Schritt 2). */
    @Column(nullable = false)
    private boolean versendet;

    /** Zeitpunkt, an dem die Status angepasst wurden (Schritte 4 und 5), sonst {@code null}. */
    @Column(name = "abgeschlossen_um")
    private LocalDateTime abgeschlossenUm;

    @Column(name = "anzahl_gereinigt", nullable = false)
    private int anzahlGereinigt;

    @Column(name = "anzahl_neu_zu_reinigen", nullable = false)
    private int anzahlNeuZuReinigen;

    /** Für JPA. */
    protected Reinigungsliste() {
    }

    public Reinigungsliste(LocalDate datum) {
        this.datum = Objects.requireNonNull(datum, "datum");
    }

    /** Nimmt ein Zimmer auf die Liste (Schritt 1). Doppelte Einträge werden ignoriert. */
    public void zimmerHinzufuegen(Zimmer zimmer) {
        Objects.requireNonNull(zimmer, "zimmer");
        if (istAbgeschlossen()) {
            throw new IllegalStateException("Die Reinigungsliste vom " + datum + " ist schon abgeschlossen.");
        }
        if (!this.zimmer.contains(zimmer)) {
            this.zimmer.add(zimmer);
        }
    }

    /** Merkt sich, dass die Liste an das Reinigungsteam verschickt wurde (Schritt 2). */
    public void alsVersendetMarkieren() {
        this.versendet = true;
    }

    /**
     * Setzt die Zimmer der Liste auf "gereinigt" (Schritt 4).
     *
     * <p>Übersprungen werden Zimmer, die seit 11 Uhr gesperrt wurden oder nicht mehr
     * "zu reinigen" sind: Die wurden nicht gereinigt.
     *
     * @return Anzahl der Zimmer, die jetzt "gereinigt" sind
     */
    public int statusAktualisieren() {
        if (istAbgeschlossen()) {
            throw new IllegalStateException("Die Reinigungsliste vom " + datum + " ist schon abgeschlossen.");
        }
        int gereinigt = 0;
        for (Zimmer z : zimmer) {
            if (!z.istGesperrt() && z.getReinigungsstatus() == Reinigungsstatus.ZU_REINIGEN) {
                z.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
                gereinigt++;
            }
        }
        this.anzahlGereinigt = gereinigt;
        return gereinigt;
    }

    /** Schließt die Liste ab und hält das Ergebnis für die Meldung an den Mitarbeiter fest (Schritt 6). */
    public void abschliessen(int anzahlNeuZuReinigen, LocalDateTime zeitpunkt) {
        this.anzahlNeuZuReinigen = anzahlNeuZuReinigen;
        this.abgeschlossenUm = Objects.requireNonNull(zeitpunkt, "zeitpunkt");
    }

    public boolean istAbgeschlossen() {
        return abgeschlossenUm != null;
    }

    public Integer getId() {
        return id;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public List<Zimmer> getZimmer() {
        return Collections.unmodifiableList(zimmer);
    }

    public boolean isVersendet() {
        return versendet;
    }

    public LocalDateTime getAbgeschlossenUm() {
        return abgeschlossenUm;
    }

    public int getAnzahlGereinigt() {
        return anzahlGereinigt;
    }

    public int getAnzahlNeuZuReinigen() {
        return anzahlNeuZuReinigen;
    }
}
