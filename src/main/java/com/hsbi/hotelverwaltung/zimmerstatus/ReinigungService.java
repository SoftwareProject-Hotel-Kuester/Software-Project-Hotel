package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * UC25 SteuereTäglicheReinigung (siehe Sequenzdiagramm SteuereTaeglicheReinigung).
 *
 * <ol>
 *   <li>11 Uhr: {@link #listeErstellenUndVersenden} nimmt alle Zimmer mit "zu reinigen"
 *       auf die Reinigungsliste und schickt sie per E-Mail an das Reinigungsteam.
 *       Belegte Zimmer, die schon "gereinigt" sind, kommen ebenfalls dazu: Bleibegäste
 *       bekommen täglich eine Reinigung, außer sie haben "nicht reinigen" gewählt (UC07).</li>
 *   <li>12 Uhr: {@link #statusAktualisieren} setzt die Zimmer der Liste auf "gereinigt"
 *       und danach alle "nicht reinigen" auf "zu reinigen". Die Reihenfolge ist wichtig,
 *       sonst würden die "nicht reinigen"-Zimmer am selben Tag auf "gereinigt" springen.</li>
 *   <li>Das Ergebnis steht in der Liste und wird dem Mitarbeiter in der
 *       Zimmerstatus-Ansicht angezeigt ({@link #ladeLetzteReinigung}).</li>
 * </ol>
 *
 * Gesperrte Zimmer werden nicht gereinigt und bleiben unverändert.
 */
@Service
public class ReinigungService {

    private static final Logger log = LoggerFactory.getLogger(ReinigungService.class);
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ZimmerstatusRepository zimmerRepository;
    private final ReinigungslisteRepository reinigungslisteRepository;
    private final JavaMailSender mailSender;
    private final String absender;
    private final String empfaenger;

    public ReinigungService(ZimmerstatusRepository zimmerRepository,
                            ReinigungslisteRepository reinigungslisteRepository,
                            JavaMailSender mailSender,
                            @Value("${hotel.mail-absender}") String absender,
                            @Value("${hotel.reinigung.empfaenger}") String empfaenger) {
        this.zimmerRepository = zimmerRepository;
        this.reinigungslisteRepository = reinigungslisteRepository;
        this.mailSender = mailSender;
        this.absender = absender;
        this.empfaenger = empfaenger;
    }

    // ------------------------------------------------------------------
    // Zeitgeber
    // ------------------------------------------------------------------

    /** Auslöser von UC25: täglich um 11 Uhr. */
    @Scheduled(cron = "0 0 11 * * *", zone = "Europe/Berlin")
    public void starteTaeglicheReinigung() {
        listeErstellenUndVersenden(LocalDate.now());
    }

    /** Nach der Reinigungszeit, täglich um 12 Uhr. */
    @Scheduled(cron = "0 0 12 * * *", zone = "Europe/Berlin")
    public void schliesseTaeglicheReinigungAb() {
        statusAktualisieren(LocalDate.now(), LocalDateTime.now());
    }

    // ------------------------------------------------------------------
    // Ablauf
    // ------------------------------------------------------------------

    /**
     * Schritte 1 und 2: Reinigungsliste erstellen und an das Reinigungsteam schicken.
     * Gibt es für den Tag schon eine Liste, bleibt es bei dieser (kein zweiter Versand).
     */
    @Transactional
    public Reinigungsergebnis listeErstellenUndVersenden(LocalDate datum) {
        Optional<Reinigungsliste> vorhanden = reinigungslisteRepository.findByDatum(datum);
        if (vorhanden.isPresent()) {
            log.info("Reinigungsliste für {} gibt es schon.", datum);
            return Reinigungsergebnis.von(vorhanden.get());
        }

        // Bleibegäste: Ein belegtes Zimmer, das gestern gereinigt wurde, ist heute wieder dran,
        // außer der Gast hat "nicht reinigen" gewählt (UC07: Standard ist "zu reinigen").
        List<Zimmer> bleibegaeste = zimmerRepository.findByZimmerstatusAndReinigungsstatus(
                Zimmerstatus.BELEGT, Reinigungsstatus.GEREINIGT);
        bleibegaeste.forEach(z -> z.reinigungsstatusAendern(Reinigungsstatus.ZU_REINIGEN));

        List<Zimmer> zuReinigen = new ArrayList<>(bleibegaeste);
        zimmerRepository.findByReinigungsstatusAndZimmerstatusNotOrderByEtageAscZimmerNrAsc(
                        Reinigungsstatus.ZU_REINIGEN, Zimmerstatus.GESPERRT).stream()
                .filter(z -> !zuReinigen.contains(z))
                .forEach(zuReinigen::add);
        zuReinigen.sort(Comparator.comparingInt(Zimmer::getEtage).thenComparingInt(Zimmer::getZimmerNr));

        Reinigungsliste liste = new Reinigungsliste(datum);
        zuReinigen.forEach(liste::zimmerHinzufuegen);
        zimmerRepository.saveAll(bleibegaeste);

        if (versenden(liste)) {
            liste.alsVersendetMarkieren();
        }
        reinigungslisteRepository.save(liste);
        log.info("Reinigungsliste für {} mit {} Zimmern erstellt.", datum, liste.getZimmer().size());
        return Reinigungsergebnis.von(liste);
    }

    /**
     * Schritte 4 bis 6: Zimmerstatus nach der Reinigung anpassen und das Ergebnis festhalten.
     *
     * @return das Ergebnis, leer wenn es für den Tag keine Liste gibt (dann wurde auch nichts gereinigt)
     */
    @Transactional
    public Optional<Reinigungsergebnis> statusAktualisieren(LocalDate datum, LocalDateTime jetzt) {
        Optional<Reinigungsliste> gefunden = reinigungslisteRepository.findByDatum(datum);
        if (gefunden.isEmpty()) {
            log.warn("Keine Reinigungsliste für {}: Status bleiben unverändert.", datum);
            return Optional.empty();
        }
        Reinigungsliste liste = gefunden.get();
        if (liste.istAbgeschlossen()) {
            return Optional.of(Reinigungsergebnis.von(liste));
        }

        // Schritt 4: erst die Zimmer der Liste auf "gereinigt"
        liste.statusAktualisieren();

        // Schritt 5: danach "nicht reinigen" auf "zu reinigen"
        List<Zimmer> nichtReinigen = zimmerRepository.findByReinigungsstatusAndZimmerstatusNotOrderByEtageAscZimmerNrAsc(
                Reinigungsstatus.NICHT_REINIGEN, Zimmerstatus.GESPERRT);
        nichtReinigen.forEach(z -> z.reinigungsstatusAendern(Reinigungsstatus.ZU_REINIGEN));

        zimmerRepository.saveAll(liste.getZimmer());
        zimmerRepository.saveAll(nichtReinigen);

        // Schritt 6: Ergebnis für die Meldung an den Mitarbeiter
        liste.abschliessen(nichtReinigen.size(), jetzt);
        reinigungslisteRepository.save(liste);
        log.info("Reinigung {} abgeschlossen: {} gereinigt, {} wieder zu reinigen.",
                datum, liste.getAnzahlGereinigt(), liste.getAnzahlNeuZuReinigen());
        return Optional.of(Reinigungsergebnis.von(liste));
    }

    /**
     * Führt den ganzen Ablauf sofort aus, ohne auf 11 und 12 Uhr zu warten.
     * Zum Ausprobieren und Vorführen über die Zimmerstatus-Ansicht.
     *
     * @throws ZimmerstatusException wenn die Reinigung für heute schon abgeschlossen ist
     */
    @Transactional
    public Reinigungsergebnis jetztAusfuehren() {
        LocalDate heute = LocalDate.now();
        Optional<Reinigungsliste> vorhanden = reinigungslisteRepository.findByDatum(heute);
        if (vorhanden.isPresent() && vorhanden.get().istAbgeschlossen()) {
            throw new ZimmerstatusException("Die Reinigung für heute ist schon abgeschlossen.");
        }
        listeErstellenUndVersenden(heute);
        return statusAktualisieren(heute, LocalDateTime.now()).orElseThrow();
    }

    /** Die neueste Reinigung für den Hinweis an den Mitarbeiter (Schritt 6). */
    @Transactional(readOnly = true)
    public Optional<Reinigungsergebnis> ladeLetzteReinigung() {
        return reinigungslisteRepository.findFirstByOrderByDatumDesc().map(Reinigungsergebnis::von);
    }

    // ------------------------------------------------------------------
    // E-Mail an das Reinigungsteam
    // ------------------------------------------------------------------

    /** Schickt die Liste. Ein Fehler beim Versand bricht den Ablauf nicht ab, er wird nur gemeldet. */
    private boolean versenden(Reinigungsliste liste) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(absender);
        mail.setTo(empfaenger);
        mail.setSubject("Reinigungsliste " + liste.getDatum().format(DATUM));
        mail.setText(mailtext(liste));
        try {
            mailSender.send(mail);
            return true;
        } catch (MailException e) {
            log.error("Reinigungsliste für {} konnte nicht verschickt werden.", liste.getDatum(), e);
            return false;
        }
    }

    static String mailtext(Reinigungsliste liste) {
        StringBuilder text = new StringBuilder("Hallo Reinigungsteam,\n\n");
        if (liste.getZimmer().isEmpty()) {
            text.append("heute (").append(liste.getDatum().format(DATUM))
                    .append(") sind keine Zimmer zu reinigen.\n");
        } else {
            text.append("bitte reinigt heute (").append(liste.getDatum().format(DATUM))
                    .append(") bis 11:55 Uhr diese ").append(liste.getZimmer().size()).append(" Zimmer:\n\n");
            int etage = Integer.MIN_VALUE;
            for (Zimmer z : liste.getZimmer()) {
                if (z.getEtage() != etage) {
                    etage = z.getEtage();
                    text.append(etage).append(". Etage\n");
                }
                text.append("  - Zimmer ").append(z.getZimmerNr());
                if (z.istBelegt()) {
                    text.append(" (belegt)");
                }
                text.append('\n');
            }
        }
        text.append("\nViele Grüße\nHotel Sparrenhof");
        return text.toString();
    }

    // ------------------------------------------------------------------

    /** Lesesicht auf eine Reinigungsliste für die Oberfläche. */
    public record Reinigungsergebnis(
            LocalDate datum,
            int anzahlAufListe,
            boolean versendet,
            LocalDateTime abgeschlossenUm,
            int anzahlGereinigt,
            int anzahlNeuZuReinigen) {

        static Reinigungsergebnis von(Reinigungsliste liste) {
            return new Reinigungsergebnis(liste.getDatum(), liste.getZimmer().size(), liste.isVersendet(),
                    liste.getAbgeschlossenUm(), liste.getAnzahlGereinigt(), liste.getAnzahlNeuZuReinigen());
        }

        public boolean abgeschlossen() {
            return abgeschlossenUm != null;
        }
    }
}
