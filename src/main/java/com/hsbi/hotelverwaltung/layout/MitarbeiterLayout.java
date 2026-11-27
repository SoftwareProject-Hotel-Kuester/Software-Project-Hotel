package com.hsbi.hotelverwaltung.layout;

import com.hsbi.hotelverwaltung.belegung.BelegungsuebersichtView;
import com.hsbi.hotelverwaltung.startseite.MitarbeiterStartView;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.gast.ui.GastAnlegenView;
import com.hsbi.hotelverwaltung.gast.ui.GastBearbeitenView;
import com.hsbi.hotelverwaltung.preis.PreisverwaltungView;
import com.hsbi.hotelverwaltung.verfuegbarkeit.VerfuegbarkeitView;
import com.hsbi.hotelverwaltung.zimmer.ZimmerverwaltungView;
import com.hsbi.hotelverwaltung.zimmerstatus.ui.ZimmerstatusView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Rahmen für alle Seiten des Mitarbeiterportals: oben die Kopfzeile, links das
 * Menü. Mitarbeiter und Hotelleitung benutzen denselben Rahmen, nur die Gruppe
 * "Verwaltung" sehen allein die Hotelleitung.
 *
 * Begriffe:
 * AppLayout: Vaadin-Grundgerüst mit Kopfzeile (Navbar) und Seitenleiste (Drawer).
 * SideNav: Menü in der Seitenleiste. Ein SideNavItem ist ein Menüpunkt darin.
 * RolesAllowed: Vaadin prüft diese Regel auch für das Layout, nicht nur für die Seite. Ohne sie ist keine Seite im Layout erreichbar.
 */
import jakarta.annotation.security.RolesAllowed;
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class MitarbeiterLayout extends AppLayout {

    public MitarbeiterLayout(AuthenticationContext authContext,
                             BenutzerRepository benutzerRepository) {

        addToNavbar(new DrawerToggle(), new Kopfzeile("Verwaltung", authContext, benutzerRepository));

        VerticalLayout menue = new VerticalLayout();

        // Seite null bedeutet: gibt es noch nicht, der Punkt wird ausgegraut.
        menue.add(gruppe("Start",
                eintrag("Übersicht", MitarbeiterStartView.class)));

        menue.add(gruppe("Rezeption",
                eintrag("Check-in", null),
                eintrag("Check-out", null)));

        menue.add(gruppe("Buchungen",
                eintrag("Verfügbarkeit prüfen", VerfuegbarkeitView.class),
                eintrag("Neue Buchung", null),
                eintrag("Buchungen", null),
                eintrag("Belegungsübersicht", BelegungsuebersichtView.class)));

        menue.add(gruppe("Gäste",
                eintrag("Gast anlegen", GastAnlegenView.class),
                eintrag("Gast bearbeiten", GastBearbeitenView.class)));

        menue.add(gruppe("Zimmer & Reinigung",
                eintrag("Zimmerstatus", ZimmerstatusView.class),
                eintrag("Reinigungsplan", null)));

        // Nur für die Hotelleitung sichtbar. Der eigentliche Schutz liegt
        // trotzdem an den Seiten selbst (@RolesAllowed), das hier ist nur Anzeige.
        if (authContext.hasRole("HOTELLEITUNG")) {
            menue.add(gruppe("Verwaltung",
                    eintrag("Zimmerverwaltung", ZimmerverwaltungView.class),
                    eintrag("Preise", PreisverwaltungView.class),
                    eintrag("Kosten", null),
                    eintrag("Mitarbeiter & Rollen", null),
                    eintrag("Statistik & Export", null)));
        }

        addToDrawer(menue);
    }

    // Mit Seite: normaler Link. Ohne Seite: sichtbar, aber ausgegraut und nicht klickbar.
    private SideNavItem eintrag(String text, Class<? extends Component> seite) {
        if (seite == null) {
            SideNavItem platzhalter = new SideNavItem(text);
            platzhalter.setEnabled(false);
            return platzhalter;
        }
        return new SideNavItem(text, seite);
    }

    // Eine Menügruppe mit Überschrift.
    private SideNav gruppe(String titel, SideNavItem... eintraege) {
        SideNav gruppe = new SideNav(titel);
        gruppe.addItem(eintraege);
        return gruppe;
    }
}