package com.hsbi.hotelverwaltung.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Datenbankzugriff auf Buchungen. Spring Data erzeugt die Methoden aus ihrem Namen. */
public interface BuchungRepository extends JpaRepository<Buchung, Integer> {

    /** Alle Buchungen eines Gastes (UC13, UC18). */
    List<Buchung> findByGast(Gast gast);

    /** Alle Buchungen eines Zimmers, egal mit welchem Status (UC02: künftige Buchungen prüfen, Zimmer löschen). */
    List<Buchung> findByZimmer(Zimmer zimmer);

    /**
     * Gibt es für das Zimmer eine Buchung, die sich mit dem Zeitraum überschneidet (UC04)?
     * Zwei Zeiträume überschneiden sich, wenn die vorhandene Anreise vor der
     * gewünschten Abreise liegt UND die vorhandene Abreise nach der gewünschten
     * Anreise. Reist ein Gast am selben Tag ab, an dem der nächste anreist,
     * gilt das nicht als Überschneidung.
     *
     * @param zimmer das Zimmer, das geprüft wird
     * @param status nur Buchungen mit diesen Status zählen (z. B. keine stornierten)
     * @param bis    gewünschte Abreise
     * @param von    gewünschte Anreise
     */
    boolean existsByZimmerAndStatusInAndAnreiseBeforeAndAbreiseAfter(
            Zimmer zimmer, Collection<Buchungsstatus> status, LocalDate bis, LocalDate von);
}
