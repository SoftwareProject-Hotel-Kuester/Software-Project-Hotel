package com.hsbi.hotelverwaltung.startseite;

import com.hsbi.hotelverwaltung.belegung.BelegungsuebersichtView;
import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.BenutzerRepository;
import com.hsbi.hotelverwaltung.gast.ui.GastAnlegenView;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import com.hsbi.hotelverwaltung.verfuegbarkeit.VerfuegbarkeitView;
import com.hsbi.hotelverwaltung.zimmer.ZimmerverwaltungView;
import com.hsbi.hotelverwaltung.zimmerstatus.ui.ZimmerstatusView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Startseite für Mitarbeiter und Hotelleitung. Von hier aus erreicht man die
 * Funktionen des Hauses. Die Kachel "Zimmerverwaltung" sehen nur Hotelleitung.
 */
@Route(value = "uebersicht", layout = MitarbeiterLayout.class)
@PageTitle("Übersicht | Hotel Sparrenhof")
@RolesAllowed({"MITARBEITER", "HOTELLEITUNG"})
public class MitarbeiterStartView extends VerticalLayout {

    public MitarbeiterStartView(AuthenticationContext authContext,
                                BenutzerRepository benutzerRepository) {

        // Vorname der angemeldeten Person für die Begrüßung.
        String vorname = authContext.getPrincipalName()
                .flatMap(benutzerRepository::findByEmailIgnoreCase)
                .map(Benutzer::getVorname)
                .orElse("");

        String heute = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN));

        Div raster = new Div();
        raster.addClassName("kachel-raster");

        // Seite null bedeutet: gibt es noch nicht, die Kachel wird grau.
        raster.add(
                new Kachel("Check-in", "Ankommende Gäste einchecken.", null),
                new Kachel("Check-out", "Abreisende Gäste auschecken.", null),
                new Kachel("Verfügbarkeit prüfen", "Freie Zimmer für einen Zeitraum suchen.",
                        VerfuegbarkeitView.class),
                new Kachel("Belegungsübersicht", "Belegungskalender aller Zimmer ansehen.",
                        BelegungsuebersichtView.class),
                new Kachel("Neue Buchung", "Eine Buchung für einen Gast anlegen.", null),
                new Kachel("Gast anlegen", "Ein Gästeprofil neu erfassen.", GastAnlegenView.class),
                new Kachel("Zimmerstatus", "Belegung, Reinigung und Sperren der Zimmer.",
                        ZimmerstatusView.class));

        if (authContext.hasRole("HOTELLEITUNG")) {
            raster.add(new Kachel("Zimmerverwaltung", "Zimmer anlegen, ändern und löschen.",
                    ZimmerverwaltungView.class));
        }

        add(new H2("Willkommen, " + vorname + "!"), new Paragraph(heute), raster);
    }
}