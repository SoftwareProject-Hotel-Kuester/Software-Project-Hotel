package com.hsbi.hotelverwaltung.gaesteportal;

import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.layout.Kopfzeile;
import com.hsbi.hotelverwaltung.zimmerstatus.ui.ZimmerreinigungView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

// Gemeinsamer Rahmen für alle Seiten des Gästeportals:
// oben die gemeinsame Kopfzeile (mit Anmelden/Abmelden), links das Menü
// "Startseite · Zimmersuche · Meine Buchungen · Profil".
//
// Eine Seite erscheint in diesem Rahmen, wenn sie
// @Route(value = "...", layout = GaesteportalLayout.class) angibt.
//
// extends AppLayout: Vaadin-Grundgerüst mit Kopfzeile (Navbar) und Inhaltsbereich.
// @AnonymousAllowed: Das Menü darf jeder sehen. Die Seiten selbst prüfen ihre Rechte.
@AnonymousAllowed
public class GaesteportalLayout extends AppLayout {

    // Adressen der Seiten. TODO: an die echten Seiten anpassen, sobald es sie gibt.
    static final String ROUTE_ZIMMERSUCHE = "zimmersuche";   // UC16, Gruppe 2
    static final String ROUTE_PROFIL = "profil";             // UC21, Gästeportal

    // Spring übergibt AuthenticationContext und BenutzerRepository automatisch.
    // Beides braucht die gemeinsame Kopfzeile aus dem Paket layout,
    // um den Namen des angemeldeten Benutzers und den Abmelden-Button anzuzeigen.
    public GaesteportalLayout(AuthenticationContext authContext,
                              BenutzerRepository benutzerRepository) {

        // Zimmersuche und Profil gibt es noch nicht, deshalb normale Links über die Adresse.
        Anchor zimmersuche = new Anchor(ROUTE_ZIMMERSUCHE, "Zimmersuche");
        RouterLink meineBuchungen = new RouterLink("Meine Buchungen", MeineBuchungenView.class);
        Anchor profil = new Anchor(ROUTE_PROFIL, "Profil");
        RouterLink startseite = new RouterLink("Startseite", GaesteStartView.class);
        RouterLink zimmerreinigung = new RouterLink("Zimmerreinigung", ZimmerreinigungView.class); // UC07

        // Kopfzeile oben (mit Anmelden/Abmelden), Menü in der linken Seitenleiste.
        addToNavbar(new DrawerToggle(), new Kopfzeile("Gästeportal", authContext, benutzerRepository));
        addToDrawer(new VerticalLayout(startseite, zimmersuche, meineBuchungen, zimmerreinigung, profil));
    }
}
