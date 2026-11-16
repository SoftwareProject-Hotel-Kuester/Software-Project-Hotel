package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Datenbankzugriff auf Gästeprofile. */
public interface GastRepository extends JpaRepository<Gast, Long> {

    /** Sucht den Gast über seine E-Mail-Adresse (zugleich Anmeldename). */
    Optional<Gast> findByEmail(String email);
}
