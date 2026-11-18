package com.hsbi.hotelverwaltung.gast.ui;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Paragraph;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

// Popup-Fenster, wenn es den Gast vermutlich schon gibt. Gehört zu UC11 LegeGastprofilAn, E1.
// Zeigt die gefundenen Profile in einer Tabelle. Der Mitarbeiter entscheidet:
//   - "Bestehendes Profil verwenden" → es wird nichts gespeichert
//   - "Trotzdem neu anlegen"         → nur erlaubt, wenn die E-Mail NICHT vergeben ist
//   - "Abbrechen"                    → zurück zur Maske, Eingaben bleiben stehen
//
// Der Dialog selbst entscheidet nichts. Was bei einem Klick passiert, bekommt er
// von der View mitgegeben (wie im SperrenDialog vom Zimmerstatus).
class DublettenDialog extends Dialog {

    // Deutsches Datumsformat für die Tabelle, z. B. 04.03.1992.
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Tabelle mit den gefundenen Gästen.
    private final Grid<Gast> tabelle = new Grid<>();

    // Konstruktor: baut das Popup auf.
    // Bekommt:
    //   dubletten          – die gefundenen Gäste (mindestens einer)
    //   neuAnlegenErlaubt  – false, wenn die E-Mail schon vergeben ist
    //   beiVerwenden       – was passieren soll, wenn ein Gast gewählt wird.
    //                        Consumer<Gast> = "eine Methode, die einen Gast bekommt".
    //   beiNeuAnlegen      – was passieren soll bei "Trotzdem neu anlegen".
    //                        Runnable = "eine Methode ohne Parameter".
    // Gibt nichts zurück.
    DublettenDialog(List<Gast> dubletten, boolean neuAnlegenErlaubt,
                    Consumer<Gast> beiVerwenden, Runnable beiNeuAnlegen) {

        setHeaderTitle("Gast vermutlich schon vorhanden");
        setWidth("min(48rem, 95vw)");

        // Erklärung für den Mitarbeiter, je nachdem ob neu anlegen erlaubt ist.
        if (neuAnlegenErlaubt) {
            add(new Paragraph("Es gibt bereits einen Gast mit diesem Namen und Geburtsdatum. "
                    + "Bitte prüfen, ob es dieselbe Person ist."));
        } else {
            add(new Paragraph("Zu dieser E-Mail-Adresse gibt es bereits ein Gästeprofil. "
                    + "Ein zweites Profil mit derselben E-Mail-Adresse ist nicht möglich."));
        }

        // Tabelle mit den gefundenen Gästen.
        tabelle.addColumn(Gast::getGastNr).setHeader("Gastnr.").setAutoWidth(true).setFlexGrow(0);
        tabelle.addColumn(Gast::getVollerName).setHeader("Name").setAutoWidth(true);
        tabelle.addColumn(gast -> gast.getGeburtsdatum().format(DATUM)).setHeader("Geburtsdatum").setAutoWidth(true);
        tabelle.addColumn(Gast::getEmail).setHeader("E-Mail").setAutoWidth(true);
        tabelle.addColumn(gast -> adresseAlsText(gast)).setHeader("Anschrift").setFlexGrow(1);
        tabelle.setItems(dubletten);
        tabelle.addThemeVariants(GridVariant.ROW_STRIPES);
        tabelle.setAllRowsVisible(true);
        // Der erste Treffer ist schon ausgewählt, damit der Mitarbeiter nur noch klicken muss.
        tabelle.select(dubletten.get(0));
        add(tabelle);

        // Knöpfe unten im Popup.
        Button abbrechen = new Button("Abbrechen", event -> close());
        abbrechen.addThemeVariants(ButtonVariant.TERTIARY);

        Button verwenden = new Button("Bestehendes Profil verwenden", event -> {
            // Welcher Gast ist in der Tabelle ausgewählt?
            Gast gewaehlt = tabelle.asSingleSelect().getValue();
            if (gewaehlt == null) {
                return; // nichts ausgewählt → nichts tun
            }
            close();
            beiVerwenden.accept(gewaehlt);
        });
        verwenden.addThemeVariants(ButtonVariant.PRIMARY);

        getFooter().add(abbrechen);
        // "Trotzdem neu anlegen" gibt es nur, wenn die E-Mail nicht vergeben ist.
        if (neuAnlegenErlaubt) {
            Button neuAnlegen = new Button("Trotzdem neu anlegen", event -> {
                close();
                beiNeuAnlegen.run();
            });
            getFooter().add(neuAnlegen);
        }
        getFooter().add(verwenden);
    }

    // Anschrift als eine Zeile, z. B. "Hauptstraße 12, 33602 Bielefeld".
    // Bekommt: den Gast. Gibt zurück: die Anschrift oder "–", wenn keine hinterlegt ist.
    private String adresseAlsText(Gast gast) {
        if (gast.getAdresse() == null) {
            return "–";
        }
        return gast.getAdresse().alsText();
    }
}
