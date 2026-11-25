package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC07 PflegeZimmerStatus aus Sicht des Gastes. Repositories sind Attrappen.
 */
class GastZimmerServiceTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 11, 25);
    private static final LocalDateTime NACHMITTAG = HEUTE.atTime(15, 0);

    private BuchungRepository buchungRepository;
    private ZimmerstatusRepository zimmerRepository;
    private DefektmeldungRepository defektmeldungRepository;
    private GastZimmerService service;

    private Gast gina;
    private Gast erika;
    private Zimmer zimmer;
    private Buchung laufend;

    @BeforeEach
    void setUp() {
        buchungRepository = mock(BuchungRepository.class);
        zimmerRepository = mock(ZimmerstatusRepository.class);
        defektmeldungRepository = mock(DefektmeldungRepository.class);
        service = new GastZimmerService(buchungRepository, zimmerRepository, defektmeldungRepository);

        gina = gast(1, "gast@sparrenhof.test");
        erika = gast(2, "erika@example.com");

        zimmer = new Zimmer(202, 2, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        zimmer.einchecken();

        laufend = buchung(10, gina, zimmer, HEUTE.minusDays(1), HEUTE.plusDays(3), Buchungsstatus.EINGECHECKT);
        when(buchungRepository.findById(10)).thenReturn(Optional.of(laufend));
    }

    // ---------------- Laufende Aufenthalte ----------------

    @Test
    void nurLaufendeAufenthalteWerdenAngezeigt() {
        Zimmer anderes = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);
        Buchung kuenftig = buchung(11, gina, anderes, HEUTE.plusDays(5), HEUTE.plusDays(7), Buchungsstatus.GEBUCHT);
        Buchung storniert = buchung(12, gina, anderes, HEUTE.minusDays(1), HEUTE.plusDays(1), Buchungsstatus.STORNIERT);
        Buchung abreiseHeute = buchung(13, gina, anderes, HEUTE.minusDays(3), HEUTE, Buchungsstatus.EINGECHECKT);
        when(buchungRepository.findByGast(gina)).thenReturn(List.of(kuenftig, storniert, abreiseHeute, laufend));

        List<GastZimmerService.Aufenthalt> aufenthalte = service.laufendeAufenthalte(gina, HEUTE);

        assertEquals(1, aufenthalte.size());
        assertEquals(202, aufenthalte.get(0).zimmerNr());
    }

    // ---------------- Standard und E1 ----------------

    @Test
    void gastWaehltNichtReinigen() {
        GastZimmerService.Aufenthalt ergebnis = service.reinigungWaehlen(gina, 10, true, NACHMITTAG);

        assertEquals(Reinigungsstatus.NICHT_REINIGEN, zimmer.getReinigungsstatus());
        assertTrue(ergebnis.nichtReinigen());
        verify(zimmerRepository).save(zimmer);
    }

    @Test
    void gastNimmtNichtReinigenZurueck() {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.NICHT_REINIGEN);

        service.reinigungWaehlen(gina, 10, false, HEUTE.atTime(9, 30));

        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
    }

    @Test
    void bitteReinigenLaesstGereinigtesZimmerGereinigt() {
        service.reinigungWaehlen(gina, 10, false, NACHMITTAG);

        assertEquals(Reinigungsstatus.GEREINIGT, zimmer.getReinigungsstatus());
    }

    @Test
    void zwischenElfUndZwoelfIstKeineAenderungMoeglich() {
        assertThrows(ZimmerstatusException.class,
                () -> service.reinigungWaehlen(gina, 10, true, HEUTE.atTime(11, 30)));
        assertEquals(Reinigungsstatus.GEREINIGT, zimmer.getReinigungsstatus());
    }

    @Test
    void reinigungszeitGrenzen() {
        assertFalse(GastZimmerService.istReinigungszeit(LocalTime.of(10, 59)));
        assertTrue(GastZimmerService.istReinigungszeit(LocalTime.of(11, 0)));
        assertTrue(GastZimmerService.istReinigungszeit(LocalTime.of(11, 59)));
        assertFalse(GastZimmerService.istReinigungszeit(LocalTime.of(12, 0)));
    }

    @Test
    void gesperrtesZimmerKannNichtGewaehltWerden() {
        zimmer.sperren("Wasserschaden");

        assertThrows(ZimmerstatusException.class, () -> service.reinigungWaehlen(gina, 10, true, NACHMITTAG));
    }

    @Test
    void fremdeBuchungWirdAbgelehnt() {
        assertThrows(ZimmerstatusException.class, () -> service.reinigungWaehlen(erika, 10, true, NACHMITTAG));
        verify(zimmerRepository, never()).save(any());
    }

    @Test
    void beendeterAufenthaltWirdAbgelehnt() {
        assertThrows(ZimmerstatusException.class,
                () -> service.reinigungWaehlen(gina, 10, true, HEUTE.plusDays(3).atTime(9, 0)));
    }

    // ---------------- E2 ----------------

    @Test
    void defektMeldenSperrtDasZimmerMitVermerkDesGastes() {
        service.defektMelden(gina, 10, "  Dusche wird nicht warm  ", HEUTE);

        assertTrue(zimmer.istGesperrt());
        assertEquals("Dusche wird nicht warm", zimmer.getSperrgrund());

        ArgumentCaptor<Defektmeldung> meldung = ArgumentCaptor.forClass(Defektmeldung.class);
        verify(defektmeldungRepository).save(meldung.capture());
        assertEquals(Defektmeldung.Quelle.GAST, meldung.getValue().getQuelle());
        assertEquals(HEUTE, meldung.getValue().getGemeldetAm());
    }

    @Test
    void weitererDefektWirdAnDenSperrgrundAngehaengt() {
        zimmer.sperren("Fernseher defekt");

        service.defektMelden(gina, 10, "Fenster klemmt", HEUTE);

        assertEquals("Fernseher defekt; Gast: Fenster klemmt", zimmer.getSperrgrund());
    }

    @Test
    void defektOhneBeschreibungWirdAbgelehnt() {
        assertThrows(ZimmerstatusException.class, () -> service.defektMelden(gina, 10, "   ", HEUTE));
        assertThrows(ZimmerstatusException.class,
                () -> service.defektMelden(gina, 10, "x".repeat(Defektmeldung.MAX_LAENGE + 1), HEUTE));
        assertFalse(zimmer.istGesperrt());
        verify(defektmeldungRepository, never()).save(any());
    }

    @Test
    void defektFuerFremdeBuchungWirdAbgelehnt() {
        assertThrows(ZimmerstatusException.class, () -> service.defektMelden(erika, 10, "Dusche kalt", HEUTE));
        assertFalse(zimmer.istGesperrt());
    }

    // ---------------- Hilfen ----------------

    private static Gast gast(int id, String email) {
        Gast gast = new Gast(email, "Vor", "Nach", LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(gast, "benutzerId", id);
        return gast;
    }

    private static Buchung buchung(int nr, Gast gast, Zimmer zimmer, LocalDate anreise, LocalDate abreise,
                                   Buchungsstatus status) {
        Buchung buchung = new Buchung(gast, zimmer, anreise, abreise, new BigDecimal("100.00"));
        buchung.setStatus(status);
        ReflectionTestUtils.setField(buchung, "buchungsNr", nr);
        return buchung;
    }
}
