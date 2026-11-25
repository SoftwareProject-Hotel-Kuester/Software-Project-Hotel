package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Adresse;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.zimmer.ZimmerTestdaten;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

// Diese Klasse legt beim Start der App Testdaten für das Gästeportal an:
// zwei Gäste und ihre Buchungen. Gehört zu UC18 ZeigeEigeneBuchungen.
//
// - Max Mustermann (wie im Mockup) hat eine vergangene, zwei anstehende und eine stornierte Buchung.
//   Die anstehenden kann er stornieren (UC20).
// - Erika Musterfrau hat eine eigene Buchung. Damit lässt sich zeigen,
//   dass Max ihre Buchung nicht sehen kann.
// - Gina Gast (gast@sparrenhof.test, aus BenutzerTestdaten) bekommt zwei anstehende Buchungen,
//   damit man UC20 auch mit ihr testen kann. Siehe buchungenFuerGinaAnlegen().
//
// Beide können sich anmelden, mit ihrer E-Mail und dem Passwort "Test1234!"
// (dasselbe Passwort wie bei den Testkonten aus BenutzerTestdaten).
//
// Die Zimmer legen wir nicht selbst an, sondern nutzen die aus ZimmerTestdaten (101 bis 303).
//
// @Component + CommandLineRunner: Spring ruft run(...) einmal auf, sobald die App gestartet ist.
@Component
public class GaesteportalTestdaten implements CommandLineRunner {

    // E-Mail des Test-Gastes Max.
    static final String TESTGAST_EMAIL = "max.mustermann@example.com";

    // E-Mail von Gina. Sie wird in BenutzerTestdaten (security) angelegt, nicht hier.
    private static final String GINA_EMAIL = "gast@sparrenhof.test";

    // Passwort der Test-Gäste. Nur für die Entwicklung, nie für eine echte Installation.
    private static final String TEST_PASSWORT = "Test1234!";

    private final GastRepository gastRepository;
    private final ZimmerRepository zimmerRepository;
    private final BuchungRepository buchungRepository;
    private final ZimmerTestdaten zimmerTestdaten;
    // PasswordEncoder: verschlüsselt das Passwort, bevor es gespeichert wird.
    // Im Klartext darf es nie in der Datenbank stehen.
    private final PasswordEncoder passwordEncoder;

    // Konstruktor: Spring übergibt alle Repositories, ZimmerTestdaten und den PasswordEncoder automatisch.
    public GaesteportalTestdaten(GastRepository gastRepository, ZimmerRepository zimmerRepository,
                                 BuchungRepository buchungRepository, ZimmerTestdaten zimmerTestdaten,
                                 PasswordEncoder passwordEncoder) {
        this.gastRepository = gastRepository;
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
        this.zimmerTestdaten = zimmerTestdaten;
        this.passwordEncoder = passwordEncoder;
    }

    // Wird einmal nach dem Start der App aufgerufen.
    @Override
    public void run(String... args) {
        // Schritt 1: Gibt es Max schon, sind die Testdaten schon da. Dann nichts tun.
        if (gastRepository.findByEmail(TESTGAST_EMAIL).isPresent()) {
            return;
        }

        // Schritt 2: Sicherstellen, dass die Zimmer da sind.
        // In welcher Reihenfolge Spring die Testdaten-Klassen startet, ist nicht festgelegt.
        // ZimmerTestdaten legt nur Zimmer an, wenn es noch keine gibt. Ein doppelter Aufruf schadet also nicht.
        zimmerTestdaten.run();

        Zimmer zimmer101 = zimmerRepository.findById(101).orElseThrow();   // Einzel, Economy
        Zimmer zimmer202 = zimmerRepository.findById(202).orElseThrow();   // Doppel, Standard
        Zimmer zimmer303 = zimmerRepository.findById(303).orElseThrow();   // Familie, Premium
        Zimmer zimmer201 = zimmerRepository.findById(201).orElseThrow();   // Einzel, Standard

        // Schritt 3: Die beiden Gäste anlegen.
        Gast max = gastAnlegen(TESTGAST_EMAIL, "Max", "Mustermann", LocalDate.of(1990, 5, 12),
                new Adresse("Hauptstraße", "1", "33602", "Bielefeld", "Deutschland"));
        Gast erika = gastAnlegen("erika.musterfrau@example.com", "Erika", "Musterfrau", LocalDate.of(1985, 8, 3),
                new Adresse("Bahnhofstraße", "5", "33602", "Bielefeld", "Deutschland"));

        // Schritt 4: Buchungen anlegen. Die Daten hängen vom heutigen Tag ab,
        // damit es immer vergangene und anstehende Buchungen gibt.
        LocalDate heute = LocalDate.now();

        Buchung vergangen = new Buchung(max, zimmer101, heute.minusDays(40), heute.minusDays(37), new BigDecimal("240.00"));
        vergangen.setStatus(Buchungsstatus.AUSGECHECKT);

        Buchung anstehend = new Buchung(max, zimmer202, heute.plusDays(14), heute.plusDays(17), new BigDecimal("360.00"));

        Buchung storniert = new Buchung(max, zimmer303, heute.plusDays(30), heute.plusDays(32), new BigDecimal("500.00"));
        storniert.setStatus(Buchungsstatus.STORNIERT);

        Buchung vonErika = new Buchung(erika, zimmer303, heute.plusDays(3), heute.plusDays(5), new BigDecimal("500.00"));

        // Zweite anstehende Buchung von Max, damit man UC20 (Stornieren) mehr als einmal ausprobieren kann.
        Buchung zumStornieren = new Buchung(max, zimmer201, heute.plusDays(20), heute.plusDays(22), new BigDecimal("180.00"));

        buchungRepository.save(vergangen);
        buchungRepository.save(anstehend);
        buchungRepository.save(storniert);
        buchungRepository.save(vonErika);
        buchungRepository.save(zumStornieren);
    }

    // UC20: Gina bekommt zwei anstehende Buchungen, die sie stornieren kann.
    // @EventListener(ApplicationReadyEvent.class): Spring ruft die Methode auf, wenn die App
    // komplett gestartet ist. Dann haben alle anderen Testdaten-Klassen Gina und die Zimmer
    // sicher schon angelegt.
    // Hat Gina noch eine Buchung mit Status "gebucht", passiert nichts. Hat sie alle storniert,
    // bekommt sie beim nächsten Start neue. So kann man immer wieder testen.
    @EventListener(ApplicationReadyEvent.class)
    public void buchungenFuerGinaAnlegen() {
        Optional<Gast> gina = gastRepository.findByEmail(GINA_EMAIL);
        if (gina.isEmpty()) {
            return;
        }

        for (Buchung buchung : buchungRepository.findByGast(gina.get())) {
            if (buchung.getStatus() == Buchungsstatus.GEBUCHT) {
                return;
            }
        }

        LocalDate heute = LocalDate.now();
        Zimmer zimmer103 = zimmerRepository.findById(103).orElseThrow();   // Familie, Economy
        Zimmer zimmer301 = zimmerRepository.findById(301).orElseThrow();   // Einzel, Premium

        buchungRepository.save(new Buchung(gina.get(), zimmer103, heute.plusDays(10), heute.plusDays(12), new BigDecimal("300.00")));
        buchungRepository.save(new Buchung(gina.get(), zimmer301, heute.plusDays(25), heute.plusDays(28), new BigDecimal("450.00")));
    }

    // Legt einen Gast mit Konto an und speichert ihn.
    // Bekommt: E-Mail, Vorname, Nachname, Geburtsdatum und Adresse.
    // Gibt zurück: den gespeicherten Gast.
    private Gast gastAnlegen(String email, String vorname, String nachname, LocalDate geburtsdatum, Adresse adresse) {
        Gast gast = new Gast(email, vorname, nachname, geburtsdatum);
        gast.setPasswort(passwordEncoder.encode(TEST_PASSWORT));
        gast.setAktiv(true);
        gast.setTelefon("0521 123456");
        gast.setAdresse(adresse);
        gast.setHatKonto(true);
        return gastRepository.save(gast);
    }
}
