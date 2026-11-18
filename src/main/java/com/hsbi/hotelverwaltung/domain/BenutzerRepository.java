package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Zugriff auf alle Benutzer (Gäste und Mitarbeiter) in der Datenbank.
 * Der Login braucht ihn, weil er die Person über die E-Mail-Adresse sucht
 * und nicht wissen kann, ob sie Gast oder Mitarbeiter ist.
 */
public interface BenutzerRepository extends JpaRepository<Benutzer, Integer> {

    /**
     * Sucht eine Person über ihre E-Mail-Adresse, ohne auf Groß- und
     * Kleinschreibung zu achten. Optional ist leer, wenn niemand passt.
     */
    Optional<Benutzer> findByEmailIgnoreCase(String email);
}