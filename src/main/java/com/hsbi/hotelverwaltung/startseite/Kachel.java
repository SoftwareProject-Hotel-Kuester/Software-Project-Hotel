package com.hsbi.hotelverwaltung.startseite;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Eine Kachel der Startseite: Titel, kurzer Text und ein Button, der zur Seite
 * führt. Gibt es die Seite noch nicht, ist die Kachel grau und der Button aus.
 *
 * Begriffe:
 * Ziel: die View-Klasse, zu der der Button führt. null heißt: Seite gibt es noch nicht.
 */
public class Kachel extends Div {

    public Kachel(String titel, String text, Class<? extends Component> ziel) {
        addClassNames("aura-surface", "kachel");

        Button knopf = new Button("Öffnen");
        knopf.addThemeVariants(ButtonVariant.PRIMARY);

        if (ziel == null) {
            addClassName("kachel-aus");
            knopf.setText("Noch nicht verfügbar");
            knopf.setEnabled(false);
        } else {
            knopf.addClickListener(klick -> UI.getCurrent().navigate(ziel));
        }

        add(new H3(titel), new Paragraph(text), knopf);
    }
}