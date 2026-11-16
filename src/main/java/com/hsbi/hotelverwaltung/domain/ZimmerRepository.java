package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Datenbankzugriff auf Zimmer. Schlüssel ist die Zimmernummer. */
public interface ZimmerRepository extends JpaRepository<Zimmer, String> {
}
