package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.security.LoginView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Registrierung für neue Gäste. Die Seite sammelt nur Name, Geburtsdatum und
 * E-Mail. Das Passwort legt der Gast später über den Link in der E-Mail fest.
 * Die View prüft selbst keine Regeln, das macht der RegistrationService.
 *
 * Begriffe:
 * AnonymousAllowed: Die Seite darf auch ohne Anmeldung geöffnet werden.
 */
@Route(value = "registrierung", autoLayout = false)
@PageTitle("Registrieren | Hotel Sparrenhof")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

    private final RegistrationService registrationService;
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationView.class);
    private final TextField vorname = new TextField("Vorname");
    private final TextField nachname = new TextField("Nachname");
    private final DatePicker geburtsdatum = new DatePicker("Geburtsdatum");
    private final EmailField email = new EmailField("E-Mail-Adresse");
    private final Image logo = new Image("/images/logo.png", "Hotel Sparrenhof");

    public RegistrationView(RegistrationService registrationService) {
        this.registrationService = registrationService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        logo.setHeight("140px");

        // Vaadin soll die Felder nicht selbst prüfen, das macht der Service.
        vorname.setRequiredIndicatorVisible(true);
        nachname.setRequiredIndicatorVisible(true);
        geburtsdatum.setRequiredIndicatorVisible(true);
        email.setRequiredIndicatorVisible(true);
        geburtsdatum.setLocale(Locale.GERMANY);
        geburtsdatum.setMax(LocalDate.now());

        // Eine Spalte, damit die Felder auch auf schmalen Bildschirmen gut passen.
        FormLayout formular = new FormLayout(vorname, nachname, geburtsdatum, email);
        formular.setAutoResponsive(true);
        formular.setMaxColumns(1);
        // Spalte und Felder füllen die ganze Formularbreite, sonst bleiben sie links schmal stehen.
        formular.setExpandColumns(true);
        formular.setExpandFields(true);
        formular.setWidth("420px");
        formular.setMaxWidth("100%");

        Button registrieren = new Button("Registrieren", klick -> absenden());
        registrieren.addThemeVariants(ButtonVariant.PRIMARY);

        add(logo, new H2("Konto erstellen"), formular, registrieren,
                new RouterLink("Schon ein Konto? Zur Anmeldung", LoginView.class));
        setAlignSelf(Alignment.CENTER, formular);
    }

    // Schickt die Eingaben an den Service und zeigt danach die Antwort an.
    private void absenden() {
        try {
            registrationService.registrieren(new RegistrierungsDaten(
                    vorname.getValue(), nachname.getValue(), geburtsdatum.getValue(), email.getValue()));
        } catch (IllegalArgumentException fehler) {
            // Ungültige Eingabe, die Meldung kommt vom Service.
            zeigeFehler(fehler.getMessage());
            return;
        } catch (MailException fehler) {
            LOG.warn("Bestätigungsmail konnte nicht verschickt werden", fehler);
            zeigeFehler("Die E-Mail konnte gerade nicht verschickt werden. Bitte versuchen Sie es später noch einmal.");
            return;
        }
        zeigeBestaetigung(email.getValue().trim());
    }

    // Die Antwort ist immer dieselbe, egal ob es die Adresse schon gab.
    private void zeigeBestaetigung(String adresse) {
        removeAll();
        add(logo,
                new H2("Bitte prüfen Sie Ihr Postfach"),
                new Paragraph("Wir haben eine E-Mail an " + adresse + " geschickt. "
                        + "Öffnen Sie den Link darin, um Ihr Passwort festzulegen. "
                        + "Der Link ist 24 Stunden gültig. Keine E-Mail erhalten? "
                        + "Schauen Sie im Spam-Ordner nach oder registrieren Sie sich noch einmal."),
                new RouterLink("Zur Anmeldung", LoginView.class));
    }

    private void zeigeFehler(String text) {
        Notification meldung = Notification.show(text, 5000, Notification.Position.MIDDLE);
        meldung.addThemeVariants(NotificationVariant.ERROR);
    }
}