package com.hsbi.hotelverwaltung;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@AnonymousAllowed
public class TestView extends H1 {

    public TestView() {
        super("Sparrenhof läuft");
    }
}