package com.hsbi.hotelverwaltung;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startklasse der Anwendung. Sie ist zugleich die Grundkonfiguration der Seite,
 * hier wird festgelegt, welche Stylesheets überall geladen werden.
 *
 * Begriffe:
 * AppShellConfigurator: Marker für die Klasse, die die Seitenhülle konfiguriert.
 * StyleSheet: lädt eine CSS-Datei beim Start der Seite, für alle Views.
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)   // zuerst das Theme
@StyleSheet("styles.css")      // dann unsere Anpassungen
public class HotelverwaltungApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(HotelverwaltungApplication.class, args);
	}

}
