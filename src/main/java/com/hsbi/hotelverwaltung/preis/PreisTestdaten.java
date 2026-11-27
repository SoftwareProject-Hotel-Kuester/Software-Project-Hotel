package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

// Legt beim Start der App Beispielpreise an, damit alle im Team dieselben Preise sehen.
// Gehört zu UC03 PflegePreise. Ohne Preise könnte UC05 BerechnePreis nichts berechnen.
//
// @Component sagt Spring: "Erzeuge von dieser Klasse beim Start ein Objekt."
// CommandLineRunner bedeutet: Spring ruft run(...) genau einmal auf, sobald die App gestartet ist.
@Component
public class PreisTestdaten implements CommandLineRunner {

    // Über den Service anlegen, damit auch hier die Regeln aus UC03 gelten.
    private final PreisService preisService;

    // Über das Repository prüfen wir, ob schon Preise in der Datenbank sind.
    private final PreisRepository preisRepository;

    // Konstruktor: Spring übergibt Service und Repository automatisch.
    // Bekommt: den PreisService und das PreisRepository. Gibt nichts zurück.
    public PreisTestdaten(PreisService preisService, PreisRepository preisRepository) {
        this.preisService = preisService;
        this.preisRepository = preisRepository;
    }

    // Wird einmal nach dem Start der App aufgerufen und legt die Beispielpreise an.
    // Bekommt: die Startparameter der App (brauchen wir hier nicht). Gibt nichts zurück.
    @Override
    public void run(String... args) {
        // Schritt 1: Gibt es schon Preise, machen wir nichts.
        // Sonst würde bei jedem Neustart derselbe Preis noch einmal angelegt und abgelehnt.
        if (preisRepository.count() > 0) {
            return;
        }

        // Schritt 2: Für jede Kombination aus Klasse und Belegungsart einen Preis ab 01.01.2026.
        // Die Beträge passen zu den Buchungspreisen in den Testdaten der anderen Gruppen
        // (z. B. Standard-Doppelzimmer 120 € pro Nacht).
        LocalDate start = LocalDate.of(2026, 1, 1);

        preisService.preisAnlegen(Zimmerklasse.ECONOMY, Belegungsart.EINZEL, new BigDecimal("80.00"), start);
        preisService.preisAnlegen(Zimmerklasse.ECONOMY, Belegungsart.DOPPEL, new BigDecimal("100.00"), start);
        preisService.preisAnlegen(Zimmerklasse.ECONOMY, Belegungsart.FAMILIE, new BigDecimal("130.00"), start);

        preisService.preisAnlegen(Zimmerklasse.STANDARD, Belegungsart.EINZEL, new BigDecimal("100.00"), start);
        preisService.preisAnlegen(Zimmerklasse.STANDARD, Belegungsart.DOPPEL, new BigDecimal("120.00"), start);
        preisService.preisAnlegen(Zimmerklasse.STANDARD, Belegungsart.FAMILIE, new BigDecimal("160.00"), start);

        preisService.preisAnlegen(Zimmerklasse.PREMIUM, Belegungsart.EINZEL, new BigDecimal("150.00"), start);
        preisService.preisAnlegen(Zimmerklasse.PREMIUM, Belegungsart.DOPPEL, new BigDecimal("190.00"), start);
        preisService.preisAnlegen(Zimmerklasse.PREMIUM, Belegungsart.FAMILIE, new BigDecimal("250.00"), start);

        // Schritt 3: Eine Preisänderung zum Testen.
        // Ab 01.01.2027 kostet das Standard-Doppelzimmer 130 €. Der alte Preis (120 €)
        // endet dadurch automatisch am 31.12.2026 (UC03, E1). Eine Buchung über den
        // Jahreswechsel zeigt so die Berechnung pro Nacht (UC05, E1).
        preisService.preisAnlegen(Zimmerklasse.STANDARD, Belegungsart.DOPPEL, new BigDecimal("130.00"),
                LocalDate.of(2027, 1, 1));
    }
}
