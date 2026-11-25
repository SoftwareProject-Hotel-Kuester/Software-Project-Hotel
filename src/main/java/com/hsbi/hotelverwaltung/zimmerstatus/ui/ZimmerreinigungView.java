package com.hsbi.hotelverwaltung.zimmerstatus.ui;

import com.hsbi.hotelverwaltung.domain.Gast;
import com.hsbi.hotelverwaltung.gaesteportal.AktuellerGast;
import com.hsbi.hotelverwaltung.gaesteportal.GaesteportalLayout;
import com.hsbi.hotelverwaltung.gaesteportal.MeineBuchungenView;
import com.hsbi.hotelverwaltung.zimmerstatus.Defektmeldung;
import com.hsbi.hotelverwaltung.zimmerstatus.GastZimmerService;
import com.hsbi.hotelverwaltung.zimmerstatus.GastZimmerService.Aufenthalt;
import com.hsbi.hotelverwaltung.zimmerstatus.ZimmerstatusException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * UC07 PflegeZimmerStatus im Gästeportal: Der Gast legt für seinen laufenden
 * Aufenthalt fest, ob sein Zimmer gereinigt werden soll (E1), und kann einen
 * Defekt melden (E2).
 */
@Route(value = "zimmerreinigung", layout = GaesteportalLayout.class)
@PageTitle("Zimmerreinigung | Hotel Sparrenhof")
@RolesAllowed("GAST")
public class ZimmerreinigungView extends VerticalLayout {

    private static final String REINIGEN = "Bitte täglich reinigen";
    private static final String NICHT_REINIGEN = "Nicht reinigen";

    private final GastZimmerService gastZimmerService;
    private final Optional<Gast> gast;
    private final VerticalLayout inhalt = new VerticalLayout();

    public ZimmerreinigungView(AktuellerGast aktuellerGast, GastZimmerService gastZimmerService) {
        this.gastZimmerService = gastZimmerService;
        this.gast = aktuellerGast.get();

        inhalt.setPadding(false);
        add(new H2("Zimmerreinigung"), inhalt);
        aktualisieren();
    }

    private void aktualisieren() {
        inhalt.removeAll();
        if (gast.isEmpty()) {
            inhalt.add(new Paragraph("Bitte melden Sie sich als Gast an."));
            return;
        }

        List<Aufenthalt> aufenthalte = gastZimmerService.laufendeAufenthalte(gast.get(), LocalDate.now());
        if (aufenthalte.isEmpty()) {
            inhalt.add(new Paragraph("Sie haben zurzeit keinen laufenden Aufenthalt. "
                            + "Die Zimmerreinigung können Sie während Ihres Aufenthalts festlegen."),
                    new RouterLink("Zu meinen Buchungen", MeineBuchungenView.class));
            return;
        }

        Paragraph info = new Paragraph("Ihr Zimmer wird täglich gereinigt. Wenn Sie keine Reinigung "
                + "wünschen, wählen Sie bis 11 Uhr „Nicht reinigen“. Zwischen 11 und 12 Uhr "
                + "wird gereinigt, dann ist keine Änderung möglich.");
        info.getStyle().set("color", Anzeige.GRAU);
        inhalt.add(info);
        aufenthalte.forEach(a -> inhalt.add(karte(a)));
    }

    private Div karte(Aufenthalt aufenthalt) {
        H3 titel = new H3("Zimmer " + aufenthalt.zimmer());
        Span zeitraum = new Span("Aufenthalt " + aufenthalt.anreise().format(Anzeige.DATUM)
                + " – " + aufenthalt.abreise().format(Anzeige.DATUM));
        zeitraum.getStyle().set("color", Anzeige.GRAU);

        Div karte = new Div(titel, zeitraum);
        karte.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.5rem")
                .set("border", "1px solid #d5dbe3")
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("width", "min(36rem, 100%)")
                .set("box-sizing", "border-box");

        if (aufenthalt.gesperrt()) {
            Div gesperrt = new Div(new Span("Ihr Zimmer ist wegen eines Defekts gesperrt: "
                    + aufenthalt.sperrgrund() + ". Die Rezeption meldet sich bei Ihnen."));
            gesperrt.getStyle()
                    .set("background-color", "#fde7e7")
                    .set("border-left", "4px solid #b3261e")
                    .set("border-radius", "4px")
                    .set("padding", "0.5rem 0.75rem");
            karte.add(gesperrt);
        } else {
            karte.add(reinigungswahl(aufenthalt));
        }

        Button defekt = new Button("Defekt melden", klick -> defektDialog(aufenthalt));
        defekt.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        karte.add(defekt);
        return karte;
    }

    /** E1: Reinigung wählen. Während der Reinigungszeit nur Anzeige. */
    private VerticalLayout reinigungswahl(Aufenthalt aufenthalt) {
        RadioButtonGroup<String> wahl = new RadioButtonGroup<>("Reinigung");
        wahl.setItems(REINIGEN, NICHT_REINIGEN);
        wahl.setValue(aufenthalt.nichtReinigen() ? NICHT_REINIGEN : REINIGEN);

        Span status = new Span("Aktueller Status: ");
        status.add(Anzeige.status(aufenthalt.reinigungsstatus()));

        VerticalLayout bereich = new VerticalLayout(wahl, status);
        bereich.setPadding(false);
        bereich.setSpacing(false);

        if (GastZimmerService.istReinigungszeit(LocalTime.now())) {
            wahl.setEnabled(false);
            wahl.setHelperText("Zwischen 11 und 12 Uhr wird gereinigt. Ab 12 Uhr können Sie wieder wählen.");
            return bereich;
        }

        wahl.addValueChangeListener(event -> {
            if (!event.isFromClient()) {
                return;
            }
            try {
                boolean nicht = NICHT_REINIGEN.equals(event.getValue());
                gastZimmerService.reinigungWaehlen(gast.orElseThrow(), aufenthalt.buchungsNr(), nicht,
                        LocalDateTime.now());
                Anzeige.erfolg(nicht
                        ? "Ihr Zimmer wird bei der nächsten Reinigung ausgelassen."
                        : "Ihr Zimmer wird wieder täglich gereinigt.");
            } catch (ZimmerstatusException e) {
                Anzeige.fehler(e.getMessage());
            }
            aktualisieren();
        });
        return bereich;
    }

    /** E2: Defekt mit Vermerk melden, danach ist das Zimmer gesperrt. */
    private void defektDialog(Aufenthalt aufenthalt) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Defekt melden – Zimmer " + aufenthalt.zimmerNr());
        dialog.setWidth("min(30rem, 95vw)");

        TextArea beschreibung = new TextArea("Was ist defekt?");
        beschreibung.setPlaceholder("z. B. Die Dusche wird nicht warm.");
        beschreibung.setRequiredIndicatorVisible(true);
        beschreibung.setErrorMessage("Bitte beschreiben Sie, was defekt ist.");
        beschreibung.setMaxLength(Defektmeldung.MAX_LAENGE);
        beschreibung.setWidthFull();
        beschreibung.setMinHeight("7rem");

        Paragraph hinweis = new Paragraph("Ihr Zimmer wird danach gesperrt und die Rezeption kümmert sich darum, "
                + "z. B. mit einem Ersatzzimmer.");
        hinweis.getStyle().set("color", Anzeige.GRAU).set("font-size", "0.9em");
        dialog.add(beschreibung, hinweis);

        Button abbrechen = new Button("Abbrechen", klick -> dialog.close());
        abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
        Button senden = new Button("Defekt melden", klick -> {
            if (beschreibung.getValue().isBlank()) {
                beschreibung.setInvalid(true);
                return;
            }
            try {
                gastZimmerService.defektMelden(gast.orElseThrow(), aufenthalt.buchungsNr(),
                        beschreibung.getValue(), LocalDate.now());
                Anzeige.erfolg("Vielen Dank, Ihre Meldung ist bei der Rezeption angekommen.");
                dialog.close();
                aktualisieren();
            } catch (ZimmerstatusException e) {
                Anzeige.fehler(e.getMessage());
            }
        });
        senden.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        dialog.getFooter().add(abbrechen, senden);
        dialog.open();
    }
}
