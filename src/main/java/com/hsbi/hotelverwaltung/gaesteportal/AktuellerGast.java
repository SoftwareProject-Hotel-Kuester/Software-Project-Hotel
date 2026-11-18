package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Liefert den Gast, der gerade im Gästeportal angemeldet ist.
// Alle Seiten des Gästeportals fragen hier nach, statt das selbst zu lösen.
//
// Den Login baut Gruppe 6 (UC15). Bis dahin geben wir immer den Test-Gast
// "Max Mustermann" aus GaesteportalTestdaten zurück. Sobald der Login fertig ist,
// muss nur diese eine Klasse geändert werden.
//
// @Component: Spring erzeugt beim Start ein Objekt dieser Klasse
// und übergibt es automatisch an die Views.
@Component
public class AktuellerGast {

    private final GastRepository gastRepository;

    // Konstruktor: Spring übergibt das Repository automatisch.
    public AktuellerGast(GastRepository gastRepository) {
        this.gastRepository = gastRepository;
    }

    // Gibt den angemeldeten Gast zurück.
    // Optional bedeutet: Es kann auch "kein Gast" herauskommen, z. B. wenn niemand angemeldet ist.
    // Gibt zurück: den Gast oder ein leeres Optional.
    public Optional<Gast> get() {
        // TODO: Sobald der Login von Gruppe 6 fertig ist, hier die E-Mail des
        // angemeldeten Benutzers aus Spring Security verwenden.
        return gastRepository.findByEmail(GaesteportalTestdaten.TESTGAST_EMAIL);
    }
}
