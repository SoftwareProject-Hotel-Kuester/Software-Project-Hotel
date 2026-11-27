package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;

// Ein Nachtpreis für eine Kombination aus Zimmerklasse und Belegungsart, der ab einem Datum gilt.
// Beispiel: "Standard, Doppelzimmer, 120,00 € pro Nacht, gültig ab 01.01.2027".
// Gehört zu UC03 PflegePreise. Die Preisberechnung (UC05 BerechnePreis) liest diese Preise.
//
// Ein Preis gilt nicht für ein einzelnes Zimmer, sondern für alle Zimmer mit derselben
// Klasse und Belegungsart. So muss die Hotelleitung nicht jedes Zimmer einzeln bepreisen.
//
// @Entity: Spring/Hibernate legt für diese Klasse eine Tabelle "preis" an.
// Jedes Preis-Objekt entspricht einer Zeile in dieser Tabelle.
@Entity
public class Preis {

    // @Id: Die Preisnummer ist der Schlüssel, über den ein Preis eindeutig gefunden wird.
    // @GeneratedValue: Die Datenbank vergibt die Nummer selbst (1, 2, 3, ...).
    // Wir nehmen eine einfache Nummer statt Klasse + Belegungsart + gültigAb als Schlüssel,
    // weil das in JPA viel einfacher ist. Doppelte Preise verhindert der PreisService (UC03, Schritt 4).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer preisNr;

    // Für welche Zimmerklasse und Belegungsart der Preis gilt.
    // @Enumerated(EnumType.STRING): Der Wert wird als Text gespeichert (z. B. "STANDARD"),
    // damit die Datenbank lesbar bleibt.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zimmerklasse zimmerklasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Belegungsart belegungsart;

    // Preis für eine Nacht. Geldbeträge immer als BigDecimal, weil double beim Rechnen
    // Rundungsfehler macht (z. B. 0.1 + 0.2 = 0.30000000000000004).
    // precision = 10, scale = 2: bis zu 10 Stellen, davon 2 hinter dem Komma.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal nachtpreis;

    // Ab diesem Tag gilt der Preis (Pflichtangabe in UC03, Schritt 3).
    @Column(nullable = false)
    private LocalDate gueltigAb;

    // Bis zu diesem Tag gilt der Preis (einschließlich).
    // Leer (null) heißt: Der Preis gilt unbegrenzt, bis ein neuer Preis ihn beendet (UC03, E1).
    private LocalDate gueltigBis;

    // Leerer Konstruktor, den JPA braucht, um Preise aus der Datenbank zu laden.
    // protected: Im eigenen Code soll ihn niemand benutzen.
    protected Preis() {
    }

    // Legt einen neuen Preis an, der ab gueltigAb unbegrenzt gilt (UC03, Schritt 3).
    // Bekommt: Zimmerklasse, Belegungsart, Nachtpreis und den ersten gültigen Tag.
    // Das Ende (gueltigBis) bleibt leer. Es wird erst gesetzt, wenn ein neuer Preis folgt.
    public Preis(Zimmerklasse zimmerklasse, Belegungsart belegungsart, BigDecimal nachtpreis, LocalDate gueltigAb) {
        this.zimmerklasse = zimmerklasse;
        this.belegungsart = belegungsart;
        this.nachtpreis = nachtpreis;
        this.gueltigAb = gueltigAb;
    }

    // Prüft, ob dieser Preis an einem bestimmten Tag gilt.
    // Gebraucht für UC05 (welcher Preis gilt in dieser Nacht?) und UC03, Schritt 4 (Überschneidung).
    // Bekommt: das Datum, das geprüft wird.
    // Gibt zurück: true, wenn der Tag zwischen gueltigAb und gueltigBis liegt (beide Tage zählen mit),
    // sonst false. Ist gueltigBis leer, gilt der Preis ab gueltigAb ohne Ende.
    public boolean giltAm(LocalDate datum) {
        // Schritt 1: Vor dem ersten gültigen Tag gilt der Preis noch nicht.
        if (datum.isBefore(gueltigAb)) {
            return false;
        }

        // Schritt 2: Ohne Enddatum gilt der Preis ab gueltigAb für immer.
        if (gueltigBis == null) {
            return true;
        }

        // Schritt 3: Mit Enddatum gilt der Preis nur bis einschließlich gueltigBis.
        return !datum.isAfter(gueltigBis);
    }

    // Beendet diesen Preis an einem bestimmten Tag (UC03, E1 Schritt 7).
    // Wird aufgerufen, wenn ein neuer Preis beginnt: Der alte Preis endet am Vortag des neuen.
    // So gilt an jedem Tag genau ein Preis, und die Preisberechnung (UC05) ist eindeutig.
    // Bekommt: den letzten Tag, an dem der Preis noch gelten soll. Gibt nichts zurück.
    // Wirft eine IllegalArgumentException, wenn der letzte Tag vor gueltigAb liegt.
    public void beenden(LocalDate letzterTag) {
        // Regel: Der Preis muss mindestens einen Tag gültig bleiben.
        // Ein Ende vor dem Anfang wäre ein Preis, der nie gilt.
        if (letzterTag.isBefore(gueltigAb)) {
            throw new IllegalArgumentException("Der Preis kann nicht vor seinem Gültigkeitsbeginn ("
                    + gueltigAb + ") enden.");
        }
        this.gueltigBis = letzterTag;
    }

    // Getter: geben die gespeicherten Werte zurück.
    // Es gibt bewusst keine Setter. Geändert wird ein Preis nur über Fachmethoden,
    // die als Nächstes folgen (z. B. den Preis zu einem Datum beenden).

    public Integer getPreisNr() {
        return preisNr;
    }

    public Zimmerklasse getZimmerklasse() {
        return zimmerklasse;
    }

    public Belegungsart getBelegungsart() {
        return belegungsart;
    }

    public BigDecimal getNachtpreis() {
        return nachtpreis;
    }

    public LocalDate getGueltigAb() {
        return gueltigAb;
    }

    public LocalDate getGueltigBis() {
        return gueltigBis;
    }
}
