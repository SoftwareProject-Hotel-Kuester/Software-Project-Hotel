package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Liefert den Gast, der gerade im Gästeportal angemeldet ist.
// Alle Seiten des Gästeportals fragen hier nach, statt das selbst zu lösen.
//
// Den Login hat Gruppe 6 gebaut (UC15). Beim Anmelden ist der Benutzername
// die E-Mail-Adresse. Über diese E-Mail suchen wir den passenden Gast.
//
// @Component: Spring erzeugt beim Start ein Objekt dieser Klasse
// und übergibt es automatisch an die Views.
@Component
public class AktuellerGast {

    // AuthenticationContext (von Vaadin): weiß, wer gerade angemeldet ist.
    private final AuthenticationContext authContext;
    private final GastRepository gastRepository;

    // Konstruktor: Spring übergibt beides automatisch.
    public AktuellerGast(AuthenticationContext authContext, GastRepository gastRepository) {
        this.authContext = authContext;
        this.gastRepository = gastRepository;
    }

    // Gibt den angemeldeten Gast zurück.
    // Optional bedeutet: Es kann auch "kein Gast" herauskommen,
    // z. B. wenn niemand angemeldet ist oder ein Mitarbeiter angemeldet ist.
    // Gibt zurück: den Gast oder ein leeres Optional.
    public Optional<Gast> get() {
        // Schritt 1: E-Mail des angemeldeten Benutzers holen (leer, wenn niemand angemeldet ist).
        Optional<String> email = authContext.getPrincipalName();
        if (email.isEmpty()) {
            return Optional.empty();
        }
        // Schritt 2: Den Gast mit dieser E-Mail aus der Datenbank laden.
        return gastRepository.findByEmail(email.get());
    }
}
