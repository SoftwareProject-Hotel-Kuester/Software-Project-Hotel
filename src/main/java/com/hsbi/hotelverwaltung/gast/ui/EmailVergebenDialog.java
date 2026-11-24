package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Benutzer;
import com.hsbi.hotelverwaltung.domain.Gast;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

import java.time.format.DateTimeFormatter;

// Popup, wenn die neue E-Mail-Adresse schon einem anderen Benutzer gehört.
// Gehört zu UC12 BearbeiteGastprofil, E1.
// Zeigt das andere Profil an. Der Mitarbeiter entscheidet:
//   - "E-Mail korrigieren"   → zurück zur Maske, Eingaben bleiben stehen
//   - "Änderung verwerfen"   → gespeicherter Stand wird wieder geladen
//
// Gehört die E-Mail einem Mitarbeiter, zeigen wir nur das, aber keine Daten des Mitarbeiters.
// Wie der DublettenDialog entscheidet dieser Dialog nichts selbst: Was beim Verwerfen
// passiert, gibt ihm die View mit.
class EmailVergebenDialog extends Dialog {

    // Deutsches Datumsformat, z. B. 04.03.1992.
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Konstruktor: baut das Popup auf.
    // Bekommt:
    //   anderer       – der Benutzer, dem die E-Mail gehört (Gast oder Mitarbeiter)
    //   beiVerwerfen  – was bei "Änderung verwerfen" passieren soll (Runnable = Methode ohne Parameter)
    // Gibt nichts zurück.
    EmailVergebenDialog(Benutzer anderer, Runnable beiVerwerfen) {
        setHeaderTitle("E-Mail-Adresse bereits vergeben");
        setWidth("min(32rem, 95vw)");

        // instanceof prüft, ob der Benutzer ein Gast ist (und kein Mitarbeiter).
        if (anderer instanceof Gast) {
            Gast gast = (Gast) anderer;
            add(new Paragraph("Die E-Mail-Adresse " + gast.getEmail() + " gehört bereits zu diesem Gast:"));
            add(gastDaten(gast));
            add(new Paragraph("Bitte eine andere E-Mail-Adresse eingeben oder die Änderung verwerfen."));
        } else {
            add(new Paragraph("Die E-Mail-Adresse " + anderer.getEmail()
                    + " gehört zu einem Mitarbeiterkonto und kann nicht für einen Gast verwendet werden."));
        }

        // Knöpfe unten im Popup.
        Button verwerfen = new Button("Änderung verwerfen", event -> {
            close();
            beiVerwerfen.run();
        });
        verwerfen.addThemeVariants(ButtonVariant.TERTIARY);

        Button korrigieren = new Button("E-Mail korrigieren", event -> close());
        korrigieren.addThemeVariants(ButtonVariant.PRIMARY);

        getFooter().add(verwerfen, korrigieren);
    }

    // Kasten mit den wichtigsten Daten des anderen Gastes.
    // Bekommt: den Gast. Gibt zurück: den Kasten.
    private Div gastDaten(Gast gast) {
        Div kasten = new Div();
        kasten.add(new Div("Gastnummer: " + gast.getGastNr()));
        kasten.add(new Div("Name: " + gast.getVollerName()));
        if (gast.getGeburtsdatum() != null) {
            kasten.add(new Div("Geburtsdatum: " + gast.getGeburtsdatum().format(DATUM)));
        }
        if (gast.getAdresse() != null) {
            kasten.add(new Div("Anschrift: " + gast.getAdresse().alsText()));
        }
        kasten.getStyle()
                .set("background-color", "#eef1f5")
                .set("border-radius", "8px")
                .set("padding", "0.5rem 0.75rem");
        return kasten;
    }
}
