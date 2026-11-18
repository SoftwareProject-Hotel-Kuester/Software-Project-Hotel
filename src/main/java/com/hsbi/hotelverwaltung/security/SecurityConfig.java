package com.hsbi.hotelverwaltung.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.hsbi.hotelverwaltung.security.LoginView;

/**
 * Sicherheitskonfiguration der Anwendung. Der VaadinSecurityConfigurer sorgt
 * dafür, dass Spring Security die internen Anfragen von Vaadin durchlässt.
 * Ohne ihn zeigt der Browser "Connection lost".
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                // Sagt Vaadin, wohin nicht angemeldete Personen umgeleitet werden.
                configurer.loginView(LoginView.class));
        return http.build();
    }

/**
   * Der DelegatingPasswordEncoder hasht neue Passwörter mit BCrypt und schreibt
   * den Algorithmus als Präfix davor, z. B. "{bcrypt}$2a$10$...".
   * Beim Prüfen liest er das Präfix und wählt den passenden Algorithmus.
   * So könnte man später auf ein anderes Verfahren wechseln, ohne dass alte
   * Passwörter ungültig werden.
   */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}