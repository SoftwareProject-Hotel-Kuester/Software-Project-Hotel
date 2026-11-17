package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Beispiel-Status zum Ausprobieren, damit alle dieselben Daten sehen (siehe README).
 * Legt KEINE Zimmer an (das ist UC01), sondern setzt nur bei schon vorhandenen
 * Zimmern verschiedene Status: gereinigt, belegt, nicht reinigen, gesperrt.
 *
 * <p>Läuft erst, wenn die App fertig gestartet ist, also nach den Testdaten der
 * anderen. Nur einmal: Gibt es schon eine Defektmeldung, passiert nichts.
 */
@Component
public class ZimmerstatusTestdaten {

    private static final Logger log = LoggerFactory.getLogger(ZimmerstatusTestdaten.class);

    private final ZimmerstatusRepository zimmerRepository;
    private final DefektmeldungRepository defektmeldungRepository;

    public ZimmerstatusTestdaten(ZimmerstatusRepository zimmerRepository,
                                 DefektmeldungRepository defektmeldungRepository) {
        this.zimmerRepository = zimmerRepository;
        this.defektmeldungRepository = defektmeldungRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void statusSetzen() {
        List<Zimmer> zimmer = zimmerRepository.findAllByOrderByEtageAscZimmerNrAsc();
        if (zimmer.isEmpty() || defektmeldungRepository.count() > 0) {
            return; // noch keine Zimmer da oder schon einmal gelaufen
        }

        // Reihum verschiedene Status verteilen, damit jeder Filter etwas zeigt (UC24)
        for (int i = 0; i < zimmer.size(); i++) {
            Zimmer z = zimmer.get(i);
            if (z.istBelegt() || z.istGesperrt()) {
                continue; // Status von anderen Teilen nicht überschreiben
            }
            switch (i % 5) {
                case 0 -> z.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
                case 1 -> belegen(z, Reinigungsstatus.ZU_REINIGEN);
                case 2 -> belegen(z, Reinigungsstatus.NICHT_REINIGEN);
                case 3 -> sperren(z, "Fernseher defekt", Defektmeldung.Quelle.GAST);
                default -> { } // bleibt frei und "zu reinigen"
            }
            zimmerRepository.save(z);
        }
        log.info("Beispiel-Status für {} Zimmer gesetzt.", zimmer.size());
    }

    /** Wie nach dem Check-in (UC22): geht nur bei gereinigtem Zimmer. */
    private void belegen(Zimmer zimmer, Reinigungsstatus danach) {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        zimmer.einchecken();
        zimmer.reinigungsstatusAendern(danach);
    }

    private void sperren(Zimmer zimmer, String grund, Defektmeldung.Quelle quelle) {
        zimmer.sperren(grund);
        defektmeldungRepository.save(new Defektmeldung(zimmer, grund, LocalDate.now(), quelle));
    }
}
