package com.hsbi.hotelverwaltung.registrierung;

import java.time.LocalDate;

// Alle Eingaben aus dem Registrierungsformular in einem Objekt (ein "record", wie GastDaten).
// Ein Passwort gehört nicht dazu: Das legt der Gast erst nach der E-Mail-Bestätigung fest.
public record RegistrierungsDaten(
        String vorname,
        String nachname,
        LocalDate geburtsdatum,
        String email) {
}