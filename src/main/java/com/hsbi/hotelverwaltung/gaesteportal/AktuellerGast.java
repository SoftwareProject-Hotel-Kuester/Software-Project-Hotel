package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Liefert den Gast, der gerade im Gästeportal angemeldet ist.
 *
 * Solange es noch keinen Login gibt (Gruppe 6), wird ersatzweise der
 * Test-Gast aus {@link GaesteportalTestdaten} verwendet. Sobald der Login
 * steht, greift automatisch der obere Zweig und der Platzhalter kann raus.
 */
@Component
public class AktuellerGast {

    private final GastRepository gastRepository;

    public AktuellerGast(GastRepository gastRepository) {
        this.gastRepository = gastRepository;
    }

    public Optional<Gast> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Der Anmeldename ist die E-Mail-Adresse (UC15).
            return gastRepository.findByEmail(auth.getName());
        }
        // TODO: entfernen, sobald der Login fertig ist
        return gastRepository.findByEmail(GaesteportalTestdaten.TESTGAST_EMAIL);
    }
}
