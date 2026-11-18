package com.hsbi.hotelverwaltung;

import com.hsbi.hotelverwaltung.gaesteportal.GaesteStartView;
import com.hsbi.hotelverwaltung.startseite.MitarbeiterStartView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Einstiegsadresse "/". Zeigt selbst nichts an, sondern leitet je nach Rolle
 * zur passenden Startseite weiter. Hierher schickt Spring Security nach dem Login.
 *
 * Begriffe:
 * PermitAll: Jede angemeldete Person darf rein, anonyme werden zum Login geschickt.
 * BeforeEnterObserver: Vaadin ruft beforeEnter auf, bevor die Seite angezeigt wird.
 * forwardTo: Leitet zu einer anderen Seite weiter, ohne dass diese hier erscheint.
 */
@Route("")
@PermitAll
public class HomeView extends Div implements BeforeEnterObserver {

    // transient, weil AuthenticationContext nicht serialisierbar ist (wie in Kopfzeile).
    private final transient AuthenticationContext authContext;

    public HomeView(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authContext.hasAnyRole("MITARBEITER", "HOTELLEITUNG")) {
            event.forwardTo(MitarbeiterStartView.class);
        } else {
            event.forwardTo(GaesteStartView.class);
        }
    }
}