package com.hsbi.hotelverwaltung.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Verschickt alle E-Mails der Anwendung als einfachen Text. In der Entwicklung
 * landen sie in Mailpit (http://localhost:8025), es geht nichts nach draußen.
 *
 * Begriffe:
 * SMTP: Standardverfahren, mit dem E-Mails an einen Mailserver gesendet werden.
 * JavaMailSender: Spring-Klasse, die die Verbindung zum Mailserver kennt.
 * Value: Spring trägt hier einen Wert aus der application.yaml ein.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String absender;

    // Spring übergibt den JavaMailSender (aus spring.mail.* in der application.yaml)
    // und den Absender (hotel.mail-absender).
    public MailService(JavaMailSender mailSender,
                       @Value("${hotel.mail-absender}") String absender) {
        this.mailSender = mailSender;
        this.absender = absender;
    }

    // Mail an einen neuen Gast mit dem Link zum Bestätigen der E-Mail-Adresse.
    // Über den Link legt der Gast auch sein Passwort fest.
    public void sendeBestaetigungsLink(String an, String vorname, String link) {
        String text = """
                Guten Tag %s,

                vielen Dank für Ihre Registrierung im Hotel Sparrenhof.
                Über diesen Link bestätigen Sie Ihre E-Mail-Adresse und legen
                Ihr Passwort fest:

                %s

                Der Link ist 24 Stunden gültig. Wenn Sie sich nicht registriert
                haben, können Sie diese Nachricht ignorieren.

                Ihr Hotel Sparrenhof
                """.formatted(vorname, link);
        senden(an, "Bitte bestätigen Sie Ihre E-Mail-Adresse und legen Sie Ihr Passwort fest", text);
    }

    // Mail an jemanden, der sich mit einer schon vergebenen Adresse registriert hat.
    // Ohne diese Mail würde der Besucher im Browser nie erfahren, dass es schon ein Konto gibt.
    public void sendeKontoBereitsVorhanden(String an, String vorname, String anmeldeLink) {
        String text = """
                Guten Tag %s,

                für diese E-Mail-Adresse gibt es im Hotel Sparrenhof schon ein Konto.
                Sie können sich direkt anmelden:

                %s

                Wenn Sie sich nicht erneut registrieren wollten, können Sie diese
                Nachricht ignorieren.

                Ihr Hotel Sparrenhof
                """.formatted(vorname, anmeldeLink);
        senden(an, "Ihr Konto im Hotel Sparrenhof", text);
    }

    // UC20: Bestätigung, nachdem ein Gast seine Buchung im Gästeportal storniert hat.
    // Zimmer und Zeitraum kommen schon als fertiger Text, z. B. "15.06.2027 bis 18.06.2027".
    public void sendeStornobestaetigung(String an, String vorname, int buchungsNr, int zimmerNr, String zeitraum) {
        String text = """
                Guten Tag %s,

                Ihre Buchung Nr. %d wurde storniert.

                Zimmer: %d
                Zeitraum: %s

                Wenn Sie diese Stornierung nicht selbst vorgenommen haben,
                melden Sie sich bitte bei uns.

                Ihr Hotel Sparrenhof
                """.formatted(vorname, buchungsNr, zimmerNr, zeitraum);
        senden(an, "Ihre Buchung Nr. " + buchungsNr + " wurde storniert", text);
    }

    // Baut die Mail zusammen und schickt sie ab. Bei Problemen mit dem Mailserver
    // wirft Spring eine MailException, die der Aufrufer behandeln muss.
    private void senden(String an, String betreff, String text) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(absender);
        mail.setTo(an);
        mail.setSubject(betreff);
        mail.setText(text);
        mailSender.send(mail);
    }
}