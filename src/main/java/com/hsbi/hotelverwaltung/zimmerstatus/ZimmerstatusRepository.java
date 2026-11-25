package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Abfragen auf die gemeinsame Zimmer-Tabelle, die nur der Zimmerstatus braucht.
 * Eigenes Repository, damit das gemeinsame {@code ZimmerRepository}
 * unverändert bleibt. Spring Data erzeugt die Abfragen aus den Methodennamen.
 */
public interface ZimmerstatusRepository extends JpaRepository<Zimmer, Integer> {

    /** Alle Zimmer, sortiert nach Etage und Zimmernummer (UC24). */
    List<Zimmer> findAllByOrderByEtageAscZimmerNrAsc();

    /** Zimmer mit einer bestimmten Belegung, z. B. alle gesperrten (UC27). */
    List<Zimmer> findByZimmerstatusOrderByZimmerNrAsc(Zimmerstatus zimmerstatus);

    /**
     * Zimmer mit einem Reinigungsstatus, die nicht den angegebenen Zimmerstatus haben,
     * z. B. alle "zu reinigen", die nicht gesperrt sind (UC25).
     */
    List<Zimmer> findByReinigungsstatusAndZimmerstatusNotOrderByEtageAscZimmerNrAsc(
            Reinigungsstatus reinigungsstatus, Zimmerstatus zimmerstatus);

    /** Zimmer mit genau dieser Belegung und Reinigung, z. B. belegt und gereinigt (UC25, Bleibegäste). */
    List<Zimmer> findByZimmerstatusAndReinigungsstatus(Zimmerstatus zimmerstatus, Reinigungsstatus reinigungsstatus);
}
