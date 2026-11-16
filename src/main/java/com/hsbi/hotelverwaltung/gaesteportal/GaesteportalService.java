package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fachlogik des Gästeportals (UC17 bis UC21).
 *
 * Wichtig: Jede Methode bekommt den angemeldeten Gast übergeben und prüft
 * selbst, ob die Buchung ihm gehört. Die Oberfläche allein reicht nicht,
 * weil man dort Buchungsnummern manipulieren könnte.
 */
@Service
public class GaesteportalService {

    private final BuchungRepository buchungRepository;
    private final Clock clock;

    @Autowired
    public GaesteportalService(BuchungRepository buchungRepository) {
        this(buchungRepository, Clock.systemDefaultZone());
    }

    /** Für Tests: Mit einer festen Uhr lässt sich "heute" vorgeben. */
    GaesteportalService(BuchungRepository buchungRepository, Clock clock) {
        this.buchungRepository = buchungRepository;
        this.clock = clock;
    }

    public LocalDate heute() {
        return LocalDate.now(clock);
    }

    /**
     * UC18: Alle Buchungen des Gastes. Anstehende zuerst (die nächste Anreise
     * oben), danach vergangene (die jüngste oben).
     */
    public List<Buchung> eigeneBuchungen(Gast gast) {
        LocalDate heute = heute();
        List<Buchung> anstehend = new ArrayList<>();
        List<Buchung> vergangen = new ArrayList<>();
        for (Buchung buchung : buchungRepository.findByGast(gast)) {
            if (buchung.istAnstehend(heute)) {
                anstehend.add(buchung);
            } else {
                vergangen.add(buchung);
            }
        }
        anstehend.sort(Comparator.comparing(Buchung::getAnreise));
        vergangen.sort(Comparator.comparing(Buchung::getAnreise).reversed());

        List<Buchung> ergebnis = new ArrayList<>(anstehend);
        ergebnis.addAll(vergangen);
        return ergebnis;
    }

    /**
     * UC18 Schritt 4: Eine einzelne Buchung des Gastes. Wird auch von UC19
     * und UC20 genutzt, damit niemand fremde Buchungen ändern kann.
     *
     * @throws BuchungNichtGefundenException wenn es die Buchung nicht gibt
     *         oder sie einem anderen Gast gehört
     */
    public Buchung eigeneBuchung(Gast gast, Long buchungsNr) {
        Buchung buchung = buchungRepository.findById(buchungsNr)
                .orElseThrow(() -> new BuchungNichtGefundenException(buchungsNr));
        if (!buchung.getGast().istDerselbeWie(gast)) {
            throw new BuchungNichtGefundenException(buchungsNr);
        }
        return buchung;
    }
}
