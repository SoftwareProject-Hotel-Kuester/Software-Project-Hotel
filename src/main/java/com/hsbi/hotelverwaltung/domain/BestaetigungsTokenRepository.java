package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Zugriff auf die Bestätigungslinks in der Datenbank.
 */
public interface BestaetigungsTokenRepository extends JpaRepository<BestaetigungsToken, Long> {

    /** Sucht den Token zu einem Hash. Leer, wenn es ihn nicht gibt. */
    Optional<BestaetigungsToken> findByTokenHash(String tokenHash);
}