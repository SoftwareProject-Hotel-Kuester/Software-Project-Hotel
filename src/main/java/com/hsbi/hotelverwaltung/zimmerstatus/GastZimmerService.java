package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * UC07 PflegeZimmerStatus: Der Gast pflegt den Status seines Zimmers im Gästeportal.
 *
 * <ul>
 *   <li>Standard: Das Zimmer wird täglich gereinigt ("zu reinigen", siehe UC25).</li>
 *   <li>E1: Der Gast wählt "nicht reinigen". Das geht von 12 bis 11 Uhr, nicht während
 *       der Reinigung von 11 bis 12 Uhr. Der Gast kann die Wahl im selben Zeitfenster
 *       wieder zurücknehmen.</li>
 *   <li>E2: Der Gast meldet einen Defekt. Das Zimmer wird mit seinem Vermerk gesperrt,
 *       weiter geht es mit UC27 (Sperre aufheben) durch einen Mitarbeiter.</li>
 * </ul>
 *
 * Jede Methode prüft, ob die Buchung dem angemeldeten Gast gehört und ob der Aufenthalt
 * gerade läuft. Die Oberfläche allein reicht dafür nicht.
 */
@Service
public class GastZimmerService {

    /** Von 11 bis 12 Uhr wird gereinigt (UC25), dann kann der Gast nichts ändern. */
    static final LocalTime REINIGUNG_BEGINN = LocalTime.of(11, 0);
    static final LocalTime REINIGUNG_ENDE = LocalTime.of(12, 0);

    /** Status, in denen ein Aufenthalt laufen kann. Ohne Check-in (UC22) zählt auch "gebucht". */
    private static final Set<Buchungsstatus> LAUFEND = EnumSet.of(Buchungsstatus.GEBUCHT, Buchungsstatus.EINGECHECKT);

    private final BuchungRepository buchungRepository;
    private final ZimmerstatusRepository zimmerRepository;
    private final DefektmeldungRepository defektmeldungRepository;

    public GastZimmerService(BuchungRepository buchungRepository,
                             ZimmerstatusRepository zimmerRepository,
                             DefektmeldungRepository defektmeldungRepository) {
        this.buchungRepository = buchungRepository;
        this.zimmerRepository = zimmerRepository;
        this.defektmeldungRepository = defektmeldungRepository;
    }

    /**
     * Die laufenden Aufenthalte des Gastes: Anreise heute oder früher, Abreise nach heute,
     * nicht storniert oder ausgecheckt.
     */
    @Transactional(readOnly = true)
    public List<Aufenthalt> laufendeAufenthalte(Gast gast, LocalDate heute) {
        return buchungRepository.findByGast(gast).stream()
                .filter(b -> istLaufend(b, heute))
                .sorted(Comparator.comparing((Buchung b) -> b.getZimmer().getZimmerNr()))
                .map(Aufenthalt::von)
                .toList();
    }

    /**
     * E1: "nicht reinigen" wählen oder zurücknehmen ("bitte reinigen").
     *
     * @param nichtReinigen true für "nicht reinigen", false für die tägliche Reinigung
     * @throws ZimmerstatusException zwischen 11 und 12 Uhr, bei gesperrtem Zimmer
     *                               oder wenn die Buchung nicht zum laufenden Aufenthalt des Gastes gehört
     */
    @Transactional
    public Aufenthalt reinigungWaehlen(Gast gast, int buchungsNr, boolean nichtReinigen, LocalDateTime jetzt) {
        Buchung buchung = laufendeBuchung(gast, buchungsNr, jetzt.toLocalDate());
        if (istReinigungszeit(jetzt.toLocalTime())) {
            throw new ZimmerstatusException("Zwischen 11 und 12 Uhr wird gereinigt. "
                    + "Ab 12 Uhr können Sie Ihre Wahl wieder ändern.");
        }
        Zimmer zimmer = buchung.getZimmer();
        if (zimmer.istGesperrt()) {
            throw new ZimmerstatusException("Ihr Zimmer ist gesperrt. Die Rezeption meldet sich bei Ihnen.");
        }

        if (nichtReinigen) {
            zimmer.reinigungsstatusAendern(Reinigungsstatus.NICHT_REINIGEN);
        } else if (zimmer.getReinigungsstatus() == Reinigungsstatus.NICHT_REINIGEN) {
            // Zurücknehmen: wieder der Standard. "gereinigt" bleibt, UC25 nimmt es morgen wieder auf.
            zimmer.reinigungsstatusAendern(Reinigungsstatus.ZU_REINIGEN);
        }
        zimmerRepository.save(zimmer);
        return Aufenthalt.von(buchung);
    }

    /**
     * E2: Der Gast meldet einen Defekt. Das Zimmer wird mit dem Vermerk gesperrt.
     * Ist es schon gesperrt, wird der Vermerk an den Sperrgrund angehängt.
     *
     * @throws ZimmerstatusException wenn die Beschreibung fehlt oder zu lang ist
     *                               oder die Buchung nicht zum laufenden Aufenthalt des Gastes gehört
     */
    @Transactional
    public Aufenthalt defektMelden(Gast gast, int buchungsNr, String beschreibung, LocalDate heute) {
        if (beschreibung == null || beschreibung.isBlank()) {
            throw new ZimmerstatusException("Bitte beschreiben Sie, was defekt ist.");
        }
        String text = beschreibung.strip();
        if (text.length() > Defektmeldung.MAX_LAENGE) {
            throw new ZimmerstatusException(
                    "Die Beschreibung darf höchstens " + Defektmeldung.MAX_LAENGE + " Zeichen lang sein.");
        }
        Buchung buchung = laufendeBuchung(gast, buchungsNr, heute);
        Zimmer zimmer = buchung.getZimmer();

        defektmeldungRepository.save(new Defektmeldung(zimmer, text, heute, Defektmeldung.Quelle.GAST));

        String sperrgrund = zimmer.istGesperrt() && zimmer.getSperrgrund() != null
                ? zimmer.getSperrgrund() + "; Gast: " + text
                : text;
        zimmer.sperren(kuerzen(sperrgrund));
        zimmerRepository.save(zimmer);
        return Aufenthalt.von(buchung);
    }

    /** true von 11:00 bis 11:59, dann darf der Gast nichts ändern. */
    public static boolean istReinigungszeit(LocalTime uhrzeit) {
        return !uhrzeit.isBefore(REINIGUNG_BEGINN) && uhrzeit.isBefore(REINIGUNG_ENDE);
    }

    // ------------------------------------------------------------------

    private Buchung laufendeBuchung(Gast gast, int buchungsNr, LocalDate heute) {
        return buchungRepository.findById(buchungsNr)
                .filter(b -> b.getGast().istDerselbeWie(gast))
                .filter(b -> istLaufend(b, heute))
                .orElseThrow(() -> new ZimmerstatusException(
                        "Diese Buchung gehört nicht zu Ihrem aktuellen Aufenthalt."));
    }

    private static boolean istLaufend(Buchung buchung, LocalDate heute) {
        return LAUFEND.contains(buchung.getStatus())
                && !buchung.getAnreise().isAfter(heute)
                && buchung.getAbreise().isAfter(heute);
    }

    private static String kuerzen(String text) {
        return text.length() > Defektmeldung.MAX_LAENGE ? text.substring(0, Defektmeldung.MAX_LAENGE) : text;
    }

    /** Lesesicht auf einen Aufenthalt für das Gästeportal, ohne Entities. */
    public record Aufenthalt(
            int buchungsNr,
            int zimmerNr,
            String zimmer,
            LocalDate anreise,
            LocalDate abreise,
            Reinigungsstatus reinigungsstatus,
            boolean gesperrt,
            String sperrgrund) {

        static Aufenthalt von(Buchung buchung) {
            Zimmer z = buchung.getZimmer();
            return new Aufenthalt(buchung.getBuchungsNr(), z.getZimmerNr(), z.getKurzbeschreibung(),
                    buchung.getAnreise(), buchung.getAbreise(), z.getReinigungsstatus(),
                    z.istGesperrt(), z.getSperrgrund());
        }

        /** Hat der Gast "nicht reinigen" gewählt? */
        public boolean nichtReinigen() {
            return reinigungsstatus == Reinigungsstatus.NICHT_REINIGEN;
        }
    }
}
