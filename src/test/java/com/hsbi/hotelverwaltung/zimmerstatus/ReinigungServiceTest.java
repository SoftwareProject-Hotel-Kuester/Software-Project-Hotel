package com.hsbi.hotelverwaltung.zimmerstatus;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC25 SteuereTäglicheReinigung. Repositories und Mailversand sind Attrappen,
 * es braucht also keine Datenbank und keinen Mailserver.
 */
class ReinigungServiceTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 11, 25);
    private static final LocalDateTime MITTAG = HEUTE.atTime(12, 0);

    private ZimmerstatusRepository zimmerRepository;
    private ReinigungslisteRepository listeRepository;
    private JavaMailSender mailSender;
    private ReinigungService service;

    private Zimmer zuReinigen;
    private Zimmer nichtReinigen;

    @BeforeEach
    void setUp() {
        zimmerRepository = mock(ZimmerstatusRepository.class);
        listeRepository = mock(ReinigungslisteRepository.class);
        mailSender = mock(JavaMailSender.class);
        service = new ReinigungService(zimmerRepository, listeRepository, mailSender,
                "hotel@sparrenhof.de", "reinigung@sparrenhof.de");

        zuReinigen = new Zimmer(101, 1, Belegungsart.EINZEL, Zimmerklasse.STANDARD); // neu: "zu reinigen"
        nichtReinigen = new Zimmer(201, 2, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        nichtReinigen.reinigungsstatusAendern(Reinigungsstatus.NICHT_REINIGEN);

        when(zimmerRepository.findByReinigungsstatusAndZimmerstatusNotOrderByEtageAscZimmerNrAsc(
                Reinigungsstatus.ZU_REINIGEN, Zimmerstatus.GESPERRT)).thenReturn(List.of(zuReinigen));
        when(zimmerRepository.findByReinigungsstatusAndZimmerstatusNotOrderByEtageAscZimmerNrAsc(
                Reinigungsstatus.NICHT_REINIGEN, Zimmerstatus.GESPERRT)).thenReturn(List.of(nichtReinigen));
        when(listeRepository.findByDatum(HEUTE)).thenReturn(Optional.empty());
        when(listeRepository.save(any(Reinigungsliste.class))).thenAnswer(aufruf -> aufruf.getArgument(0));
    }

    @Test
    void elfUhrErstelltListeUndSchicktSieAnDasReinigungsteam() {
        ReinigungService.Reinigungsergebnis ergebnis = service.listeErstellenUndVersenden(HEUTE);

        assertEquals(1, ergebnis.anzahlAufListe());
        assertTrue(ergebnis.versendet());
        assertFalse(ergebnis.abgeschlossen());

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertEquals("reinigung@sparrenhof.de", mail.getValue().getTo()[0]);
        assertTrue(mail.getValue().getText().contains("Zimmer 101"));
        assertFalse(mail.getValue().getText().contains("Zimmer 201"));

        // Status ändern sich erst um 12 Uhr
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zuReinigen.getReinigungsstatus());
    }

    @Test
    void bleibegastMitGereinigtemZimmerKommtWiederAufDieListe() {
        Zimmer bleibegast = new Zimmer(301, 3, Belegungsart.DOPPEL, Zimmerklasse.STANDARD);
        bleibegast.reinigungsstatusAendern(Reinigungsstatus.GEREINIGT);
        bleibegast.einchecken();
        when(zimmerRepository.findByZimmerstatusAndReinigungsstatus(Zimmerstatus.BELEGT, Reinigungsstatus.GEREINIGT))
                .thenReturn(List.of(bleibegast));

        ReinigungService.Reinigungsergebnis ergebnis = service.listeErstellenUndVersenden(HEUTE);

        assertEquals(2, ergebnis.anzahlAufListe());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, bleibegast.getReinigungsstatus());
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertTrue(mail.getValue().getText().contains("Zimmer 301 (belegt)"));
    }

    @Test
    void zweiterLaufAmSelbenTagVerschicktNichtNochmal() {
        when(listeRepository.findByDatum(HEUTE)).thenReturn(Optional.of(new Reinigungsliste(HEUTE)));

        service.listeErstellenUndVersenden(HEUTE);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(listeRepository, never()).save(any());
    }

    @Test
    void mailFehlerWirdVermerktAberListeBleibt() {
        doThrow(new MailSendException("Mailserver weg")).when(mailSender).send(any(SimpleMailMessage.class));

        ReinigungService.Reinigungsergebnis ergebnis = service.listeErstellenUndVersenden(HEUTE);

        assertFalse(ergebnis.versendet());
        verify(listeRepository).save(any(Reinigungsliste.class));
    }

    @Test
    void zwoelfUhrSetztErstGereinigtDannNichtReinigenAufZuReinigen() {
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.zimmerHinzufuegen(zuReinigen);
        when(listeRepository.findByDatum(HEUTE)).thenReturn(Optional.of(liste));

        ReinigungService.Reinigungsergebnis ergebnis = service.statusAktualisieren(HEUTE, MITTAG).orElseThrow();

        assertEquals(Reinigungsstatus.GEREINIGT, zuReinigen.getReinigungsstatus());
        // nicht auf "gereinigt" durchgerutscht, obwohl jetzt "zu reinigen"
        assertEquals(Reinigungsstatus.ZU_REINIGEN, nichtReinigen.getReinigungsstatus());
        assertTrue(ergebnis.abgeschlossen());
        assertEquals(1, ergebnis.anzahlGereinigt());
        assertEquals(1, ergebnis.anzahlNeuZuReinigen());
        assertEquals(MITTAG, ergebnis.abgeschlossenUm());
    }

    @Test
    void zimmerDasNachElfGesperrtWurdeWirdNichtGereinigt() {
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.zimmerHinzufuegen(zuReinigen);
        zuReinigen.sperren("Wasserschaden");
        when(listeRepository.findByDatum(HEUTE)).thenReturn(Optional.of(liste));

        ReinigungService.Reinigungsergebnis ergebnis = service.statusAktualisieren(HEUTE, MITTAG).orElseThrow();

        assertEquals(0, ergebnis.anzahlGereinigt());
        assertEquals(Reinigungsstatus.ZU_REINIGEN, zuReinigen.getReinigungsstatus());
    }

    @Test
    void ohneListeAendertSichUmZwoelfNichts() {
        assertTrue(service.statusAktualisieren(HEUTE, MITTAG).isEmpty());
        assertEquals(Reinigungsstatus.NICHT_REINIGEN, nichtReinigen.getReinigungsstatus());
    }

    @Test
    void abgeschlosseneListeWirdNichtZweimalAngewendet() {
        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.zimmerHinzufuegen(zuReinigen);
        when(listeRepository.findByDatum(HEUTE)).thenReturn(Optional.of(liste));
        service.statusAktualisieren(HEUTE, MITTAG);

        nichtReinigen.reinigungsstatusAendern(Reinigungsstatus.NICHT_REINIGEN); // Gast wählt neu
        service.statusAktualisieren(HEUTE, MITTAG.plusMinutes(5));

        assertEquals(Reinigungsstatus.NICHT_REINIGEN, nichtReinigen.getReinigungsstatus());
    }

    @Test
    void mailtextNachEtagenOhneZimmerHinweis() {
        Reinigungsliste leer = new Reinigungsliste(HEUTE);
        assertTrue(ReinigungService.mailtext(leer).contains("keine Zimmer zu reinigen"));

        Reinigungsliste liste = new Reinigungsliste(HEUTE);
        liste.zimmerHinzufuegen(zuReinigen);
        String text = ReinigungService.mailtext(liste);
        assertTrue(text.contains("25.11.2026"));
        assertTrue(text.contains("1. Etage"));
        assertTrue(text.contains("11:55"));
    }
}
