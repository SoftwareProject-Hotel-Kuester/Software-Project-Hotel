package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Buchung;
import com.hsbi.hotelverwaltung.domain.BuchungRepository;
import com.hsbi.hotelverwaltung.domain.Buchungsstatus;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Tests für GaesteportalService. Gehört zu UC18 ZeigeEigeneBuchungen und UC20 StorniereEigeneBuchung.
// Prüft: Sortierung der Buchungen, Zugriff nur auf eigene Buchungen, Stornieren.
//
// Diese Tests laufen ohne Datenbank und ohne Docker:
// Statt des echten BuchungRepository und MailService nutzen wir "Mocks" (Attrappen) von Mockito.
// mock(...) erzeugt die Attrappe, when(...).thenReturn(...) legt fest, was sie zurückgibt.
// verify(...) prüft danach, ob eine Methode der Attrappe aufgerufen wurde.
class GaesteportalServiceTest {

    // Alle Tests tun so, als wäre heute der 01.06.2027.
    private static final LocalDate HEUTE = LocalDate.of(2027, 6, 1);

    private BuchungRepository buchungRepository;
    private MailService mailService;
    private GaesteportalService service;
    private Gast max;
    private Gast erika;
    private Zimmer zimmer;

    // @BeforeEach: läuft vor jedem Test, damit jeder Test mit frischen Objekten startet.
    @BeforeEach
    void setUp() {
        buchungRepository = mock(BuchungRepository.class);
        mailService = mock(MailService.class);
        service = new GaesteportalService(buchungRepository, mailService);

        max = testGast(1, "max@example.com");
        erika = testGast(2, "erika@example.com");
        zimmer = new Zimmer(202, 2, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
    }

    // Anstehende Buchungen stehen oben (nächste zuerst), vergangene unten (jüngste zuerst).
    @Test
    void anstehendeBuchungenStehenVorVergangenen() {
        Buchung vergangenAlt = testBuchung(1, max, HEUTE.minusDays(60), HEUTE.minusDays(58));
        Buchung vergangenNeu = testBuchung(2, max, HEUTE.minusDays(10), HEUTE.minusDays(8));
        Buchung anstehendSpaet = testBuchung(3, max, HEUTE.plusDays(30), HEUTE.plusDays(32));
        Buchung anstehendBald = testBuchung(4, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findByGast(max))
                .thenReturn(List.of(vergangenAlt, anstehendSpaet, vergangenNeu, anstehendBald));

        List<Buchung> ergebnis = service.eigeneBuchungen(max, HEUTE);

        assertEquals(List.of(anstehendBald, anstehendSpaet, vergangenNeu, vergangenAlt), ergebnis);
    }

    // Ein Gast, der gerade im Hotel ist (Abreise noch nicht vorbei), steht bei den anstehenden.
    @Test
    void laufenderAufenthaltZaehltAlsAnstehend() {
        Buchung laufend = testBuchung(1, max, HEUTE.minusDays(2), HEUTE.plusDays(1));
        Buchung vergangen = testBuchung(2, max, HEUTE.minusDays(10), HEUTE.minusDays(8));
        when(buchungRepository.findByGast(max)).thenReturn(List.of(vergangen, laufend));

        assertEquals(List.of(laufend, vergangen), service.eigeneBuchungen(max, HEUTE));
    }

    // UC18 E1: Ohne Buchungen ist die Liste leer.
    @Test
    void ohneBuchungenIstDieListeLeer() {
        when(buchungRepository.findByGast(max)).thenReturn(List.of());

        assertTrue(service.eigeneBuchungen(max, HEUTE).isEmpty());
    }

    // Die eigene Buchung wird geliefert.
    @Test
    void eigeneBuchungWirdGeliefert() {
        Buchung buchung = testBuchung(7, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7)).thenReturn(Optional.of(buchung));

        assertSame(buchung, service.eigeneBuchung(max, 7));
    }

    // Eine fremde Buchung wird nicht geliefert, auch wenn man ihre Nummer kennt.
    @Test
    void fremdeBuchungWirdNichtGeliefert() {
        Buchung vonErika = testBuchung(7, erika, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7)).thenReturn(Optional.of(vonErika));

        assertThrows(BuchungNichtGefundenException.class, () -> service.eigeneBuchung(max, 7));
    }

    // Eine Buchungsnummer, die es nicht gibt, führt zum selben Fehler.
    @Test
    void unbekannteBuchungWirdNichtGeliefert() {
        when(buchungRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BuchungNichtGefundenException.class, () -> service.eigeneBuchung(max, 99));
    }

    // Ändern/Stornieren (UC19, UC20) nur bei anstehenden Buchungen mit Status "gebucht".
    @Test
    void nurAnstehendeGebuchteBuchungenSindAenderbar() {
        Buchung anstehend = testBuchung(1, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        Buchung anreiseHeute = testBuchung(2, max, HEUTE, HEUTE.plusDays(2));
        Buchung storniert = testBuchung(3, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        storniert.setStatus(Buchungsstatus.STORNIERT);

        assertTrue(anstehend.kannVomGastGeaendertWerden(HEUTE));
        assertFalse(anreiseHeute.kannVomGastGeaendertWerden(HEUTE));
        assertFalse(storniert.kannVomGastGeaendertWerden(HEUTE));
    }

    // UC20: Eigene, anstehende Buchung wird storniert, gespeichert und per Mail bestätigt.
    @Test
    void eigeneBuchungWirdStorniert() {
        Buchung buchung = testBuchung(7, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7)).thenReturn(Optional.of(buchung));

        service.eigeneBuchungStornieren(max, 7, HEUTE);

        assertEquals(Buchungsstatus.STORNIERT, buchung.getStatus());
        verify(buchungRepository).save(buchung);
        verify(mailService).sendeStornobestaetigung("max@example.com", "Test", 7, 202,
                "06.06.2027 bis 08.06.2027");
    }

    // UC20: Eine fremde Buchung darf nicht storniert werden. Es wird nichts gespeichert und keine Mail verschickt.
    @Test
    void fremdeBuchungKannNichtStorniertWerden() {
        Buchung vonErika = testBuchung(7, erika, HEUTE.plusDays(5), HEUTE.plusDays(7));
        when(buchungRepository.findById(7)).thenReturn(Optional.of(vonErika));

        assertThrows(BuchungNichtGefundenException.class, () -> service.eigeneBuchungStornieren(max, 7, HEUTE));
        assertEquals(Buchungsstatus.GEBUCHT, vonErika.getStatus());
        verify(buchungRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    // UC20 Vorbedingung: Ist die Anreise heute (oder schon vorbei), geht online stornieren nicht mehr.
    @Test
    void buchungMitAnreiseHeuteKannNichtStorniertWerden() {
        Buchung anreiseHeute = testBuchung(7, max, HEUTE, HEUTE.plusDays(2));
        when(buchungRepository.findById(7)).thenReturn(Optional.of(anreiseHeute));

        assertThrows(IllegalStateException.class, () -> service.eigeneBuchungStornieren(max, 7, HEUTE));
        assertEquals(Buchungsstatus.GEBUCHT, anreiseHeute.getStatus());
        verifyNoInteractions(mailService);
    }

    // UC20 E1: Eine eingecheckte Buchung kann der Gast nicht selbst stornieren.
    @Test
    void eingecheckteBuchungKannNichtStorniertWerden() {
        Buchung eingecheckt = testBuchung(7, max, HEUTE.minusDays(1), HEUTE.plusDays(2));
        eingecheckt.setStatus(Buchungsstatus.EINGECHECKT);
        when(buchungRepository.findById(7)).thenReturn(Optional.of(eingecheckt));

        assertThrows(IllegalStateException.class, () -> service.eigeneBuchungStornieren(max, 7, HEUTE));
        assertEquals(Buchungsstatus.EINGECHECKT, eingecheckt.getStatus());
    }

    // Die Methode stornieren() in Buchung: Eine schon stornierte Buchung kann man nicht noch mal stornieren.
    @Test
    void stornierteBuchungKannNichtNochmalStorniertWerden() {
        Buchung buchung = testBuchung(7, max, HEUTE.plusDays(5), HEUTE.plusDays(7));
        buchung.stornieren();

        assertEquals(Buchungsstatus.STORNIERT, buchung.getStatus());
        assertThrows(IllegalStateException.class, () -> buchung.stornieren());
    }

    // Hilfsmethode: Gast mit fester Nummer erzeugen.
    // Die Nummer vergibt sonst die Datenbank. Ohne Datenbank setzen wir sie
    // mit ReflectionTestUtils direkt in das private Feld "benutzerId".
    private static Gast testGast(Integer nummer, String email) {
        Gast gast = new Gast(email, "Test", "Gast", LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(gast, "benutzerId", nummer);
        return gast;
    }

    // Hilfsmethode: Buchung mit fester Buchungsnummer erzeugen.
    private Buchung testBuchung(Integer nummer, Gast gast, LocalDate anreise, LocalDate abreise) {
        Buchung buchung = new Buchung(gast, zimmer, anreise, abreise, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(buchung, "buchungsNr", nummer);
        return buchung;
    }
}
