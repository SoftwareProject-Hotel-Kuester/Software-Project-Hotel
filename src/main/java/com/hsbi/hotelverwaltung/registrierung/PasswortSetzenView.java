package com.hsbi.hotelverwaltung.registrierung;

import com.hsbi.hotelverwaltung.domain.TokenZweck;
import com.hsbi.hotelverwaltung.security.LoginView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Seite hinter dem Link aus der E-Mail. Der Gast legt hier sein Passwort fest,
 * danach ist sein Konto aktiv. Ist der Link ungültig oder abgelaufen, zeigt die
 * Seite stattdessen einen Hinweis.
 *
 * Begriffe:
 * Query-Parameter: Der Teil der Adresse nach dem Fragezeichen, hier ?token=...
 * BeforeEnterObserver: Vaadin ruft beforeEnter auf, bevor die Seite angezeigt wird.
 */
@Route(value = "passwort-setzen", autoLayout = false)
@PageTitle("Passwort festlegen | Hotel Sparrenhof")
@AnonymousAllowed
public class PasswortSetzenView extends VerticalLayout implements BeforeEnterObserver {

    private final RegistrationService registrationService;
    private final TokenService tokenService;

    private final Image logo = new Image("/images/logo.png", "Hotel Sparrenhof");
    private final PasswordField passwort = new PasswordField("Passwort");
    private final PasswordField wiederholung = new PasswordField("Passwort wiederholen");

    // Das Geheimnis aus dem Link. Es steht nur hier und wird nie angezeigt.
    private String geheimnis;

    public PasswortSetzenView(RegistrationService registrationService, TokenService tokenService) {
        this.registrationService = registrationService;
        this.tokenService = tokenService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        logo.setHeight("140px");
        passwort.setWidth("320px");
        wiederholung.setWidth("320px");
        passwort.setHelperText("Mindestens 8 Zeichen");
    }

    // Liest das Geheimnis aus der Adresse und entscheidet, was die Seite zeigt.
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        geheimnis = event.getLocation().getQueryParameters()
                .getSingleParameter("token").orElse(null);

        // istGueltig verbraucht den Link nicht. Erst das Absenden löst ihn ein.
        if (tokenService.istGueltig(geheimnis, TokenZweck.PASSWORT_SETZEN)) {
            zeigeFormular();
        } else {
            zeigeUngueltig();
        }
    }

    private void zeigeFormular() {
        Button speichern = new Button("Passwort festlegen", klick -> speichern());
        speichern.addThemeVariants(ButtonVariant.PRIMARY);

        removeAll();
        add(logo, new H2("Passwort festlegen"), passwort, wiederholung, speichern);
    }

    private void speichern() {
        if (!passwort.getValue().equals(wiederholung.getValue())) {
            zeigeFehler("Die beiden Passwörter stimmen nicht überein.");
            return;
        }
        try {
            if (registrationService.passwortSetzen(geheimnis, passwort.getValue())) {
                zeigeErfolg();
            } else {
                zeigeUngueltig();
            }
        } catch (IllegalArgumentException fehler) {
            // Passwort zu kurz oder zu lang. Der Link bleibt gültig.
            zeigeFehler(fehler.getMessage());
        }
    }

    private void zeigeErfolg() {
        removeAll();
        add(logo,
                new H2("Passwort gespeichert"),
                new Paragraph("Ihr Konto ist aktiv. Sie können sich jetzt anmelden."),
                new RouterLink("Zur Anmeldung", LoginView.class));
    }

    private void zeigeUngueltig() {
        removeAll();
        add(logo,
                new H2("Link nicht mehr gültig"),
                new Paragraph("Dieser Link ist abgelaufen oder wurde schon benutzt. "
                        + "Sie können sich noch einmal registrieren und bekommen einen neuen Link."),
                new RouterLink("Zur Registrierung", RegistrationView.class));
    }

    private void zeigeFehler(String text) {
        Notification meldung = Notification.show(text, 5000, Notification.Position.MIDDLE);
        meldung.addThemeVariants(NotificationVariant.ERROR);
    }
}