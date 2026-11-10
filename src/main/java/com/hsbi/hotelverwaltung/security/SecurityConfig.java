package com.hsbi.hotelverwaltung.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            // Hier kommt später die eigene Login-Seite hinein:
            // configurer.loginView(LoginView.class);
        });
        return http.build();
    }
}