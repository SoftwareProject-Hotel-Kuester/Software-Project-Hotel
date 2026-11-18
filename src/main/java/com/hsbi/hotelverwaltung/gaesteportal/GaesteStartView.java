package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.startseite.Kachel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

/**
 * Startseite des Gästeportals. Gäste kommen nach dem Login hierher und erreichen
 * von hier ihre Funktionen. Seiten, die es noch nicht gibt, sind grau.
 *
 * Begriffe:
 * RolesAllowed("GAST"): Nur Konten mit der Rolle Gast dürfen die Seite öffnen.
 */
@Route(value = "willkommen", layout = GaesteportalLayout.class)
@PageTitle("Willkommen | Hotel Sparrenhof")
@RolesAllowed("GAST")
public class GaesteStartView extends VerticalLayout {

    public GaesteStartView(AuthenticationContext authContext,
                           BenutzerRepository benutzerRepository) {

        String vorname = authContext.getPrincipalName()
                .flatMap(benutzerRepository::findByEmailIgnoreCase)
                .map(Benutzer::getVorname)
                .orElse("");

        Div raster = new Div();
        raster.addClassName("kachel-raster");

        // Ziel null bedeutet: Die Seite gibt es noch nicht, die Kachel wird grau.
        raster.add(
                new Kachel("Zimmer suchen & buchen", "Freie Zimmer finden und buchen.", null),
                new Kachel("Meine Buchungen", "Ihre Buchungen ansehen.", MeineBuchungenView.class),
                new Kachel("Zimmerreinigung", "Die Reinigung für Ihren Aufenthalt festlegen.", null),
                new Kachel("Mein Profil", "Name, Adresse, Telefon und Passwort bearbeiten.", null));

        add(new H2("Willkommen zurück, " + vorname + "!"),
                new Paragraph("Hier finden Sie alle Funktionen für Ihren Aufenthalt."),
                raster);
    }
}