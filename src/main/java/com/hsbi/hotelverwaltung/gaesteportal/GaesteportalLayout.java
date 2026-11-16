package com.hsbi.hotelverwaltung.gaesteportal;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Gemeinsamer Rahmen aller Seiten des Gästeportals (wie im Mockup):
 * links der Hotelname, rechts das Menü "Zimmersuche · Meine Buchungen · Profil".
 *
 * Eine Seite erscheint in diesem Rahmen, wenn sie
 * {@code @Route(value = "...", layout = GaesteportalLayout.class)} angibt.
 *
 * Der Mitarbeiterbereich bekommt ein eigenes Layout (nicht Teil des Gästeportals).
 */
@AnonymousAllowed // Das Menü selbst ist für alle sichtbar, die Seiten prüfen ihre Rechte selbst.
public class GaesteportalLayout extends AppLayout {

    /** Route der Profilseite (UC21). TODO: anpassen, sobald die Seite existiert. */
    static final String ROUTE_PROFIL = "profil";

    public GaesteportalLayout() {
        Span hotelname = new Span("Hotel Sparrenhof");
        hotelname.getStyle().set("font-size", "1.25rem").set("font-weight", "600");

        // Zimmersuche und Profil gibt es noch nicht, deshalb einfache Links über die Adresse.
        // Sobald die Views existieren: new RouterLink("Zimmersuche", ZimmersucheView.class) usw.
        Anchor zimmersuche = new Anchor(MeineBuchungenView.ROUTE_ZIMMERSUCHE, "Zimmersuche");
        RouterLink meineBuchungen = new RouterLink("Meine Buchungen", MeineBuchungenView.class);
        Anchor profil = new Anchor(ROUTE_PROFIL, "Profil");
        // TODO Gruppe 6: Anmelden/Abmelden hier ergänzen

        HorizontalLayout menue = new HorizontalLayout(zimmersuche, meineBuchungen, profil);
        menue.getStyle().set("gap", "1.5rem");

        HorizontalLayout kopfzeile = new HorizontalLayout(hotelname, menue);
        kopfzeile.setWidthFull();
        kopfzeile.setAlignItems(FlexComponent.Alignment.CENTER);
        kopfzeile.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        kopfzeile.getStyle().set("padding", "0 1.5rem");

        addToNavbar(kopfzeile);
    }
}
