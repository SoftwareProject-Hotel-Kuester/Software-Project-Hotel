package com.hsbi.hotelverwaltung;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Mitarbeiter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Startseite unter "/". Nur für angemeldete Personen, egal mit welcher Rolle.
 * Zeigt den Namen der Person und einen Button zum Abmelden.
 *
 * Begriffe:
 * PermitAll: Jede angemeldete Person darf die Seite sehen, anonyme nicht.
 * AuthenticationContext: Vaadin-Hilfsklasse, die weiß, wer gerade angemeldet ist.
 */
@Route("")
@PageTitle("Startseite | Hotel Sparrenhof")
@PermitAll
public class HomeView extends VerticalLayout {

    // transient, weil AuthenticationContext nicht serialisierbar ist (so steht es in der Vaadin-Doku).
    private final transient AuthenticationContext authContext;

    // Spring übergibt AuthenticationContext und Repository automatisch.
    public HomeView(AuthenticationContext authContext, BenutzerRepository benutzerRepository) {
        this.authContext = authContext;

        // Anmeldename (E-Mail) holen und die Person in der Datenbank suchen.
        String begruessung = authContext.getPrincipalName()
                .flatMap(benutzerRepository::findByEmailIgnoreCase)
                .map(benutzer -> "Willkommen, " + benutzer.getVollerName()
                        + " (" + rolleVon(benutzer) + ")")
                .orElse("Willkommen");

        Button abmelden = new Button("Abmelden", klick -> this.authContext.logout());

        add(new H1("Hotel Sparrenhof"), new Span(begruessung), abmelden);
    }

    // Lesbarer Name der Rolle für die Anzeige. Gäste haben keine gespeicherte Rolle.
    private String rolleVon(Benutzer benutzer) {
        if (benutzer instanceof Mitarbeiter mitarbeiter) {
            return mitarbeiter.getRolle().getBezeichnung();
        }
        return "Gast";
    }
}