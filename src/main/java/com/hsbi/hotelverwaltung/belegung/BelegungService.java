package com.hsbi.hotelverwaltung.belegung;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.verfuegbarkeit.VerfuegbarkeitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gehört zu UC06 (ZeigeBelegungsübersicht).
 *
 * Liefert den Belegungskalender: alle Zimmer (optional gefiltert) mit ihren
 * Buchungen im Zeitraum. Welche Buchungen ein Zimmer belegen, legt der
 * VerfuegbarkeitService (UC04) fest. Hier gilt dieselbe Regel.
 */
@Service
public class BelegungService {

    private final ZimmerRepository zimmerRepository;
    private final BuchungRepository buchungRepository;

    public BelegungService(ZimmerRepository zimmerRepository, BuchungRepository buchungRepository) {
        this.zimmerRepository = zimmerRepository;
        this.buchungRepository = buchungRepository;
    }

    /**
     * Baut den Kalender für den Zeitraum.
     *
     * @param von          erster angezeigter Tag (inklusive), Pflichtfeld
     * @param bis          letzter angezeigter Tag (inklusive), Pflichtfeld, nicht vor "von"
     * @param belegungsart optionaler Filter (null = alle)
     * @param zimmerklasse optionaler Filter (null = alle)
     * @return eine Zeile je passendem Zimmer, sortiert nach Zimmernummer.
     *         Leer, wenn kein Zimmer zum Filter passt (UC06 E1).
     * @throws IllegalArgumentException wenn ein Datum fehlt oder "bis" vor "von" liegt
     */
    @Transactional(readOnly = true)
    public List<BelegungsZeile> erstelleKalender(LocalDate von, LocalDate bis,
                                                 Belegungsart belegungsart, Zimmerklasse zimmerklasse) {
        if (von == null || bis == null) {
            throw new IllegalArgumentException("Bitte Beginn und Ende des Zeitraums angeben.");
        }
        if (bis.isBefore(von)) {
            throw new IllegalArgumentException("Das Ende darf nicht vor dem Beginn liegen.");
        }

        List<Zimmer> zimmer = zimmerRepository.findAll().stream()
                .filter(z -> belegungsart == null || z.getBelegungsart() == belegungsart)
                .filter(z -> zimmerklasse == null || z.getZimmerklasse() == zimmerklasse)
                .sorted(Comparator.comparingInt(Zimmer::getZimmerNr))
                .toList();
        if (zimmer.isEmpty()) {
            return List.of();
        }

        // Eine einzige Abfrage für alle Buchungen im Zeitraum, danach je Zimmer verteilen.
        // Der letzte angezeigte Tag "bis" soll noch als Nacht zählen, deshalb bis + 1 (exklusive).
        List<Buchung> buchungen = buchungRepository.findUeberschneidende(
                von, bis.plusDays(1), VerfuegbarkeitService.BELEGENDE_STATUS);

        Map<Integer, Map<LocalDate, Buchung>> jeZimmer = new HashMap<>();
        for (Buchung buchung : buchungen) {
            Map<LocalDate, Buchung> tage = jeZimmer.computeIfAbsent(
                    buchung.getZimmer().getZimmerNr(), nr -> new HashMap<>());
            for (LocalDate tag = buchung.getAnreise(); tag.isBefore(buchung.getAbreise()); tag = tag.plusDays(1)) {
                if (!tag.isBefore(von) && !tag.isAfter(bis)) {
                    tage.put(tag, buchung);
                }
            }
        }

        return zimmer.stream()
                .map(z -> new BelegungsZeile(z, jeZimmer.getOrDefault(z.getZimmerNr(), Map.of())))
                .toList();
    }
}
