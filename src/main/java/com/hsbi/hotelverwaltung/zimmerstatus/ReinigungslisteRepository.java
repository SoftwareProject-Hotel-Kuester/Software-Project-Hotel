package com.hsbi.hotelverwaltung.zimmerstatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/** Zugriff auf die Reinigungslisten (UC25). */
public interface ReinigungslisteRepository extends JpaRepository<Reinigungsliste, Integer> {

    /** Die Liste eines Tages, es gibt höchstens eine. */
    Optional<Reinigungsliste> findByDatum(LocalDate datum);

    /** Die neueste Liste, für den Hinweis in der Zimmerstatus-Ansicht. */
    Optional<Reinigungsliste> findFirstByOrderByDatumDesc();
}
