package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.GastRepository;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Testdaten für UC07: Der Testgast "gast@sparrenhof.test" (Passwort wie in
 * BenutzerTestdaten) bekommt einen laufenden Aufenthalt (gestern bis in drei Tagen),
 * damit man die Zimmerreinigung im Gästeportal ausprobieren kann.
 *
 * <p>Läuft nach den anderen Testdaten und nur, wenn der Gast noch keinen laufenden
 * Aufenthalt hat. Ein Zimmer mit einer anderen Buchung im Zeitraum wird nicht genommen.
 */
@Component
public class GastAufenthaltTestdaten {

    static final String TESTGAST_EMAIL = "gast@sparrenhof.test";

    private static final Logger log = LoggerFactory.getLogger(GastAufenthaltTestdaten.class);
    private static final EnumSet<Buchungsstatus> BELEGEND = EnumSet.of(Buchungsstatus.GEBUCHT, Buchungsstatus.EINGECHECKT);

    private final GastRepository gastRepository;
    private final BuchungRepository buchungRepository;
    private final ZimmerstatusRepository zimmerRepository;

    public GastAufenthaltTestdaten(GastRepository gastRepository, BuchungRepository buchungRepository,
                                   ZimmerstatusRepository zimmerRepository) {
        this.gastRepository = gastRepository;
        this.buchungRepository = buchungRepository;
        this.zimmerRepository = zimmerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Transactional
    public void aufenthaltAnlegen() {
        Optional<Gast> gefunden = gastRepository.findByEmail(TESTGAST_EMAIL);
        if (gefunden.isEmpty()) {
            return;
        }
        Gast gast = gefunden.get();
        LocalDate heute = LocalDate.now();
        LocalDate anreise = heute.minusDays(1);
        LocalDate abreise = heute.plusDays(3);

        boolean hatAufenthalt = buchungRepository.findByGast(gast).stream()
                .anyMatch(b -> BELEGEND.contains(b.getStatus())
                        && !b.getAnreise().isAfter(heute) && b.getAbreise().isAfter(heute));
        if (hatAufenthalt) {
            return;
        }

        // Ein freies oder schon belegtes Zimmer ohne andere Buchung im Zeitraum, belegte zuerst
        Optional<Zimmer> zimmer = zimmerRepository.findAllByOrderByEtageAscZimmerNrAsc().stream()
                .filter(z -> !z.istGesperrt())
                .filter(z -> !buchungRepository.existsByZimmerAndStatusInAndAnreiseBeforeAndAbreiseAfter(
                        z, BELEGEND, abreise, anreise))
                .min(Comparator.comparing((Zimmer z) -> !z.istBelegt()));
        if (zimmer.isEmpty()) {
            log.info("Kein freies Zimmer für den Test-Aufenthalt von {} gefunden.", TESTGAST_EMAIL);
            return;
        }

        Zimmer z = zimmer.get();
        if (!z.istBelegt()) {
            z.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT); // Check-in geht nur gereinigt (UC22)
            z.einchecken();
            zimmerRepository.save(z);
        }
        Buchung buchung = new Buchung(gast, z, anreise, abreise, new BigDecimal("400.00"));
        buchung.setStatus(Buchungsstatus.EINGECHECKT);
        buchungRepository.save(buchung);
        log.info("Test-Aufenthalt für {} in Zimmer {} angelegt.", TESTGAST_EMAIL, z.getZimmerNr());
    }
}
