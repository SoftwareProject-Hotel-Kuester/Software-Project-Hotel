package com.hsbi.hotelverwaltung.gaesteportal;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

// Gemeinsamer Rahmen für alle Seiten des Gästeportals (wie im Mockup):
// links der Hotelname, rechts das Menü "Zimmersuche · Meine Buchungen · Profil".
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

    public GaesteportalLayout() {
        H3 hotelname = new H3("Hotel Sparrenhof");

        // Zimmersuche und Profil gibt es noch nicht, deshalb normale Links über die Adresse.
        Anchor zimmersuche = new Anchor(ROUTE_ZIMMERSUCHE, "Zimmersuche");
        RouterLink meineBuchungen = new RouterLink("Meine Buchungen", MeineBuchungenView.class);
        Anchor profil = new Anchor(ROUTE_PROFIL, "Profil");
        // TODO Gruppe 6: Anmelden/Abmelden hier ergänzen

        HorizontalLayout menue = new HorizontalLayout(zimmersuche, meineBuchungen, profil);

        // Kopfzeile: Hotelname links, Menü rechts.
        HorizontalLayout kopfzeile = new HorizontalLayout(hotelname, menue);
        kopfzeile.setWidthFull();
        kopfzeile.setAlignItems(FlexComponent.Alignment.CENTER);
        kopfzeile.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        kopfzeile.getStyle().set("padding", "0 1.5rem");

        addToNavbar(kopfzeile);
    }
}
