package com.hsbi.hotelverwaltung.belegung;

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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Testdaten für UC06: ein paar Buchungen rund um heute, damit der Belegungskalender
 * etwas zeigt (gebucht, eingecheckt, storniert). Die Daten hängen vom heutigen Tag ab.
 */
@Component
public class BelegungTestdaten implements CommandLineRunner {

    private static final String GAST_EMAIL = "lisa.beispiel@example.com";

    private final GastRepository gastRepository;
    private final ZimmerRepository zimmerRepository;
    private final BuchungRepository buchungRepository;
    private final ZimmerTestdaten zimmerTestdaten;

    public BelegungTestdaten(GastRepository gastRepository, ZimmerRepository zimmerRepository,
                             BuchungRepository buchungRepository, ZimmerTestdaten zimmerTestdaten) {
        this.gastRepository = gastRepository;
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
        this.zimmerTestdaten = zimmerTestdaten;
    }

    @Override
    public void run(String... args) {
        if (gastRepository.findByEmail(GAST_EMAIL).isPresent()) {
            return;
        }
        zimmerTestdaten.run();

        Gast lisa = gast(GAST_EMAIL, "Lisa", "Beispiel");
        Gast tom = gast("tom.test@example.com", "Tom", "Test");

        LocalDate heute = LocalDate.now();
        Zimmer z101 = zimmerRepository.findById(101).orElseThrow();
        Zimmer z202 = zimmerRepository.findById(202).orElseThrow();
        Zimmer z303 = zimmerRepository.findById(303).orElseThrow();

        Buchung laufend = new Buchung(lisa, z101, heute.minusDays(1), heute.plusDays(3), new BigDecimal("320.00"));
        laufend.setStatus(Buchungsstatus.EINGECHECKT);
        Buchung kommend = new Buchung(tom, z202, heute.plusDays(2), heute.plusDays(5), new BigDecimal("360.00"));
        Buchung folge = new Buchung(lisa, z101, heute.plusDays(3), heute.plusDays(6), new BigDecimal("240.00"));
        Buchung storniert = new Buchung(tom, z303, heute.plusDays(1), heute.plusDays(3), new BigDecimal("500.00"));
        storniert.setStatus(Buchungsstatus.STORNIERT);

        buchungRepository.save(laufend);
        buchungRepository.save(kommend);
        buchungRepository.save(folge);
        buchungRepository.save(storniert);
    }

    private Gast gast(String email, String vorname, String nachname) {
        Gast gast = new Gast(email, vorname, nachname, LocalDate.of(1992, 3, 4));
        gast.setAktiv(true);
        gast.setTelefon("0521 654321");
        gast.setAdresse(new Adresse("Marktplatz", "2", "33602", "Bielefeld", "Deutschland"));
        gast.setHatKonto(true);
        return gastRepository.save(gast);
    }
}
