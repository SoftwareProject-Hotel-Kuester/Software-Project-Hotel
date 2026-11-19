package com.hsbi.hotelverwaltung.belegung;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.ZimmerRepository;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit-Tests für den Belegungskalender (UC06). Repositories sind per Mockito nachgebaut. */
class BelegungServiceTest {

    private static final LocalDate VON = LocalDate.of(2026, 6, 15);
    private static final LocalDate BIS = LocalDate.of(2026, 6, 21);

    private ZimmerRepository zimmerRepository;
    private BuchungRepository buchungRepository;
    private BelegungService service;

    private Zimmer einzel;
    private Zimmer doppel;

    @BeforeEach
    void setUp() {
        zimmerRepository = mock(ZimmerRepository.class);
        buchungRepository = mock(BuchungRepository.class);
        service = new BelegungService(zimmerRepository, buchungRepository);

        einzel = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD);
        doppel = new Zimmer(202, 2, Belegungsart.DOPPEL, Zimmerklasse.PREMIUM);
        when(zimmerRepository.findAll()).thenReturn(List.of(doppel, einzel));
        when(buchungRepository.findUeberschneidende(any(), any(), any())).thenReturn(List.of());
    }

    private Buchung buchung(Zimmer zimmer, LocalDate anreise, LocalDate abreise) {
        Gast gast = new Gast("a@b.de", "Anna", "Test", LocalDate.of(1990, 1, 1));
        return new Buchung(gast, zimmer, anreise, abreise, new BigDecimal("100.00"));
    }

    @Test
    void zeigtAlleZimmerSortiertNachNummer() {
        List<BelegungsZeile> zeilen = service.erstelleKalender(VON, BIS, null, null);
        assertEquals(List.of(101, 202), zeilen.stream().map(z -> z.zimmer().getZimmerNr()).toList());
        assertEquals(Zellenart.FREI, zeilen.get(0).art(VON));
    }

    @Test
    void buchungBelegtNaechteAberNichtDenAbreisetag() {
        Buchung b = buchung(einzel, VON.plusDays(1), VON.plusDays(3));
        when(buchungRepository.findUeberschneidende(any(), any(), any())).thenReturn(List.of(b));

        BelegungsZeile zeile = service.erstelleKalender(VON, BIS, null, null).get(0);

        assertEquals(Zellenart.FREI, zeile.art(VON));
        assertEquals(Zellenart.GEBUCHT, zeile.art(VON.plusDays(1)));
        assertEquals(Zellenart.GEBUCHT, zeile.art(VON.plusDays(2)));
        assertEquals(Zellenart.FREI, zeile.art(VON.plusDays(3)));
    }

    @Test
    void buchungAusserhalbDesZeitraumsWirdBeschnitten() {
        Buchung b = buchung(einzel, VON.minusDays(2), VON.plusDays(1));
        when(buchungRepository.findUeberschneidende(any(), any(), any())).thenReturn(List.of(b));

        BelegungsZeile zeile = service.erstelleKalender(VON, BIS, null, null).get(0);

        assertEquals(1, zeile.buchungen().size());
        assertEquals(Zellenart.GEBUCHT, zeile.art(VON));
    }

    @Test
    void gesperrtesZimmerOhneBuchungIstGesperrt() {
        einzel.sperren("Wasserschaden");
        BelegungsZeile zeile = service.erstelleKalender(VON, BIS, null, null).get(0);
        assertEquals(Zellenart.GESPERRT, zeile.art(VON));
    }

    @Test
    void filterNachBelegungsartUndKlasse() {
        assertEquals(1, service.erstelleKalender(VON, BIS, Belegungsart.DOPPEL, null).size());
        assertEquals(1, service.erstelleKalender(VON, BIS, null, Zimmerklasse.STANDARD).size());
    }

    @Test
    void keinPassendesZimmerLiefertLeereListe() {
        assertTrue(service.erstelleKalender(VON, BIS, Belegungsart.EINZEL, Zimmerklasse.PREMIUM).isEmpty());
    }

    @Test
    void fragtMitVerschobenemEndeAb() {
        service.erstelleKalender(VON, BIS, null, null);
        // bis wird um einen Tag verschoben, damit der letzte angezeigte Tag mitzählt
        verify(buchungRepository).findUeberschneidende(eq(VON), eq(BIS.plusDays(1)), any());
    }

    @Test
    void ungueltigerZeitraumWirftFehler() {
        assertThrows(IllegalArgumentException.class, () -> service.erstelleKalender(null, BIS, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.erstelleKalender(BIS, VON, null, null));
    }
}
