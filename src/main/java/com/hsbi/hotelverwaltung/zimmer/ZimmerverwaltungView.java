package com.hsbi.hotelverwaltung.zimmer;

import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

// Seite "Zimmerverwaltung" der Hotelleitung (wie im Mockup "Übersicht aller Zimmer").
// Gehört zu UC01 LegeZimmerAn, UC02 ÄndereZimmer und UC34 LöscheZimmer.
// Oben stehen die Buttons "Zimmer anlegen", "Bearbeiten" und "Löschen", darunter eine Tabelle mit allen Zimmern.
// Vorne in jeder Zeile ist ein Kreis. Ein Klick auf die Zeile wählt das Zimmer aus (UC02, Schritt 3),
// danach kann man es bearbeiten oder löschen.
//
// @Route: Die Seite ist im Browser unter http://localhost:8080/verwaltung/zimmer erreichbar.
// @PageTitle: Text im Browser-Tab.
// @AnonymousAllowed: Vorläufig darf jeder die Seite öffnen, weil es noch keinen Login gibt.
// TODO sobald es Login und Menü gibt (Philipp):
//   @RolesAllowed("HOTELLEITUNG") und @Route(value = "verwaltung/zimmer", layout = MainLayout.class)
// extends VerticalLayout: Die Bestandteile der Seite stehen untereinander.
@Route("verwaltung/zimmer")
@PageTitle("Zimmerverwaltung | Hotel Sparrenhof")
@AnonymousAllowed
public class ZimmerverwaltungView extends VerticalLayout {

    // Über den Service werden die Zimmer geladen, geändert und gelöscht (View → Service → Datenbank).
    private final ZimmerService zimmerService;

    // Grid = Tabelle. Jede Zeile ist ein Zimmer.
    private final Grid<Zimmer> tabelle = new Grid<>();

    // Buttons für das ausgewählte Zimmer. Sie sind nur klickbar, wenn ein Zimmer ausgewählt ist.
    private final Button bearbeiten = new Button("Bearbeiten");
    private final Button loeschen = new Button("Löschen");

    // Das Zimmer, das gerade ausgewählt ist. null heißt: Es ist keins ausgewählt.
    private Zimmer ausgewaehltesZimmer;

    // Baut die Seite auf. Wird jedes Mal aufgerufen, wenn jemand die Seite öffnet.
    // Bekommt: den ZimmerService (Spring übergibt ihn automatisch). Gibt nichts zurück.
    public ZimmerverwaltungView(ZimmerService zimmerService) {
        this.zimmerService = zimmerService;

        // UC01, Schritt 1: Die Hotelleitung ruft die Funktion "Zimmer anlegen" auf.
        // Der Klick öffnet das Fenster. Nach dem Speichern ruft das Fenster
        // tabelleAktualisieren() auf, damit das neue Zimmer sofort in der Tabelle steht.
        Button zimmerAnlegen = new Button("Zimmer anlegen",
                klick -> new ZimmerAnlegenDialog(zimmerService, this::tabelleAktualisieren).open());
        zimmerAnlegen.addThemeVariants(ButtonVariant.PRIMARY);

        // UC02, Schritt 1 und 4: "Bearbeiten" öffnet die Maske mit den Daten des ausgewählten Zimmers.
        bearbeiten.addClickListener(klick ->
                new ZimmerBearbeitenDialog(zimmerService, ausgewaehltesZimmer, this::tabelleAktualisieren).open());

        // "Löschen" fragt erst nach, ob das Zimmer wirklich gelöscht werden soll.
        // ButtonVariant.ERROR färbt den Button rot, damit man sieht, dass er etwas entfernt.
        loeschen.addThemeVariants(ButtonVariant.ERROR);
        loeschen.addClickListener(klick -> zeigeLoeschRueckfrage());

        // Am Anfang ist kein Zimmer ausgewählt, also sind beide Buttons ausgegraut.
        bearbeiten.setEnabled(false);
        loeschen.setEnabled(false);

        // HorizontalLayout: Die Buttons stehen nebeneinander in einer Reihe.
        HorizontalLayout buttonLeiste = new HorizontalLayout(zimmerAnlegen, bearbeiten, loeschen);

        erstelleTabelle();
        tabelleAktualisieren();

        add(new H2("Übersicht aller Zimmer"), buttonLeiste, tabelle);
    }

    // Legt die Spalten der Tabelle fest (wie im Mockup) und reagiert auf die Auswahl einer Zeile.
    // Bekommt: nichts. Gibt nichts zurück.
    private void erstelleTabelle() {
        // Erste Spalte: der Auswahlkreis. addComponentColumn zeigt in jeder Zelle ein Bauteil statt Text.
        // setFlexGrow(0) und setAutoWidth(true): Die Spalte ist nur so breit wie der Kreis.
        tabelle.addComponentColumn(zimmer -> erstelleAuswahlKreis(zimmer))
                .setAutoWidth(true)
                .setFlexGrow(0);

        // Jede weitere Spalte bekommt ein Zimmer und gibt den Text für die Zelle zurück.
        tabelle.addColumn(zimmer -> zimmer.getZimmerNr()).setHeader("Zimmer-Nr.");
        tabelle.addColumn(zimmer -> zimmer.getEtage()).setHeader("Etage");
        tabelle.addColumn(zimmer -> zimmer.getZimmerklasse().getBezeichnung()).setHeader("Klasse");
        tabelle.addColumn(zimmer -> zimmer.getBelegungsart().getBezeichnung()).setHeader("Belegungsart");
        // Belegung und Reinigungsstatus stehen hier bewusst nicht.
        // Dafür gibt es die eigene Seite "Zimmerstatus" (Gruppe 7), sonst stünde alles doppelt.

        // Alle Zeilen anzeigen, ohne eigenen Scrollbalken in der Tabelle.
        tabelle.setAllRowsVisible(true);

        // Ein Klick auf eine Zeile wählt das Zimmer aus (UC02, Schritt 3).
        // asSingleSelect: Es kann immer nur ein Zimmer gleichzeitig ausgewählt sein.
        // Ein zweiter Klick auf dieselbe Zeile hebt die Auswahl wieder auf.
        tabelle.asSingleSelect().addValueChangeListener(auswahl -> zimmerAuswaehlen(auswahl.getValue()));
    }

    // Erstellt den Kreis vorne in einer Zeile.
    // Ausgewähltes Zimmer: Kreis mit Punkt. Alle anderen: leerer Kreis.
    // Bekommt: das Zimmer der Zeile.
    // Gibt zurück: das passende Kreis-Symbol.
    private Icon erstelleAuswahlKreis(Zimmer zimmer) {
        // VaadinIcon: fertige Symbole, die bei Vaadin kostenlos dabei sind.
        if (zimmer == ausgewaehltesZimmer) {
            return VaadinIcon.DOT_CIRCLE.create();
        } else {
            return VaadinIcon.CIRCLE_THIN.create();
        }
    }

    // Merkt sich das ausgewählte Zimmer und schaltet die Buttons passend an oder aus.
    // Bekommt: das ausgewählte Zimmer oder null, wenn die Auswahl aufgehoben wurde.
    // Gibt nichts zurück.
    private void zimmerAuswaehlen(Zimmer zimmer) {
        ausgewaehltesZimmer = zimmer;

        // Bearbeiten und Löschen gehen nur, wenn ein Zimmer ausgewählt ist.
        boolean istEinZimmerAusgewaehlt = zimmer != null;
        bearbeiten.setEnabled(istEinZimmerAusgewaehlt);
        loeschen.setEnabled(istEinZimmerAusgewaehlt);

        // Die Tabelle neu zeichnen, damit die Kreise zur neuen Auswahl passen.
        tabelle.getDataProvider().refreshAll();
    }

    // Zeigt ein Fenster mit der Frage, ob das ausgewählte Zimmer wirklich gelöscht werden soll (UC34, Schritt 4).
    // Die Rückfrage verhindert, dass ein Zimmer durch einen Fehlklick verschwindet.
    // "Abbrechen" schließt nur die Rückfrage, das Zimmer bleibt unverändert (UC34, E2).
    // Bekommt: nichts. Gibt nichts zurück.
    private void zeigeLoeschRueckfrage() {
        Dialog rueckfrage = new Dialog();
        rueckfrage.setHeaderTitle("Zimmer löschen");
        rueckfrage.add(new Paragraph("Soll Zimmer " + ausgewaehltesZimmer.getZimmerNr() + " wirklich gelöscht werden?"));

        Button abbrechen = new Button("Abbrechen", klick -> rueckfrage.close());
        Button jaLoeschen = new Button("Löschen", klick -> {
            rueckfrage.close();
            zimmerLoeschen();
        });
        jaLoeschen.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        rueckfrage.getFooter().add(abbrechen, jaLoeschen);

        rueckfrage.open();
    }

    // Löscht das ausgewählte Zimmer über den ZimmerService und lädt die Tabelle neu (UC34, Schritte 5 bis 7).
    // Hat das Zimmer Buchungen, lehnt der Service das Löschen ab und hier erscheint die Meldung (UC34, E1).
    // Bekommt: nichts. Gibt nichts zurück.
    private void zimmerLoeschen() {
        int zimmerNr = ausgewaehltesZimmer.getZimmerNr();
        try {
            zimmerService.zimmerLoeschen(zimmerNr);
            Notification erfolg = Notification.show("Zimmer " + zimmerNr + " wurde gelöscht.");
            erfolg.addThemeVariants(NotificationVariant.SUCCESS);
            tabelleAktualisieren();
        } catch (IllegalArgumentException fehler) {
            Notification meldung = Notification.show(fehler.getMessage());
            meldung.addThemeVariants(NotificationVariant.ERROR);
        }
    }

    // Lädt alle Zimmer neu aus der Datenbank und zeigt sie in der Tabelle.
    // Wird beim Öffnen der Seite und nach jedem Anlegen, Ändern oder Löschen aufgerufen.
    // Danach ist kein Zimmer mehr ausgewählt.
    // Bekommt: nichts. Gibt nichts zurück.
    private void tabelleAktualisieren() {
        tabelle.setItems(zimmerService.alleZimmer());
        tabelle.deselectAll();
        zimmerAuswaehlen(null);
    }
}
