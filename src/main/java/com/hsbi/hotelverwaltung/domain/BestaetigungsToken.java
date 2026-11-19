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

import java.time.Instant;

/**
 * Ein Bestätigungslink für ein Konto, zum Beispiel für die E-Mail-Bestätigung.
 * Gespeichert wird nur der Hash des Geheimnisses, nie das Geheimnis selbst.
 *
 * Begriffe:
 * Hash: Ein Fingerabdruck des Geheimnisses, aus dem sich das Geheimnis nicht
 * zurückrechnen lässt. Beim Klick auf den Link hashen wir das mitgeschickte
 * Geheimnis erneut und vergleichen die beiden Hashes.
 * Instant: Zeitpunkt auf der Zeitachse, unabhängig von Zeitzone und Sommerzeit.
 */
@Entity
public class BestaetigungsToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 des Geheimnisses als Hexzahl (64 Zeichen), eindeutig. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "benutzer_id")
    private Benutzer benutzer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenZweck zweck;

    /** Danach ist der Link abgelaufen. */
    @Column(nullable = false)
    private Instant gueltigBis;

    /** Leer, solange der Link noch nicht benutzt wurde. */
    private Instant eingeloestAm;

    /** Für JPA. */
    protected BestaetigungsToken() {
    }

    public BestaetigungsToken(Benutzer benutzer, TokenZweck zweck,
                              String tokenHash, Instant gueltigBis) {
        this.benutzer = benutzer;
        this.zweck = zweck;
        this.tokenHash = tokenHash;
        this.gueltigBis = gueltigBis;
    }

    /** Gültig heißt: noch nicht benutzt und noch nicht abgelaufen. */
    public boolean istGueltig(Instant jetzt) {
        return eingeloestAm == null && jetzt.isBefore(gueltigBis);
    }

    /** Markiert den Link als benutzt, damit er kein zweites Mal funktioniert. */
    public void einloesen(Instant jetzt) {
        this.eingeloestAm = jetzt;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Benutzer getBenutzer() {
        return benutzer;
    }

    public TokenZweck getZweck() {
        return zweck;
    }

    public Instant getGueltigBis() {
        return gueltigBis;
    }

    public Instant getEingeloestAm() {
        return eingeloestAm;
    }
}