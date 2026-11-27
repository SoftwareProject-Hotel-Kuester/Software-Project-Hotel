package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import com.hsbi.hotelverwaltung.layout.MitarbeiterLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Seite "Preise" der Hotelleitung. Gehört zu UC03 PflegePreise.
// Oben steht der Button "Neuen Preis festlegen", darunter die Preisliste (UC03, Schritt 2).
// Ein Klick auf eine Zeile wählt einen Eintrag aus (UC03, Schritt 3). Danach kann man
// für diese Zimmerklasse und Belegungsart einen neuen Preis festlegen.
//
// @Route: Die Seite ist unter http://localhost:8080/verwaltung/preise erreichbar.
//   layout = MitarbeiterLayout.class: Die Seite erscheint im gemeinsamen Rahmen mit Menü und Kopfzeile.
// @PageTitle: Text im Browser-Tab.
// @RolesAllowed("HOTELLEITUNG"): Nur die Hotelleitung darf Preise pflegen (primärer Nutzer in UC03).
// extends VerticalLayout: Die Bestandteile der Seite stehen untereinander.
@Route(value = "verwaltung/preise", layout = MitarbeiterLayout.class)
@PageTitle("Preise | Hotel Sparrenhof")
@RolesAllowed("HOTELLEITUNG")
public class PreisverwaltungView extends VerticalLayout {

    // Datumsformat für die Tabelle, z. B. "01.01.2027".
    private static final DateTimeFormatter DATUMSFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Geldformat für Deutschland, z. B. "120,00 €" (Komma statt Punkt, Euro-Zeichen hinten).
    private static final NumberFormat GELDFORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    // Grau für Hinweistexte, gleicher Farbton wie auf der Seite "Zimmerstatus".
    private static final String GRAU = "#5f6b7a";

    // Über den Service werden die Preise geladen und gespeichert (View → Service → Datenbank).
    private final PreisService preisService;

    // Grid = Tabelle. Jede Zeile ist ein Preis.
    private final Grid<Preis> tabelle = new Grid<>();

    // Button für den ausgewählten Eintrag. Er ist nur klickbar, wenn eine Zeile ausgewählt ist.
    private final Button neuerPreis = new Button("Neuen Preis festlegen");

    // Der Preis, der gerade ausgewählt ist. null heißt: Es ist keiner ausgewählt.
    private Preis ausgewaehlterPreis;

    // Baut die Seite auf. Wird jedes Mal aufgerufen, wenn jemand die Seite öffnet (UC03, Schritt 1).
    // Bekommt: den PreisService (Spring übergibt ihn automatisch). Gibt nichts zurück.
    public PreisverwaltungView(PreisService preisService) {
        this.preisService = preisService;

        // Schritt 1: Button vorbereiten. Am Anfang ist nichts ausgewählt, also ist er ausgegraut.
        // Der Klick öffnet das Fenster "Neuen Preis festlegen" für den gewählten Eintrag (UC03, Schritt 3).
        // Nach dem Speichern ruft das Fenster tabelleAktualisieren() auf, damit der neue Preis sofort erscheint.
        neuerPreis.addClickListener(klick ->
                new PreisAnlegenDialog(preisService, ausgewaehlterPreis, this::tabelleAktualisieren).open());
        neuerPreis.addThemeVariants(ButtonVariant.PRIMARY);
        neuerPreis.setEnabled(false);

        // Schritt 2: Tabelle mit Spalten aufbauen und mit allen Preisen füllen (UC03, Schritt 2).
        erstelleTabelle();
        tabelleAktualisieren();

        // Schritt 3: Kurzer Hinweis, wie die Seite benutzt wird. Grau, damit er nicht vom Inhalt ablenkt.
        Paragraph hinweis = new Paragraph("Nachtpreise je Zimmerklasse und Belegungsart. "
                + "Eine Zeile anklicken und dann \"Neuen Preis festlegen\" wählen. "
                + "Bestehende Buchungen behalten ihren Preis.");
        hinweis.getStyle().set("color", GRAU).set("margin-top", "0");

        // Schritt 4: Alles untereinander auf die Seite setzen.
        add(new H2("Preise"), hinweis, neuerPreis, tabelle);
    }

    // Legt die Spalten der Preisliste fest und reagiert auf die Auswahl einer Zeile (UC03, Schritte 2 und 3).
    // Bekommt: nichts. Gibt nichts zurück.
    private void erstelleTabelle() {
        // Erste Spalte: der Auswahlkreis, wie in der Zimmerverwaltung.
        // addComponentColumn zeigt in jeder Zelle ein Bauteil statt Text.
        tabelle.addComponentColumn(preis -> erstelleAuswahlKreis(preis))
                .setAutoWidth(true)
                .setFlexGrow(0);

        // Alle Spalten teilen sich die Seitenbreite gleichmäßig (setFlexGrow(1)).
        // ColumnTextAlign.CENTER: Inhalt und Überschrift stehen mittig, so wirkt alles einheitlich.

        // Die Klasse als farbiges Kennzeichen, damit man Economy, Standard und Premium sofort unterscheidet.
        tabelle.addComponentColumn(preis -> klassenKennzeichen(preis)).setHeader("Klasse")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);
        // Die Belegungsart ebenfalls farbig, mit eigenen Farben, damit man nichts verwechselt.
        tabelle.addComponentColumn(preis -> belegungsKennzeichen(preis)).setHeader("Belegungsart")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);
        // GELDFORMAT macht aus 120.00 den Text "120,00 €".
        tabelle.addColumn(preis -> GELDFORMAT.format(preis.getNachtpreis())).setHeader("Nachtpreis")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);
        tabelle.addColumn(preis -> preis.getGueltigAb().format(DATUMSFORMAT)).setHeader("Gültig ab")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);
        tabelle.addColumn(preis -> gueltigBisText(preis)).setHeader("Gültig bis")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);
        // Der Preis, der nach "Gültig bis" kommt. Grau, damit er nicht vom aktuellen Preis ablenkt.
        tabelle.addComponentColumn(preis -> folgePreisText(preis)).setHeader("Folgepreis")
                .setFlexGrow(1).setTextAlign(ColumnTextAlign.CENTER);

        // Alle Zeilen anzeigen, ohne eigenen Scrollbalken in der Tabelle.
        tabelle.setAllRowsVisible(true);
        // Die Tabelle nutzt die ganze Breite der Seite. Die Schrift bleibt in Standardgröße,
        // wie auf den anderen Seiten der Anwendung.
        tabelle.setWidthFull();
        // ROW_STRIPES: Jede zweite Zeile ist leicht grau hinterlegt, so verrutscht man nicht in der Zeile.
        tabelle.addThemeVariants(GridVariant.ROW_STRIPES);

        // Ein Klick auf eine Zeile wählt den Preis aus (UC03, Schritt 3).
        // asSingleSelect: Es kann immer nur ein Preis gleichzeitig ausgewählt sein.
        tabelle.asSingleSelect().addValueChangeListener(auswahl -> preisAuswaehlen(auswahl.getValue()));
    }

    // Erstellt den Kreis vorne in einer Zeile.
    // Ausgewählter Preis: Kreis mit Punkt. Alle anderen: leerer Kreis.
    // Bekommt: den Preis der Zeile.
    // Gibt zurück: das passende Kreis-Symbol.
    private Icon erstelleAuswahlKreis(Preis preis) {
        // VaadinIcon: fertige Symbole, die bei Vaadin kostenlos dabei sind.
        if (preis == ausgewaehlterPreis) {
            return VaadinIcon.DOT_CIRCLE.create();
        } else {
            return VaadinIcon.CIRCLE_THIN.create();
        }
    }

    // Erstellt das farbige Kennzeichen für die Zimmerklasse.
    // Economy grau, Standard blau, Premium gold (wie das Gold im Hotel-Logo).
    // Bekommt: den Preis der Zeile.
    // Gibt zurück: ein kleines farbiges Feld mit dem Namen der Klasse.
    private Span klassenKennzeichen(Preis preis) {
        Zimmerklasse klasse = preis.getZimmerklasse();
        String text = klasse.getBezeichnung();
        if (klasse == Zimmerklasse.ECONOMY) {
            return kennzeichen(text, "#3d4a5c", "#eef1f5");
        } else if (klasse == Zimmerklasse.STANDARD) {
            return kennzeichen(text, "#1e4fd8", "#e3ebff");
        } else {
            return kennzeichen(text, "#7a5a1e", "#f6eedd");
        }
    }

    // Erstellt das farbige Kennzeichen für die Belegungsart.
    // Einzelzimmer türkis, Doppelzimmer violett, Familienzimmer koralle.
    // Diese Farben kommen bei der Klasse nicht vor, damit man nichts verwechselt.
    // Bekommt: den Preis der Zeile.
    // Gibt zurück: ein kleines farbiges Feld mit dem Namen der Belegungsart.
    private Span belegungsKennzeichen(Preis preis) {
        Belegungsart art = preis.getBelegungsart();
        String text = art.getBezeichnung();
        if (art == Belegungsart.EINZEL) {
            return kennzeichen(text, "#0f6b6b", "#dcf2f1");
        } else if (art == Belegungsart.DOPPEL) {
            return kennzeichen(text, "#6a3fa0", "#efe7f8");
        } else {
            return kennzeichen(text, "#b0451f", "#fde8df");
        }
    }

    // Gibt den Text für die Spalte "Folgepreis" zurück, z. B. "130,00 € ab 01.01.2027".
    // Grau und klein, weil er nur eine Zusatzinfo ist und nicht im Vordergrund stehen soll.
    // Bekommt: den Preis der Zeile.
    // Gibt zurück: den Folgepreis als grauen Text, oder ein leeres Feld, wenn der Preis unbegrenzt gilt.
    private Span folgePreisText(Preis preis) {
        Preis folgePreis = preisService.folgePreis(preis);
        Span text;
        if (folgePreis == null) {
            // Kein Folgepreis: Die Zelle bleibt leer, damit die Tabelle ruhig wirkt.
            text = new Span("");
        } else {
            text = new Span(GELDFORMAT.format(folgePreis.getNachtpreis()) + " ab "
                    + folgePreis.getGueltigAb().format(DATUMSFORMAT));
        }
        text.getStyle().set("color", GRAU).set("font-size", "0.9em");
        return text;
    }

    // Baut ein kleines farbiges Kennzeichen mit runden Ecken.
    // Gleicher Stil wie die Status-Kennzeichen auf der Seite "Zimmerstatus" (Gruppe 7),
    // damit alle Seiten einheitlich aussehen.
    // Alle Kennzeichen sind gleich breit und der Text steht mittig.
    // So stehen sie in einer Spalte bündig untereinander, egal wie lang das Wort ist.
    // Bekommt: den Text, die Schriftfarbe und die Hintergrundfarbe.
    // Gibt zurück: das fertige Kennzeichen.
    private Span kennzeichen(String text, String farbe, String hintergrund) {
        // Span: ein kurzes Stück Text, das man gestalten kann.
        Span span = new Span(text);
        span.getStyle()
                .set("color", farbe)
                .set("background-color", hintergrund)
                .set("border-radius", "999px")
                .set("padding", "0.15em 0.6em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("white-space", "nowrap")
                .set("display", "inline-block")
                .set("min-width", "8.5em")
                .set("text-align", "center");
        return span;
    }

    // Gibt den Text für die Spalte "Gültig bis" zurück.
    // Bekommt: den Preis der Zeile.
    // Gibt zurück: das Enddatum, z. B. "31.12.2026", oder "unbegrenzt", wenn der Preis kein Ende hat.
    private String gueltigBisText(Preis preis) {
        // Ein leeres Enddatum heißt: Der Preis gilt, bis ein neuer Preis ihn beendet (UC03, E1).
        if (preis.getGueltigBis() == null) {
            return "unbegrenzt";
        } else {
            return preis.getGueltigBis().format(DATUMSFORMAT);
        }
    }

    // Merkt sich den ausgewählten Preis und schaltet den Button passend an oder aus.
    // Bekommt: den ausgewählten Preis oder null, wenn die Auswahl aufgehoben wurde.
    // Gibt nichts zurück.
    private void preisAuswaehlen(Preis preis) {
        ausgewaehlterPreis = preis;

        // Einen neuen Preis festlegen geht nur, wenn ein Eintrag ausgewählt ist (UC03, Schritt 3).
        neuerPreis.setEnabled(preis != null);

        // Die Tabelle neu zeichnen, damit die Kreise zur neuen Auswahl passen.
        tabelle.getDataProvider().refreshAll();
    }

    // Lädt alle Preise neu aus der Datenbank und zeigt sie in der Tabelle (UC03, Schritt 2).
    // Wird beim Öffnen der Seite und nach jedem neuen Preis aufgerufen.
    // Danach ist kein Preis mehr ausgewählt.
    // Bekommt: nichts. Gibt nichts zurück.
    private void tabelleAktualisieren() {
        tabelle.setItems(preisService.allePreise());
        tabelle.deselectAll();
        preisAuswaehlen(null);
    }
}
