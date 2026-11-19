package com.hsbi.hotelverwaltung.layout;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.domain.Mitarbeiter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;

import java.util.Optional;

/**
 * Kopfzeile für beide Portale: links Logo und Hotelname, rechts die angemeldete
 * Person mit Rolle und Abmelden. Ohne Anmeldung steht rechts ein Anmelden-Button.
 *
 * Begriffe:
 * Bereich: Text hinter dem Hotelnamen, z. B. "Gästeportal" oder "Verwaltung".
 * AuthenticationContext: Vaadin-Hilfsklasse, die weiß, wer gerade angemeldet ist.
 */
public class Kopfzeile extends HorizontalLayout {

    // transient, weil AuthenticationContext nicht serialisierbar ist (wie in HomeView).
    private final transient AuthenticationContext authContext;

    public Kopfzeile(String bereich, AuthenticationContext authContext,
                     BenutzerRepository benutzerRepository) {
        this.authContext = authContext;

        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassName("kopfzeile");

        // Links: Logo und Name. Der Pfad ist relativ zu META-INF/resources.
        Image logo = new Image("/images/logo.png", "Hotel Sparrenhof");
        logo.setHeight("46px");
        Span hotelname = new Span("Hotel Sparrenhof");
        hotelname.addClassName("kopfzeile-name");
        Span bereichstext = new Span("· " + bereich);
        bereichstext.addClassName("kopfzeile-bereich");
        add(logo, hotelname, bereichstext);

        // Rechts: Wer ist angemeldet? Leer, wenn niemand angemeldet ist.
        Optional<Benutzer> benutzer = authContext.getPrincipalName()
                .flatMap(benutzerRepository::findByEmailIgnoreCase);

        Div rechts = benutzer.map(this::angemeldetBereich)
                .orElseGet(this::anmeldeBereich);
        rechts.getStyle().set("margin-left", "auto");   // schiebt den Block ganz nach rechts
        add(rechts);
    }

    // Avatar, Name mit E-Mail, Rollen-Badge und Abmelden.
    private Div angemeldetBereich(Benutzer benutzer) {
        Avatar avatar = new Avatar(benutzer.getVollerName());

        Span name = new Span(benutzer.getVollerName());
        Span email = new Span(benutzer.getEmail());
        email.addClassName("kopfzeile-email");
        Div wer = new Div(name, email);
        wer.addClassName("kopfzeile-wer");

        Span rolle = new Span(rolleVon(benutzer));
        rolle.addClassName("rollen-badge");

        Button abmelden = new Button("Abmelden", klick -> authContext.logout());

        return zeile(avatar, wer, rolle, abmelden);
    }

    // Für Gäste ohne Anmeldung: nur ein Button zur Anmeldeseite.
    private Div anmeldeBereich() {
        Button anmelden = new Button("Anmelden",
                klick -> UI.getCurrent().navigate("login"));
        return zeile(anmelden);
    }

    // Packt Komponenten nebeneinander mit Abstand und mittig ausgerichtet.
    private Div zeile(com.vaadin.flow.component.Component... teile) {
        HorizontalLayout layout = new HorizontalLayout(teile);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return new Div(layout);
    }

    // Lesbarer Name der Rolle. Gäste haben keine gespeicherte Rolle.
    private String rolleVon(Benutzer benutzer) {
        if (benutzer instanceof Mitarbeiter mitarbeiter) {
            return mitarbeiter.getRolle().getBezeichnung();
        }
        return "Gast";
    }
}