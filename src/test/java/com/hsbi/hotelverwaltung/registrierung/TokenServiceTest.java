package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.domain.BestaetigungsToken;
import com.hsbi.hotelverwaltung.domain.BestaetigungsTokenRepository;
import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.domain.TokenZweck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Tests für TokenService (Bestätigungslinks der Registrierung).
// @SpringBootTest startet die App mit echter Datenbank, Docker Desktop muss laufen.
// @Transactional: Jeder Test wird danach zurückgerollt, die Datenbank bleibt sauber.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private BestaetigungsTokenRepository tokenRepository;
    @Autowired
    private BenutzerRepository benutzerRepository;

    private Benutzer gast;

    // Vor jedem Test ein eigener Gast, den es in den Testdaten nicht gibt.
    @BeforeEach
    void gastAnlegen() {
        gast = benutzerRepository.save(new Gast("token-test@uc-test.de", "Tina",
                "Tokentest", LocalDate.of(1990, 1, 1)));
    }

    @Test
    void inDerDatenbankStehtNurDerHash() {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.KONTO_BESTAETIGEN);

        assertTrue(tokenRepository.findByTokenHash(geheimnis).isEmpty());
        BestaetigungsToken gespeichert =
                tokenRepository.findByTokenHash(TokenService.hash(geheimnis)).orElseThrow();
        assertNotEquals(geheimnis, gespeichert.getTokenHash());
        assertEquals(64, gespeichert.getTokenHash().length());
    }

    @Test
    void gueltigerTokenLiefertDenBenutzer() {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.KONTO_BESTAETIGEN);

        Optional<Benutzer> ergebnis = tokenService.einloesen(geheimnis, TokenZweck.KONTO_BESTAETIGEN);

        assertEquals(gast.getBenutzerId(), ergebnis.orElseThrow().getBenutzerId());
    }

    @Test
    void einTokenGiltNurEinmal() {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.KONTO_BESTAETIGEN);
        tokenService.einloesen(geheimnis, TokenZweck.KONTO_BESTAETIGEN);

        assertTrue(tokenService.einloesen(geheimnis, TokenZweck.KONTO_BESTAETIGEN).isEmpty());
    }

    @Test
    void falscherZweckWirdAbgelehnt() {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.KONTO_BESTAETIGEN);

        assertTrue(tokenService.einloesen(geheimnis, TokenZweck.PASSWORT_SETZEN).isEmpty());
    }

    @Test
    void unbekanntesGeheimnisWirdAbgelehnt() {
        assertTrue(tokenService.einloesen("gibt-es-nicht", TokenZweck.KONTO_BESTAETIGEN).isEmpty());
        assertTrue(tokenService.einloesen(null, TokenZweck.KONTO_BESTAETIGEN).isEmpty());
    }

    @Test
    void abgelaufenerTokenWirdAbgelehnt() {
        // Token mit bekanntem Geheimnis, der schon gestern abgelaufen ist.
        String geheimnis = "abgelaufenes-geheimnis";
        tokenRepository.save(new BestaetigungsToken(gast, TokenZweck.KONTO_BESTAETIGEN,
                TokenService.hash(geheimnis), Instant.now().minusSeconds(86400)));

        assertTrue(tokenService.einloesen(geheimnis, TokenZweck.KONTO_BESTAETIGEN).isEmpty());
    }

    @Test
    void pruefenVerbrauchtDenTokenNicht() {
        String geheimnis = tokenService.erzeugen(gast, TokenZweck.PASSWORT_SETZEN);

        assertTrue(tokenService.istGueltig(geheimnis, TokenZweck.PASSWORT_SETZEN));
        assertTrue(tokenService.istGueltig(geheimnis, TokenZweck.PASSWORT_SETZEN));
        assertFalse(tokenService.istGueltig(geheimnis, TokenZweck.KONTO_BESTAETIGEN));
        // Danach lässt er sich immer noch einlösen.
        assertTrue(tokenService.einloesen(geheimnis, TokenZweck.PASSWORT_SETZEN).isPresent());
    }
}