package com.hsbi.hotelverwaltung.verfuegbarkeit;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für die Verfügbarkeitsprüfung (UC04).
 * Die Repositories werden nachgebaut (Mockito), deshalb braucht der Test keine Datenbank.
 */
class VerfuegbarkeitServiceTest {

    private static final LocalDate ANREISE = LocalDate.of(2026, 6, 15);
    private static final LocalDate ABREISE = LocalDate.of(2026, 6, 18);

    private ZimmerRepository zimmerRepository;
    private BuchungRepository buchungRepository;
    private VerfuegbarkeitService service;

    private Zimmer doppelStandard;
    private Zimmer einzelStandard;

    @BeforeEach
    void setUp() {
        zimmerRepository = mock(ZimmerRepository.class);
        buchungRepository = mock(BuchungRepository.class);
        service = new VerfuegbarkeitService(zimmerRepository, buchungRepository);

        doppelStandard = new Zimmer(304, 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        einzelStandard = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        when(zimmerRepository.findAll()).thenReturn(List.of(doppelStandard, einzelStandard));
    }

    /** Hilfsmethode: Für dieses Zimmer gibt es eine überschneidende Buchung. */
    private void zimmerIstGebucht(Zimmer zimmer) {
        when(buchungRepository.existsByZimmerAndStatusInAndAnreiseBeforeAndAbreiseAfter(
                eq(zimmer), any(), eq(ABREISE), eq(ANREISE))).thenReturn(true);
    }

    @Test
    void ohneBuchungenSindAlleZimmerFreiUndSortiert() {
        List<Zimmer> frei = service.findeFreieZimmer(ANREISE, ABREISE, null, null);

        assertEquals(2, frei.size());
        assertEquals(101, frei.get(0).getZimmerNr());
        assertEquals(304, frei.get(1).getZimmerNr());
    }

    @Test
    void gebuchtesZimmerIstNichtFrei() {
        zimmerIstGebucht(doppelStandard);

        List<Zimmer> frei = service.findeFreieZimmer(ANREISE, ABREISE, null, null);

        assertEquals(1, frei.size());
        assertEquals(101, frei.get(0).getZimmerNr());
    }

    @Test
    void gesperrtesZimmerIstNichtFrei() {
        einzelStandard.sperren("Fernseher ist defekt");

        List<Zimmer> frei = service.findeFreieZimmer(ANREISE, ABREISE, null, null);

        assertEquals(1, frei.size());
        assertEquals(304, frei.get(0).getZimmerNr());
    }

    @Test
    void filterNachBelegungsart() {
        List<Zimmer> frei = service.findeFreieZimmer(ANREISE, ABREISE, Belegungsart.DOPPEL, null);

        assertEquals(1, frei.size());
        assertEquals(304, frei.get(0).getZimmerNr());
    }

    @Test
    void filterNachZimmerklasse() {
        List<Zimmer> frei = service.findeFreieZimmer(ANREISE, ABREISE, null, Zimmerklasse.PREMIUM);

        assertTrue(frei.isEmpty());
    }

    @Test
    void istFreiGibtFalseBeiUeberschneidungUndTrueOhne() {
        assertTrue(service.istFrei(doppelStandard, ANREISE, ABREISE));

        zimmerIstGebucht(doppelStandard);
        assertFalse(service.istFrei(doppelStandard, ANREISE, ABREISE));
    }

    @Test
    void abreiseVorAnreiseWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findeFreieZimmer(ABREISE, ANREISE, null, null));
    }

    @Test
    void gleichesDatumFuerAnreiseUndAbreiseWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findeFreieZimmer(ANREISE, ANREISE, null, null));
    }

    @Test
    void fehlendesDatumWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findeFreieZimmer(null, ABREISE, null, null));
    }
}