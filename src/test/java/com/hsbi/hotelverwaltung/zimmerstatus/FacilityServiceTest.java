package com.hsbi.hotelverwaltung.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import com.hsbi.hotelverwaltung.zimmerstatus.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC24, UC26 und UC27 im FacilityService. Die Repositories sind Attrappen
 * (Mockito), es braucht also keine Datenbank.
 */
class FacilityServiceTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 11, 16);

    private ZimmerstatusRepository zimmerRepository;
    private DefektmeldungRepository defektmeldungRepository;
    private FacilityService service;
    private Zimmer zimmer;

    @BeforeEach
    void setUp() {
        zimmerRepository = mock(ZimmerstatusRepository.class);
        defektmeldungRepository = mock(DefektmeldungRepository.class);
        service = new FacilityService(zimmerRepository, defektmeldungRepository);

        zimmer = new Zimmer(304, 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        when(zimmerRepository.findById(304)).thenReturn(Optional.of(zimmer));
        when(defektmeldungRepository.findFirstByZimmerAndErledigtFalseOrderByMeldungNrDesc(zimmer))
                .thenReturn(Optional.empty());
        when(defektmeldungRepository.save(any(Defektmeldung.class))).thenAnswer(aufruf -> aufruf.getArgument(0));
    }

    // ---------------- UC24 ----------------

    @Test
    void uebersichtFiltertUndZeigtSperrgrund() {
        Zimmer frei = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.ECONOMY);
        zimmer.sperren("Fernseher defekt");
        Defektmeldung offen = new Defektmeldung(zimmer, "Fernseher defekt", HEUTE, Defektmeldung.Quelle.GAST);
        when(zimmerRepository.findAllByOrderByEtageAscZimmerNrAsc()).thenReturn(List.of(frei, zimmer));
        when(defektmeldungRepository.findByErledigtFalse()).thenReturn(List.of(offen));

        List<ZimmerInfo> gesperrt = service.ladeAlleZimmer(Statusfilter.GESPERRT);
        List<ZimmerInfo> alle = service.ladeAlleZimmer(null);

        assertEquals(1, gesperrt.size());
        assertEquals(304, gesperrt.getFirst().zimmerNr());
        assertEquals("Fernseher defekt", gesperrt.getFirst().sperrgrund());
        assertEquals(2, alle.size());
    }

    // ---------------- UC26 ----------------

    @Test
    void sperrenOhneGrundGehtNicht() {
        ZimmerstatusException fehler = assertThrows(ZimmerstatusException.class,
                () -> service.zimmerSperren(304, "  "));

        assertEquals("Der Sperrgrund ist ein Pflichtfeld.", fehler.getMessage());
        assertFalse(zimmer.istGesperrt());
        verify(defektmeldungRepository, never()).save(any());
    }

    @Test
    void sperrenMitGrundLegtMeldungAnUndSperrtZimmer() {
        service.zimmerSperren(304, "Fernseher ist defekt");

        assertTrue(zimmer.istGesperrt());
        assertEquals("Fernseher ist defekt", zimmer.getSperrgrund());
        ArgumentCaptor<Defektmeldung> meldung = ArgumentCaptor.forClass(Defektmeldung.class);
        verify(defektmeldungRepository).save(meldung.capture());
        assertEquals("Fernseher ist defekt", meldung.getValue().getBeschreibung());
        assertEquals(Defektmeldung.Quelle.MITARBEITER, meldung.getValue().getQuelle());
    }

    @Test
    void sperrgrundAendernLaesstZimmerGesperrt() {
        zimmer.sperren("Fernseher defekt");

        service.zimmerSperren(304, "Fernseher und Fernbedienung defekt");

        assertTrue(zimmer.istGesperrt());
        assertEquals("Fernseher und Fernbedienung defekt", zimmer.getSperrgrund());
        verify(defektmeldungRepository, never()).save(any());
    }

    // ---------------- UC27 ----------------

    @Test
    void sperreAufhebenErledigtMeldungUndSetztZuReinigen() {
        zimmer.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        zimmer.sperren("Fernseher defekt");
        Defektmeldung offen = new Defektmeldung(zimmer, "Fernseher defekt", HEUTE, Defektmeldung.Quelle.GAST);
        when(defektmeldungRepository.findByZimmerAndErledigtFalse(zimmer)).thenReturn(List.of(offen));

        service.zimmerFreigeben(304, "Fernseher ausgetauscht");

        assertEquals(Zimmerstatus.NICHT_BELEGT, zimmer.getZimmerstatus());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zimmer.getReinigungsstatus());
        assertTrue(offen.isErledigt());
        assertEquals("Fernseher ausgetauscht", offen.getVermerkBehebung());
        assertEquals(null, zimmer.getSperrgrund());
    }

    @Test
    void nichtGesperrtesZimmerKannNichtEntsperrtWerden() {
        assertThrows(ZimmerstatusException.class, () -> service.zimmerFreigeben(304, null));
    }

    @Test
    void unbekanntesZimmerGibtVerstaendlicheMeldung() {
        when(zimmerRepository.findById(anyInt())).thenReturn(Optional.empty());

        ZimmerstatusException fehler = assertThrows(ZimmerstatusException.class,
                () -> service.zimmerSperren(999, "Test"));

        assertEquals("Zimmer 999 gibt es nicht.", fehler.getMessage());
    }
}
