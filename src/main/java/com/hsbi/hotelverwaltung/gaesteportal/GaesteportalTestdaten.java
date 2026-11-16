package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Adresse;
import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Legt beim Start Testdaten für das Gästeportal an, falls sie noch fehlen.
 * Die Daten passen zu den Mockups (Max Mustermann, Zimmer 101, 201, 304).
 *
 * Erika Musterfrau ist ein zweiter Gast. Mit ihr lässt sich zeigen, dass
 * Max ihre Buchung nicht sehen kann.
 *
 * TODO: Mit dem Team abstimmen, ob es später eine gemeinsame Testdaten-Klasse gibt.
 */
@Component
public class GaesteportalTestdaten implements CommandLineRunner {

    static final String TESTGAST_EMAIL = "max.mustermann@example.com";
    static final String ZWEITER_GAST_EMAIL = "erika.musterfrau@example.com";

    private final GastRepository gastRepository;
    private final ZimmerRepository zimmerRepository;
    private final BuchungRepository buchungRepository;

    public GaesteportalTestdaten(GastRepository gastRepository, ZimmerRepository zimmerRepository,
                                 BuchungRepository buchungRepository) {
        this.gastRepository = gastRepository;
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
    }

    @Override
    public void run(String... args) {
        if (gastRepository.findByEmail(TESTGAST_EMAIL).isPresent()) {
            return; // schon angelegt
        }

        Zimmer z101 = zimmer("101", 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);
        Zimmer z201 = zimmer("201", 2, Belegungsart.FAMILIE, Zimmerklasse.PREMIUM);
        Zimmer z304 = zimmer("304", 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);

        Gast max = gast(TESTGAST_EMAIL, "Max", "Mustermann", LocalDate.of(1990, 5, 12),
                new Adresse("Hauptstraße", "1", "33602", "Bielefeld", "Deutschland"));
        Gast erika = gast(ZWEITER_GAST_EMAIL, "Erika", "Musterfrau", LocalDate.of(1985, 8, 3),
                new Adresse("Bahnhofstraße", "5", "33602", "Bielefeld", "Deutschland"));

        LocalDate heute = LocalDate.now();

        // Max: eine vergangene, zwei anstehende (eine davon storniert)
        Buchung vergangen = new Buchung(max, z101, heute.minusDays(40), heute.minusDays(37), new BigDecimal("240.00"));
        vergangen.setStatus(Buchungsstatus.AUSGECHECKT);
        Buchung anstehend = new Buchung(max, z304, heute.plusDays(14), heute.plusDays(17), new BigDecimal("360.00"));
        Buchung storniert = new Buchung(max, z201, heute.plusDays(30), heute.plusDays(32), new BigDecimal("500.00"));
        storniert.setStatus(Buchungsstatus.STORNIERT);

        // Erika: darf bei Max nicht auftauchen
        Buchung vonErika = new Buchung(erika, z201, heute.plusDays(3), heute.plusDays(5), new BigDecimal("500.00"));

        buchungRepository.save(vergangen);
        buchungRepository.save(anstehend);
        buchungRepository.save(storniert);
        buchungRepository.save(vonErika);
    }

    /** Legt das Zimmer nur an, wenn eine andere Gruppe es nicht schon angelegt hat. */
    private Zimmer zimmer(String nr, int etage, Belegungsart art, Zimmerklasse klasse) {
        return zimmerRepository.findById(nr)
                .orElseGet(() -> zimmerRepository.save(new Zimmer(nr, etage, art, klasse)));
    }

    private Gast gast(String email, String vorname, String nachname, LocalDate geburtsdatum, Adresse adresse) {
        Gast gast = new Gast(email, vorname, nachname, geburtsdatum);
        gast.setAktiv(true);
        gast.setTelefon("0521 123456");
        gast.setAdresse(adresse);
        gast.setHatKonto(true);
        return gastRepository.save(gast);
    }
}
