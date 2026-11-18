package com.hsbi.hotelverwaltung.security;

import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Mitarbeiter;
import com.hsbi.hotelverwaltung.domain.Rolle;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.hsbi.hotelverwaltung.domain.Benutzer;
import java.time.LocalDate;

/**
 * Legt beim Start drei Konten zum Ausprobieren des Logins an.
 * Nur für die Entwicklung gedacht: Das gemeinsame Passwort steht im Code
 * und darf nie für eine echte Installation gelten.
 */
@Component
public class BenutzerTestdaten implements CommandLineRunner {

    // Gemeinsames Passwort aller Testkonten, nur lokal verwenden.
    private static final String TEST_PASSWORT = "Test1234!";

    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring übergibt Repository und PasswordEncoder automatisch.
    public BenutzerTestdaten(BenutzerRepository benutzerRepository,
                             PasswordEncoder passwordEncoder) {
        this.benutzerRepository = benutzerRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public void run(String... args) {
        // Jedes Konto wird einzeln geprüft. Andere Testdaten in der Tabelle
        // (z. B. aus anderen Branches) stören dadurch nicht.
        if (!existiert("gast@sparrenhof.test")) {
            Gast gast = new Gast("gast@sparrenhof.test", "Gina", "Gast",
                    LocalDate.of(1990, 5, 17));
            gast.setHatKonto(true);
            konfigurieren(gast);
            benutzerRepository.save(gast);
        }

        if (!existiert("mitarbeiter@sparrenhof.test")) {
            Mitarbeiter mitarbeiter = new Mitarbeiter("mitarbeiter@sparrenhof.test",
                    "Max", "Mitarbeiter", Rolle.MITARBEITER, LocalDate.of(2022, 3, 1));
            konfigurieren(mitarbeiter);
            benutzerRepository.save(mitarbeiter);
        }

        if (!existiert("leitung@sparrenhof.test")) {
            Mitarbeiter leitung = new Mitarbeiter("leitung@sparrenhof.test",
                    "Lena", "Leitung", Rolle.HOTELLEITUNG, LocalDate.of(2019, 1, 1));
            konfigurieren(leitung);
            benutzerRepository.save(leitung);
        }
    }

    // Prüft, ob es die E-Mail-Adresse schon gibt, ohne auf die Schreibweise zu achten.
    private boolean existiert(String email) {
        return benutzerRepository.findByEmailIgnoreCase(email).isPresent();
    }

    // Setzt das gehashte Passwort und schaltet das Konto frei.
    private void konfigurieren(Benutzer benutzer) {
        benutzer.setPasswort(passwordEncoder.encode(TEST_PASSWORT));
        benutzer.setAktiv(true);
    }
}