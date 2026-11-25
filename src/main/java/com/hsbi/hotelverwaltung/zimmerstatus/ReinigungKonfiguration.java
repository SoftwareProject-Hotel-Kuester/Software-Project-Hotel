package com.hsbi.hotelverwaltung.zimmerstatus;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Schaltet die zeitgesteuerten Aufgaben von Spring ein, damit die tägliche
 * Reinigung (UC25) um 11 und 12 Uhr von selbst startet.
 */
@Configuration
@EnableScheduling
class ReinigungKonfiguration {
}
