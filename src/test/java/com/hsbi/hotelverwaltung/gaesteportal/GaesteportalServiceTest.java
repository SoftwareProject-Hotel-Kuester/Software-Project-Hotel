package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für UC18. Die Datenbank wird durch ein Mockito-Mock ersetzt,
 * deshalb laufen die Tests ohne Docker.
 */
class GaesteportalServiceTest {

    /** "Heute" ist in allen Tests der 01.06.2027. */
    private static final LocalDate HEUTE = LocalDate.of(2027, 6, 1);

    private BuchungRepository buchungRepository;
    private GaesteportalService service;
    private Gast max;
    private Gast erika;
    private Zimmer zimmer;

    @BeforeEach
    void setUp() {
        buchungRepository = mock(BuchungRepository.class);
        Clock fest = Clock.fixed(HEUTE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new GaesteportalService(buchungRepository, fest);

        max = gast(1L, "max@example.com");
        erika = gast(2L, "erika@example.com");
        zimmer = new Zimmer("304", 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
    }

    @Test
    void anstehendeBuchungenStehenVorVergangenen() {
        Buchung vergangenAlt = buchung(1L, max, HEUTE.minusDays(60), HEUTE.minusDays(58));
        Buchung vergangenNeu = buchung(2L, max, HEUTE.minusDays(10), HEUTE.minusDays(8));
        Buchung anstehendSpaet = buchung(3L, max, HEUTE.plusDays(30), HEUTE.plusDays(32));
        Buchung anstehendBald = buchung(4L, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findByGast(max))
                .thenReturn(List.of(vergangenAlt, anstehendSpaet, vergangenNeu, anstehendBald));

        List<Buchung> ergebnis = service.eigeneBuchungen(max);

        assertEquals(List.of(anstehendBald, anstehendSpaet, vergangenNeu, vergangenAlt), ergebnis);
    }

    @Test
    void laufenderAufenthaltZaehltAlsAnstehend() {
        Buchung laufend = buchung(1L, max, HEUTE.minusDays(2), HEUTE.plusDays(1));
        Buchung vergangen = buchung(2L, max, HEUTE.minusDays(10), HEUTE.minusDays(8));
        when(buchungRepository.findByGast(max)).thenReturn(List.of(vergangen, laufend));

        assertEquals(List.of(laufend, vergangen), service.eigeneBuchungen(max));
    }

    @Test
    void ohneBuchungenIstDieListeLeer() {
        when(buchungRepository.findByGast(max)).thenReturn(List.of());

        assertTrue(service.eigeneBuchungen(max).isEmpty());
    }

    @Test
    void eigeneBuchungWirdGeliefert() {
        Buchung buchung = buchung(7L, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7L)).thenReturn(Optional.of(buchung));

        assertSame(buchung, service.eigeneBuchung(max, 7L));
    }

    @Test
    void fremdeBuchungWirdNichtGeliefert() {
        Buchung vonErika = buchung(7L, erika, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7L)).thenReturn(Optional.of(vonErika));

        assertThrows(BuchungNichtGefundenException.class, () -> service.eigeneBuchung(max, 7L));
    }

    @Test
    void unbekannteBuchungWirdNichtGeliefert() {
        when(buchungRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BuchungNichtGefundenException.class, () -> service.eigeneBuchung(max, 99L));
    }

    @Test
    void nurAnstehendeGebuchteBuchungenSindAenderbar() {
        Buchung anstehend = buchung(1L, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        Buchung anreiseHeute = buchung(2L, max, HEUTE, HEUTE.plusDays(2));
        Buchung storniert = buchung(3L, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        storniert.setStatus(Buchungsstatus.STORNIERT);

        assertTrue(anstehend.kannVomGastGeaendertWerden(HEUTE));
        assertFalse(anreiseHeute.kannVomGastGeaendertWerden(HEUTE));
        assertFalse(storniert.kannVomGastGeaendertWerden(HEUTE));
    }

    private static Gast gast(Long gastNr, String email) {
        Gast gast = new Gast(email, "Test", "Gast", LocalDate.of(1990, 1, 1));
        // Die Gastnummer (= Benutzer-ID) vergibt sonst die Datenbank.
        ReflectionTestUtils.setField(gast, "benutzerId", gastNr);
        return gast;
    }

    private Buchung buchung(Long nr, Gast gast, LocalDate anreise, LocalDate abreise) {
        Buchung buchung = new Buchung(gast, zimmer, anreise, abreise, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(buchung, "buchungsNr", nr);
        return buchung;
    }
}
