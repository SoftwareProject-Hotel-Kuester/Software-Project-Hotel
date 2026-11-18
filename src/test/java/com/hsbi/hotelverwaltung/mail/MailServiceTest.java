package com.hsbi.hotelverwaltung.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// Tests für MailService. Es wird nichts wirklich verschickt.
//
// mock(...): ein Attrappen-Objekt statt des echten Mailservers. Mockito merkt sich,
// womit es aufgerufen wurde, und wir prüfen das danach mit verify(...).
// ArgumentCaptor: fängt die Mail ab, die an die Attrappe übergeben wurde.
class MailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MailService mailService = new MailService(mailSender, "noreply@sparrenhof.test");

    @Test
    void bestaetigungsLinkGehtAnDieRichtigeAdresseUndEnthaeltDenLink() {
        mailService.sendeBestaetigungsLink("gina@beispiel.de", "Gina",
                "http://localhost:8080/bestaetigen?token=abc");

        SimpleMailMessage mail = gesendeteMail();
        assertArrayEquals(new String[]{"gina@beispiel.de"}, mail.getTo());
        assertEquals("noreply@sparrenhof.test", mail.getFrom());
        assertTrue(mail.getText().contains("http://localhost:8080/bestaetigen?token=abc"));
        assertTrue(mail.getText().contains("Gina"));
    }

    @Test
    void kontoBereitsVorhandenEnthaeltDenAnmeldeLink() {
        mailService.sendeKontoBereitsVorhanden("gina@beispiel.de", "Gina",
                "http://localhost:8080/login");

        SimpleMailMessage mail = gesendeteMail();
        assertArrayEquals(new String[]{"gina@beispiel.de"}, mail.getTo());
        assertTrue(mail.getText().contains("http://localhost:8080/login"));
    }

    // Holt die Mail, die der MailService an die Attrappe übergeben hat.
    private SimpleMailMessage gesendeteMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}