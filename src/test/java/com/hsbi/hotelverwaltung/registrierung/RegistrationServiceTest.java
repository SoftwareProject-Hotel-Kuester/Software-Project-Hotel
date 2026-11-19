package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.mail.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// Tests für RegistrationService. Docker Desktop muss laufen (echte Datenbank).
// @MockitoBean: Der MailService wird durch eine Attrappe ersetzt, es wird nichts verschickt.
// Die E-Mails enden auf "@reg-test.de", die gibt es in den Testdaten nicht.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class RegistrationServiceTest {

    private static final LocalDate GEBURTSDATUM = LocalDate.of(1992, 3, 4);

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private BenutzerRepository benutzerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private MailService mailService;

    private RegistrierungsDaten daten(String email) {
        return new RegistrierungsDaten("Rita", "Registriert", GEBURTSDATUM, email);
    }

    @Test
    void neueAdresseLegtProfilOhnePasswortAnUndSchicktLink() {
        registrationService.registrieren(daten(" Rita@Reg-Test.de "));

        Gast gast = (Gast) benutzerRepository.findByEmailIgnoreCase("rita@reg-test.de").orElseThrow();
        assertNull(gast.getPasswort());
        assertFalse(gast.isAktiv());
        assertFalse(gast.isHatKonto());
        verify(mailService).sendeBestaetigungsLink(eq("rita@reg-test.de"), eq("Rita"),
                contains("/passwort-setzen?token="));
    }

    @Test
    void profilOhneKontoWirdNichtUeberschriebenSondernBekommtLink() {
        benutzerRepository.save(new Gast("profil@reg-test.de", "Paul", "Profil", LocalDate.of(1980, 1, 1)));

        registrationService.registrieren(new RegistrierungsDaten("Falscher", "Name", GEBURTSDATUM, "profil@reg-test.de"));

        assertEquals("Paul", benutzerRepository.findByEmailIgnoreCase("profil@reg-test.de").orElseThrow().getVorname());
        verify(mailService).sendeBestaetigungsLink(eq("profil@reg-test.de"), eq("Paul"),
                contains("/passwort-setzen?token="));
    }

    @Test
    void vorhandenesKontoBekommtNurEinenHinweisUndBleibtGesperrt() {
        Gast konto = new Gast("konto@reg-test.de", "Karl", "Konto", GEBURTSDATUM);
        konto.setPasswort(passwordEncoder.encode("Altes1234!"));
        konto.setHatKonto(true);
        konto.setAktiv(false);   // gesperrt
        benutzerRepository.save(konto);

        registrationService.registrieren(daten("konto@reg-test.de"));

        verify(mailService).sendeKontoBereitsVorhanden(eq("konto@reg-test.de"), eq("Karl"), contains("/login"));
        verify(mailService, never()).sendeBestaetigungsLink(any(), any(), any());
        assertFalse(benutzerRepository.findByEmailIgnoreCase("konto@reg-test.de").orElseThrow().isAktiv());
    }

    @Test
    void ueberDenLinkWirdDasKontoFreigeschaltet() {
        registrationService.registrieren(daten("flow@reg-test.de"));
        String geheimnis = geheimnisAusMail("flow@reg-test.de");

        assertTrue(registrationService.passwortSetzen(geheimnis, "Sicher1234!"));

        Gast gast = (Gast) benutzerRepository.findByEmailIgnoreCase("flow@reg-test.de").orElseThrow();
        assertTrue(gast.isAktiv());
        assertTrue(gast.isHatKonto());
        assertTrue(passwordEncoder.matches("Sicher1234!", gast.getPasswort()));
        // Derselbe Link funktioniert kein zweites Mal.
        assertFalse(registrationService.passwortSetzen(geheimnis, "Anderes1234!"));
    }

    @Test
    void zuKurzesPasswortLaesstDenLinkGueltig() {
        registrationService.registrieren(daten("kurz@reg-test.de"));
        String geheimnis = geheimnisAusMail("kurz@reg-test.de");

        assertThrows(IllegalArgumentException.class, () -> registrationService.passwortSetzen(geheimnis, "kurz"));

        assertTrue(registrationService.passwortSetzen(geheimnis, "Sicher1234!"));
    }

    @Test
    void ungueltigeEingabenWerdenAbgelehnt() {
        assertThrows(IllegalArgumentException.class, () -> registrationService.registrieren(
                new RegistrierungsDaten(" ", "Registriert", GEBURTSDATUM, "x@reg-test.de")));
        assertThrows(IllegalArgumentException.class, () -> registrationService.registrieren(
                new RegistrierungsDaten("Rita", "Registriert", GEBURTSDATUM, "keine-email")));
        assertThrows(IllegalArgumentException.class, () -> registrationService.registrieren(
                new RegistrierungsDaten("Rita", "Registriert", LocalDate.now().plusDays(1), "x@reg-test.de")));
    }

    // Holt das Geheimnis aus dem Link, der an die Attrappe des MailService übergeben wurde.
    private String geheimnisAusMail(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendeBestaetigungsLink(eq(email), any(), link.capture());
        return link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
    }
}