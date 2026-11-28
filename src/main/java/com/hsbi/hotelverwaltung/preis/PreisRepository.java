package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Datenbankzugriff auf Preise. Gehört zu UC03 PflegePreise und UC05 BerechnePreis.
//
// extends JpaRepository<Preis, Integer>: Spring Data erzeugt die Klasse dazu selbst.
// Methoden wie save, findById, findAll und delete gibt es dadurch automatisch.
// "Integer" ist der Typ des Schlüssels (preisNr).
public interface PreisRepository extends JpaRepository<Preis, Integer> {

    // Alle Preise einer Kombination aus Zimmerklasse und Belegungsart, sortiert nach Startdatum.
    // Spring Data baut die Datenbankabfrage aus dem Methodennamen:
    // findBy... = suche, Zimmerklasse/Belegungsart = Bedingungen, OrderByGueltigAbAsc = aufsteigend sortiert.
    // Gebraucht für die Überschneidungsprüfung (UC03, Schritt 4) und die Berechnung pro Nacht (UC05).
    // Bekommt: Zimmerklasse und Belegungsart.
    // Gibt zurück: Liste der Preise, der älteste zuerst (leer, wenn es noch keinen gibt).
    List<Preis> findByZimmerklasseAndBelegungsartOrderByGueltigAbAsc(Zimmerklasse zimmerklasse,
                                                                     Belegungsart belegungsart);
}
