package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.domain.BestaetigungsToken;
import com.hsbi.hotelverwaltung.domain.BestaetigungsTokenRepository;
import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.TokenZweck;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Erzeugt und prüft die Geheimnisse in Bestätigungslinks.
 * In der Datenbank steht nur der Hash, das Geheimnis kennt allein der Link.
 *
 * Begriffe:
 * SecureRandom: Zufallszahlen, die sich nicht vorhersagen lassen (anders als Random).
 * SHA-256: Hashverfahren. Aus dem Hash lässt sich das Geheimnis nicht zurückrechnen.
 * Transactional: Alle Datenbankschritte der Methode gelten zusammen oder gar nicht.
 */
@Service
public class TokenService {

    // 32 Byte = 256 Bit Zufall, das ist nicht zu erraten.
    private static final int GEHEIMNIS_BYTES = 32;
    private static final Duration GUELTIGKEIT = Duration.ofHours(24);

    private final BestaetigungsTokenRepository tokenRepository;
    private final SecureRandom zufall = new SecureRandom();

    public TokenService(BestaetigungsTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // Erzeugt ein Geheimnis, speichert dessen Hash und gibt das Geheimnis zurück.
    // Das Geheimnis gehört in den Link der E-Mail und darf nirgends sonst stehen.
    @Transactional
    public String erzeugen(Benutzer benutzer, TokenZweck zweck) {
        byte[] zufallsBytes = new byte[GEHEIMNIS_BYTES];
        zufall.nextBytes(zufallsBytes);
        // URL-tauglich kodiert: nur Buchstaben, Ziffern, "-" und "_", ohne "=" am Ende.
        String geheimnis = Base64.getUrlEncoder().withoutPadding().encodeToString(zufallsBytes);

        tokenRepository.save(new BestaetigungsToken(
                benutzer, zweck, hash(geheimnis), Instant.now().plus(GUELTIGKEIT)));
        return geheimnis;
    }

    // Prüft nur, ob ein Link noch gültig wäre, und verbraucht ihn nicht.
    // Die Seite zum Passwort-Festlegen braucht das, um zu entscheiden, ob sie das
    // Formular oder eine Fehlermeldung zeigt.
    @Transactional(readOnly = true)
    public boolean istGueltig(String geheimnis, TokenZweck zweck) {
        if (geheimnis == null || geheimnis.isBlank()) {
            return false;
        }
        Instant jetzt = Instant.now();
        return tokenRepository.findByTokenHash(hash(geheimnis))
                .filter(token -> token.getZweck() == zweck)
                .filter(token -> token.istGueltig(jetzt))
                .isPresent();
    }

    // Prüft das Geheimnis aus dem Link und löst den Token ein.
    // Gibt den Benutzer zurück, wenn alles stimmt. Sonst ist das Ergebnis leer,
    // egal ob es den Token nicht gibt, er abgelaufen ist oder schon benutzt wurde.
    @Transactional
    public Optional<Benutzer> einloesen(String geheimnis, TokenZweck zweck) {
        if (geheimnis == null || geheimnis.isBlank()) {
            return Optional.empty();
        }
        Instant jetzt = Instant.now();
        return tokenRepository.findByTokenHash(hash(geheimnis))
                .filter(token -> token.getZweck() == zweck)
                .filter(token -> token.istGueltig(jetzt))
                .map(token -> {
                    token.einloesen(jetzt);
                    return token.getBenutzer();
                });
    }

    // SHA-256 des Geheimnisses als Hexzahl mit 64 Zeichen.
    // Nicht privat, damit der Test einen Token mit bekanntem Hash anlegen kann.
    static String hash(String geheimnis) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(geheimnis.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 gibt es in jeder Java-Installation, der Fall kann nicht eintreten.
            throw new IllegalStateException(e);
        }
    }
}