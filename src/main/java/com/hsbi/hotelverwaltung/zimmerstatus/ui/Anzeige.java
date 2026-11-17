package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.domain.Reinigungsstatus;
import com.hsbi.hotelverwaltung.domain.Zimmerstatus;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerInfo;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.time.format.DateTimeFormatter;

/**
 * Kleine Bausteine, die mehrere Ansichten nutzen: farbige Status-Kennzeichen,
 * Sperrgrund-Kasten und Benachrichtigungen. Die Farben stehen direkt im Code,
 * damit sie mit jedem Vaadin-Theme gleich aussehen.
 */
final class Anzeige {

    static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final DateTimeFormatter ZEITPUNKT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    static final String GRAU = "#5f6b7a";

    private Anzeige() {
    }

    /** Kennzeichen für die Belegung: frei, belegt, gesperrt. */
    static Span status(Zimmerstatus status) {
        return switch (status) {
            case BELEGT -> kennzeichen(status.getBezeichnung(), "#1e4fd8", "#e3ebff");
            case NICHT_BELEGT -> kennzeichen(status.getBezeichnung(), "#3d4a5c", "#eef1f5");
            case GESPERRT -> kennzeichen(status.getBezeichnung(), "#b3261e", "#fde7e7");
        };
    }

    /** Kennzeichen für die Reinigung: gereinigt, zu reinigen, nicht reinigen. */
    static Span status(Reinigungsstatus status) {
        return switch (status) {
            case GEREINIGT -> kennzeichen(status.getBezeichnung(), "#1b7a3a", "#e3f6e9");
            case ZU_REINIGEN -> kennzeichen(status.getBezeichnung(), "#a35200", "#fff1e0");
            case NICHT_REINIGEN -> kennzeichen(status.getBezeichnung(), "#5b4a8a", "#eeeaf8");
        };
    }

    /** Kasten mit Sperrgrund und Herkunft, wird beim Öffnen eines gesperrten Zimmers gezeigt (UC26). */
    static Div sperrgrund(ZimmerInfo zimmer) {
        Span titel = new Span("Sperrgrund");
        titel.getStyle().set("font-weight", "600");

        Div grund = new Div(zimmer.sperrgrund() == null ? "Kein Sperrgrund hinterlegt." : zimmer.sperrgrund());
        grund.getStyle().set("white-space", "pre-wrap");

        Div kasten = new Div(titel, grund);
        if (zimmer.gemeldetAm() != null && zimmer.gemeldetVon() != null) {
            Span herkunft = new Span("gemeldet am " + zimmer.gemeldetAm().format(DATUM)
                    + " von: " + zimmer.gemeldetVon().getBezeichnung());
            herkunft.getStyle().set("font-size", "0.85em").set("color", GRAU);
            kasten.add(herkunft);
        }
        kasten.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.25rem")
                .set("background-color", "#fde7e7")
                .set("border-left", "4px solid #b3261e")
                .set("border-radius", "4px")
                .set("padding", "0.5rem 0.75rem")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return kasten;
    }

    /** Grüne Erfolgsmeldung unten links. */
    static void erfolg(String text) {
        Notification notification = Notification.show(text, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.SUCCESS);
    }

    /** Rote Fehlermeldung in der Mitte, bleibt etwas länger stehen. */
    static void fehler(String text) {
        Notification notification = Notification.show(text, 6000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.ERROR);
    }

    private static Span kennzeichen(String text, String farbe, String hintergrund) {
        Span span = new Span(text);
        span.getStyle()
                .set("color", farbe)
                .set("background-color", hintergrund)
                .set("border-radius", "999px")
                .set("padding", "0.1em 0.6em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("white-space", "nowrap");
        return span;
    }
}
