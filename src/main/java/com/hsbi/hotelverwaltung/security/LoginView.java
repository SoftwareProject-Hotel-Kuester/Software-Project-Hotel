package com.hsbi.hotelverwaltung.security;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Anmeldeseite unter /login. Das Formular schickt E-Mail und Passwort an
 * Spring Security, die Prüfung passiert nicht in dieser Klasse.
 *
 * Begriffe:
 * BeforeEnterObserver: Vaadin ruft beforeEnter auf, bevor die Seite angezeigt wird.
 * i18n: Abkürzung für "internationalization", hier die Texte des Formulars.
 */
@Route(value = "login", autoLayout = false)
@PageTitle("Anmelden | Hotel Sparrenhof")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        // Spring Security nimmt das Formular unter /login per POST entgegen.
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setI18n(deutscheTexte());

        add(new H1("Hotel Sparrenhof"), loginForm);
    }

    // Bei falschen Zugangsdaten leitet Spring Security auf /login?error um.
    // Dann zeigen wir die Fehlermeldung des Formulars an.
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean fehler = event.getLocation().getQueryParameters()
                .getParameters().containsKey("error");
        loginForm.setError(fehler);
    }

    // Setzt nur die Texte, die wir ändern. Der Rest bleibt Standard.
    private LoginI18n deutscheTexte() {
        LoginI18n i18n = new LoginI18n();

        LoginI18n.Form form = new LoginI18n.Form();
        form.setTitle("Anmelden");
        form.setUsername("E-Mail");
        form.setPassword("Passwort");
        form.setSubmit("Anmelden");
        i18n.setForm(form);

        LoginI18n.ErrorMessage fehler = new LoginI18n.ErrorMessage();
        fehler.setTitle("Anmeldung fehlgeschlagen");
        fehler.setMessage("E-Mail oder Passwort stimmt nicht, oder das Konto ist noch nicht freigeschaltet.");
        i18n.setErrorMessage(fehler);

        return i18n;
    }
}