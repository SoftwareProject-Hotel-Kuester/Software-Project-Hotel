package com.hsbi.hotelverwaltung.gast;

import java.time.LocalDate;

// Alle Eingaben aus der Maske "Gästeprofil anlegen" in einem Objekt. Gehört zu UC11 LegeGastprofilAn.
//
// Warum? Ein Gast hat elf Eingabefelder. Statt elf einzelne Parameter an jede Methode
// zu übergeben, packen wir sie hier zusammen. Später kann UC12 BearbeiteGastprofil
// dieselbe Klasse benutzen.
//
// "record" ist eine Kurzschreibweise von Java für reine Datenklassen:
// Java erzeugt Konstruktor und Getter automatisch.
// Zugriff z. B. mit daten.vorname() (ohne "get").
//
// Pflicht: vorname, nachname, geburtsdatum, email, strasse, hausnummer, plz, ort.
// Optional: telefon, land (sonst "Deutschland"), praeferenzen.
public record GastDaten(
        String vorname,
        String nachname,
        LocalDate geburtsdatum,
        String email,
        String telefon,
        String strasse,
        String hausnummer,
        String plz,
        String ort,
        String land,
        String praeferenzen) {
}
