package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Datenbankzugriff auf Buchungen. Spring Data erzeugt die Methoden aus ihrem Namen. */
public interface BuchungRepository extends JpaRepository<Buchung, Integer> {

    /** Alle Buchungen eines Gastes (UC13, UC18). */
    List<Buchung> findByGast(Gast gast);
}
