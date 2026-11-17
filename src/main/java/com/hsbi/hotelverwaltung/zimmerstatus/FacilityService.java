package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Steuerung für Zimmerstatus und Sperren (in den Sequenzdiagrammen "FacilityService").
 *
 * <ul>
 *   <li>UC24 ZeigeZimmerstatus: {@link #ladeAlleZimmer}, {@link #ladeKennzahlen}</li>
 *   <li>UC26 SperreZimmer: {@link #ladeSperrdaten}, {@link #zimmerSperren}</li>
 *   <li>UC27 HebeZimmersperreAuf: {@link #ladeGesperrteZimmer}, {@link #zimmerFreigeben}</li>
 * </ul>
 */
@Service
public class FacilityService {

    private static final Comparator<Defektmeldung> NACH_MELDUNGSNUMMER = Comparator.comparing(
            Defektmeldung::getMeldungNr, Comparator.nullsFirst(Comparator.<Integer>naturalOrder()));

    private final ZimmerstatusRepository zimmerRepository;
    private final DefektmeldungRepository defektmeldungRepository;

    public FacilityService(ZimmerstatusRepository zimmerRepository,
                           DefektmeldungRepository defektmeldungRepository) {
        this.zimmerRepository = zimmerRepository;
        this.defektmeldungRepository = defektmeldungRepository;
    }


    // UC24 ZeigeZimmerstatus
    /**
     * Alle Zimmer nach Etage mit ihrem Status, optional gefiltert (UC24, E1).
     *
     * @param filter Statusfilter, {@code null} bedeutet alle Zimmer
     */
    @Transactional(readOnly = true)
    public List<ZimmerInfo> ladeAlleZimmer(Statusfilter filter) {
        Statusfilter aktiv = filter == null ? Statusfilter.ALLE : filter;
        Map<Integer, Defektmeldung> offeneMeldungen = offeneMeldungenJeZimmer();
        return zimmerRepository.findAllByOrderByEtageAscZimmerNrAsc().stream()
                .filter(aktiv::passt)
                .map(z -> ZimmerInfo.von(z, offeneMeldungen.get(z.getZimmerNr())))
                .toList();
    }

    /** Zahlen für den Kopf der Übersicht ("4 zu reinigen · 1 gesperrt"). */
    @Transactional(readOnly = true)
    public Kennzahlen ladeKennzahlen() {
        List<Zimmer> alle = zimmerRepository.findAll();
        return new Kennzahlen(
                alle.size(),
                anzahl(alle, Statusfilter.BELEGT),
                anzahl(alle, Statusfilter.ZU_REINIGEN),
                anzahl(alle, Statusfilter.GESPERRT));
    }

    // ------------------------------------------------------------------
    // UC26 SperreZimmer
    // ------------------------------------------------------------------

    /** Aktueller Status und Sperrgrund eines Zimmers (UC26, Schritt 4). */
    @Transactional(readOnly = true)
    public ZimmerInfo ladeSperrdaten(int zimmerNr) {
        Zimmer zimmer = findeZimmer(zimmerNr);
        return ZimmerInfo.von(zimmer, offeneMeldung(zimmer).orElse(null));
    }

    /**
     * Sperrt ein Zimmer mit Grund (UC26) oder ändert den Sperrgrund eines schon
     * gesperrten Zimmers (UC26, E1). Der Status bleibt dann "gesperrt".
     *
     * @throws ZimmerstatusException wenn der Sperrgrund fehlt oder das Zimmer unbekannt ist
     */
    @Transactional
    public void zimmerSperren(int zimmerNr, String sperrgrund) {
        String grund = pflichttext(sperrgrund, "Der Sperrgrund ist ein Pflichtfeld.");
        Zimmer zimmer = findeZimmer(zimmerNr);

        // Neue Sperre: festhalten, wer wann gesperrt hat
        if (!zimmer.istGesperrt() && offeneMeldung(zimmer).isEmpty()) {
            defektmeldungRepository.save(
                    new Defektmeldung(zimmer, grund, LocalDate.now(), Defektmeldung.Quelle.MITARBEITER));
        }
        zimmer.sperren(grund);
        zimmerRepository.save(zimmer);
    }

    // ------------------------------------------------------------------
    // UC27 HebeZimmersperreAuf
    // ------------------------------------------------------------------

    /** Alle gesperrten Zimmer mit Sperrgrund (UC27, Schritt 2). Leer bei E1. */
    @Transactional(readOnly = true)
    public List<ZimmerInfo> ladeGesperrteZimmer() {
        Map<Integer, Defektmeldung> offeneMeldungen = offeneMeldungenJeZimmer();
        return zimmerRepository.findByZimmerstatusOrderByZimmerNrAsc(Zimmerstatus.GESPERRT).stream()
                .map(z -> ZimmerInfo.von(z, offeneMeldungen.get(z.getZimmerNr())))
                .toList();
    }

    /**
     * Beendet die Sperre (UC27, Schritt 6): Das Zimmer ist frei, steht auf
     * "zu reinigen" und ist wieder buchbar. Offene Defektmeldungen sind erledigt.
     *
     * @param vermerk optionaler Vermerk zur Behebung
     * @throws ZimmerstatusException wenn das Zimmer nicht gesperrt ist
     */
    @Transactional
    public void zimmerFreigeben(int zimmerNr, String vermerk) {
        if (vermerk != null && vermerk.strip().length() > Defektmeldung.MAX_LAENGE) {
            throw new ZimmerstatusException(
                    "Der Vermerk darf höchstens " + Defektmeldung.MAX_LAENGE + " Zeichen lang sein.");
        }
        Zimmer zimmer = findeZimmer(zimmerNr);
        if (!zimmer.istGesperrt()) {
            throw new ZimmerstatusException("Zimmer " + zimmerNr + " ist nicht gesperrt.");
        }

        zimmer.sperreAufheben();
        zimmerRepository.save(zimmer);

        List<Defektmeldung> meldungen = defektmeldungRepository.findByZimmerAndErledigtFalse(zimmer);
        meldungen.forEach(meldung -> meldung.alsErledigtMarkieren(vermerk));
        defektmeldungRepository.saveAll(meldungen);
    }

    // ------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------

    private Zimmer findeZimmer(int zimmerNr) {
        return zimmerRepository.findById(zimmerNr)
                .orElseThrow(() -> new ZimmerstatusException("Zimmer " + zimmerNr + " gibt es nicht."));
    }

    private Optional<Defektmeldung> offeneMeldung(Zimmer zimmer) {
        return defektmeldungRepository.findFirstByZimmerAndErledigtFalseOrderByMeldungNrDesc(zimmer);
    }

    /** Alle offenen Meldungen in einer Abfrage, je Zimmer die neueste. */
    private Map<Integer, Defektmeldung> offeneMeldungenJeZimmer() {
        return defektmeldungRepository.findByErledigtFalse().stream()
                .collect(Collectors.toMap(
                        meldung -> meldung.getZimmer().getZimmerNr(),
                        Function.identity(),
                        BinaryOperator.maxBy(NACH_MELDUNGSNUMMER)));
    }

    private static int anzahl(List<Zimmer> zimmer, Statusfilter filter) {
        return (int) zimmer.stream().filter(filter::passt).count();
    }

    private static String pflichttext(String text, String fehlermeldung) {
        if (text == null || text.isBlank()) {
            throw new ZimmerstatusException(fehlermeldung);
        }
        String bereinigt = text.strip();
        if (bereinigt.length() > Defektmeldung.MAX_LAENGE) {
            throw new ZimmerstatusException(
                    "Der Text darf höchstens " + Defektmeldung.MAX_LAENGE + " Zeichen lang sein.");
        }
        return bereinigt;
    }

    /** Kennzahlen für die Übersicht. */
    public record Kennzahlen(int zimmer, int belegt, int zuReinigen, int gesperrt) {
    }
}
