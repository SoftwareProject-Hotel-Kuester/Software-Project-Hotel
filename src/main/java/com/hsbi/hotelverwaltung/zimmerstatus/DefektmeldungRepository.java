package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Zimmer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface DefektmeldungRepository extends JpaRepository<Defektmeldung, Integer> {

    /** Die neueste offene Meldung eines Zimmers: wer wann gemeldet hat (UC24, UC26). */
    Optional<Defektmeldung> findFirstByZimmerAndErledigtFalseOrderByMeldungNrDesc(Zimmer zimmer);

    /** Alle offenen Meldungen eines Zimmers, werden beim Entsperren erledigt (UC27). */
    List<Defektmeldung> findByZimmerAndErledigtFalse(Zimmer zimmer);

    /** Alle offenen Meldungen, für die Übersicht in einer einzigen Abfrage (UC24). */
    List<Defektmeldung> findByErledigtFalse();
}
