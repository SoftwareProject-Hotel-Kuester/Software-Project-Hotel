package com.hsbi.hotelverwaltung.gast;

import com.hsbi.hotelverwaltung.domain.Gast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Abfragen auf die gemeinsame Gast-Tabelle, die nur die Gästeverwaltung braucht (UC11, UC12).
// Eigenes Repository, damit das gemeinsame domain.GastRepository unverändert bleibt
// (so hat es auch der Zimmerstatus mit ZimmerstatusRepository gemacht).
//
// Spring schreibt den Code für diese Methoden selbst. Es liest dafür den Methodennamen:
// "findByEmailIgnoreCase" = "suche nach E-Mail, Groß-/Kleinschreibung egal".
// save, findAll und findById gibt es automatisch durch JpaRepository.
public interface GastprofilRepository extends JpaRepository<Gast, Integer> {

    // Sucht einen Gast mit dieser E-Mail-Adresse (UC11, Dublettenprüfung).
    // Bekommt: die E-Mail-Adresse.
    // Gibt zurück: den Gast oder ein leeres Optional, wenn es keinen gibt.
    Optional<Gast> findByEmailIgnoreCase(String email);

    // Sucht Gäste mit gleichem Vornamen, Nachnamen und Geburtsdatum (UC11, Dublettenprüfung).
    // Bekommt: Vorname, Nachname und Geburtsdatum.
    // Gibt zurück: Liste der passenden Gäste (leer, wenn es keine gibt).
    List<Gast> findByVornameIgnoreCaseAndNachnameIgnoreCaseAndGeburtsdatum(
            String vorname, String nachname, LocalDate geburtsdatum);
}
