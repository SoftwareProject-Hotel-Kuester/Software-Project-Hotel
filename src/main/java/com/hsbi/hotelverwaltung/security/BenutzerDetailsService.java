package com.hsbi.hotelverwaltung.security;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Mitarbeiter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lädt beim Login die Person aus der Datenbank und übersetzt sie in das
 * Format, das Spring Security versteht. Den Passwortvergleich übernimmt
 * Spring Security danach selbst.
 *
 * Begriffe:
 * UserDetails: Spring-Security-Objekt mit Anmeldename, Passwort-Hash,
 * Rollen und Sperrstatus.
 */
@Service
public class BenutzerDetailsService implements UserDetailsService {

    private final BenutzerRepository benutzerRepository;

    // Spring übergibt das Repository automatisch.
    public BenutzerDetailsService(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
    }

    // Wird von Spring Security beim Login aufgerufen. "username" ist bei uns
    // die E-Mail-Adresse. Wirft UsernameNotFoundException, wenn niemand passt.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Benutzer benutzer = benutzerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        // Profile ohne Portal-Konto (z. B. von einem Mitarbeiter angelegt) haben
        // noch kein Passwort. Für Spring Security gelten sie als nicht vorhanden.
        if (benutzer.getPasswort() == null) {
            throw new UsernameNotFoundException("Benutzer nicht gefunden");
        }

        return User.builder()
                .username(benutzer.getEmail())
                .password(benutzer.getPasswort())
                // roles("GAST") speichert intern "ROLE_GAST". Das Präfix setzt Spring selbst.
                .roles(rolleVon(benutzer))
                // Ein gesperrtes Konto weist Spring Security vor dem Passwortvergleich ab.
                .disabled(!benutzer.isAktiv())
                .build();
    }

    // Gäste haben keine gespeicherte Rolle, sie ergibt sich aus der Klasse.
    // Bei Mitarbeitern kommt sie aus dem Feld "rolle".
    private String rolleVon(Benutzer benutzer) {
        if (benutzer instanceof Mitarbeiter mitarbeiter) {
            return mitarbeiter.getRolle().name();
        }
        return "GAST";
    }
}