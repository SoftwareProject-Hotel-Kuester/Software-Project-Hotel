package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.TokenZweck;
import com.hsbi.hotelverwaltung.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Regeln der Selbstregistrierung für Gäste. Ablauf: registrieren() legt ein
 * Profil ohne Passwort an und schickt einen Link. Über den Link legt der Gast
 * mit passwortSetzen() sein Passwort fest, erst dann ist das Konto aktiv.
 *
 * Begriffe:
 * Profil ohne Konto: Gast ohne Passwort. Entsteht bei der Registrierung und
 * wenn ein Mitarbeiter einen Gast anlegt (UC11).
 * Ersteinrichtung: Das erste Passwort eines Kontos. Nur dann wird das Konto freigeschaltet.
 */
@Service
public class RegistrationService {

    private static final int MIN_PASSWORT_LAENGE = 8;
    // BCrypt verarbeitet höchstens 72 Byte. Längere Passwörter lehnen wir ab,
    // statt sie still abzuschneiden.
    private static final int MAX_PASSWORT_BYTES = 72;
    private static final int MAX_NAME_LAENGE = 255;

    private final BenutzerRepository benutzerRepository;
    private final TokenService tokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final String basisUrl;

    // Spring übergibt alles automatisch, die Basisadresse kommt aus der application.yaml.
    public RegistrationService(BenutzerRepository benutzerRepository,
                               TokenService tokenService,
                               MailService mailService,
                               PasswordEncoder passwordEncoder,
                               @Value("${hotel.basis-url}") String basisUrl) {
        this.benutzerRepository = benutzerRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.basisUrl = basisUrl;
    }

    // Registriert einen Gast. Von außen ist das Ergebnis immer gleich (nichts wird
    // zurückgegeben), damit niemand herausfinden kann, wer schon Kunde ist.
    // Wirft eine IllegalArgumentException, wenn eine Eingabe ungültig ist.
    @Transactional
    public void registrieren(RegistrierungsDaten daten) {
        pruefeEingaben(daten);
        String email = daten.email().trim().toLowerCase();

        Optional<Benutzer> vorhanden = benutzerRepository.findByEmailIgnoreCase(email);

        if (vorhanden.isEmpty()) {
            // Neue Adresse: Profil ohne Passwort und ohne Konto anlegen.
            Gast neu = new Gast(email, daten.vorname().trim(), daten.nachname().trim(),
                    daten.geburtsdatum());
            benutzerRepository.save(neu);
            linkSchicken(neu);
        } else if (vorhanden.get() instanceof Gast gast && gast.getPasswort() == null) {
            // Profil ohne Konto (von einem Mitarbeiter oder früher selbst angelegt):
            // Es wird nichts überschrieben, der Gast bekommt nur einen Link.
            linkSchicken(gast);
        } else {
            // Es gibt schon ein Konto mit Passwort (oder die Adresse gehört einem
            // Mitarbeiter): Der Besitzer bekommt nur einen Hinweis.
            Benutzer benutzer = vorhanden.get();
            mailService.sendeKontoBereitsVorhanden(benutzer.getEmail(),
                    benutzer.getVorname(), basisUrl + "/login");
        }
    }

    // Legt das Passwort fest und schaltet bei der Ersteinrichtung das Konto frei.
    // Gibt true zurück, wenn es geklappt hat, und false, wenn der Link ungültig,
    // abgelaufen oder schon benutzt ist. Ein zu kurzes Passwort wirft eine
    // IllegalArgumentException, der Link bleibt dann gültig.
    @Transactional
    public boolean passwortSetzen(String geheimnis, String passwort) {
        pruefePasswort(passwort);

        Optional<Benutzer> gefunden = tokenService.einloesen(geheimnis, TokenZweck.PASSWORT_SETZEN);
        if (gefunden.isEmpty()) {
            return false;
        }

        Benutzer benutzer = gefunden.get();
        boolean ersteinrichtung = benutzer.getPasswort() == null;
        benutzer.setPasswort(passwordEncoder.encode(passwort));
        // Ein gesperrtes Konto bleibt gesperrt, wenn jemand später sein Passwort ändert.
        if (ersteinrichtung) {
            benutzer.setAktiv(true);
            if (benutzer instanceof Gast gast) {
                gast.setHatKonto(true);
            }
        }
        benutzerRepository.save(benutzer);
        return true;
    }

    // Erzeugt den Link und schickt ihn per Mail.
    private void linkSchicken(Gast gast) {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.PASSWORT_SETZEN);
        mailService.sendeBestaetigungsLink(gast.getEmail(), gast.getVorname(),
                basisUrl + "/passwort-setzen?token=" + geheimnis);
    }

    private void pruefeEingaben(RegistrierungsDaten daten) {
        pruefeName(daten.vorname(), "Bitte einen Vornamen eingeben.");
        pruefeName(daten.nachname(), "Bitte einen Nachnamen eingeben.");
        if (daten.geburtsdatum() == null) {
            throw new IllegalArgumentException("Bitte ein Geburtsdatum eingeben.");
        }
        if (daten.geburtsdatum().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Das Geburtsdatum darf nicht in der Zukunft liegen.");
        }
        if (daten.email() == null || daten.email().isBlank()) {
            throw new IllegalArgumentException("Bitte eine E-Mail-Adresse eingeben.");
        }
        // Einfache Formatprüfung wie in GastService: ein @, davor etwas, danach Domain mit Punkt.
        if (!daten.email().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben, z. B. max@beispiel.de.");
        }
    }

    private void pruefeName(String name, String fehlermeldung) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fehlermeldung);
        }
        if (name.trim().length() > MAX_NAME_LAENGE) {
            throw new IllegalArgumentException("Der Name darf höchstens " + MAX_NAME_LAENGE + " Zeichen lang sein.");
        }
    }

    private void pruefePasswort(String passwort) {
        if (passwort == null || passwort.length() < MIN_PASSWORT_LAENGE) {
            throw new IllegalArgumentException(
                    "Das Passwort muss mindestens " + MIN_PASSWORT_LAENGE + " Zeichen lang sein.");
        }
        if (passwort.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORT_BYTES) {
            throw new IllegalArgumentException(
                    "Das Passwort ist zu lang (höchstens " + MAX_PASSWORT_BYTES + " Byte, Umlaute zählen doppelt).");
        }
    }
}