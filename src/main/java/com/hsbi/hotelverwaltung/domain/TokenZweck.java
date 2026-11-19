package com.hsbi.hotelverwaltung.domain;

/**
 * Wofür ein Bestätigungslink gedacht ist. Dieselbe Tabelle dient so für die
 * Registrierung und später für das Setzen eines Passworts.
 */
public enum TokenZweck {

    /** Ein Gast hat sich registriert und bestätigt seine E-Mail-Adresse. */
    KONTO_BESTAETIGEN,

    /** Ein neues Konto bekommt per Link sein erstes Passwort (z. B. Mitarbeiter). */
    PASSWORT_SETZEN
}