# Projekt: Hotelverwaltung „Sparrenhof“ (Softwareprojekt HSBI, Gruppe 5, WS 26/27)

## Ziel
Webanwendung zur Verwaltung des Hotels Sparrenhof mit Mitarbeiterbereich und Gästeportal.
7 Studierende, jede Person programmiert ihre Use-Case-Gruppe (ca. 1000 Zeilen pro Person).

## Wo die Infos liegen (Quelle der Wahrheit)
- `Use-Cases/` → Projektdokumentation als PDF: Anforderungen, Use Cases UC01–UC33, User Stories, Domänen-Klassendiagramm.
- `UML-Diagramme/` → Klassendiagramm, Sequenzdiagramme (PlantUML), ER-Modell (draw.io), Relationenmodell (PlantUML).
- Bei Fragen zu Abläufen, Feldern oder Fehlerfällen zuerst im passenden Use Case nachlesen und nichts erfinden.
- Widersprechen sich Doku, Diagramme und Code, nicht selbst entscheiden, sondern den Widerspruch melden.

## Tech-Stack (fest, nicht wechseln)
- Java (JDK 25), Spring Boot, Maven (Maven Wrapper `mvnw`)
- Oberfläche: Vaadin (Oberfläche komplett in Java, kein HTML, kein Thymeleaf)
- Datenbank: PostgreSQL in Docker (`compose.yaml`), Zugriff über Spring Data JPA
- Login und Rollen: Spring Security (Paket `security`)
- Diagramme: PlantUML und draw.io
- Nur kostenlose Bibliotheken. Keine Vaadin-Pro-Komponenten (z. B. kein Vaadin Charts).
- Start: Docker Desktop starten, dann `HotelverwaltungApplication` ausführen → http://localhost:8080/
- Test-Mails (z. B. Bestätigungslinks) landen in Mailpit: http://localhost:8025. Die App verschickt keine echten Mails.
- Jeder hat seine eigene lokale Datenbank. Testdaten werden deshalb im Code angelegt, damit alle dasselbe sehen.

## Paketstruktur
- Basispaket: `com.hsbi.hotelverwaltung`
- Darunter pro Fachbereich ein Paket (z. B. `zimmer`, `buchung`, `gast`), darin Entity, Repository, Service und View.

## Wichtige fachliche Festlegungen
- Rollen: Gast, Mitarbeiter, Hotelleitung. Hotelleitung ist keine eigene Klasse, sondern die Rolle HOTELLEITUNG eines Mitarbeiters.
- Benutzer (abstrakt) → Gast und Mitarbeiter erben davon.
- Buchungsstatus: GEBUCHT, EINGECHECKT, AUSGECHECKT, STORNIERT. Ob ein Zimmer belegt ist, ergibt sich aus einer Buchung mit Status EINGECHECKT (`istBelegt()`).
- Zimmerstatus: GEREINIGT, ZU_REINIGEN, NICHT_REINIGEN, GESPERRT.
- Preise gelten je Zimmerklasse und Belegungsart für einen Zeitraum (gültigAb, gültigBis). Buchungen speichern ihren eigenen Gesamtpreis.
- Tägliche Reinigung wird automatisch mit `@Scheduled` angestoßen. Gesperrte Zimmer sind ausgenommen.

## Code-Regeln
- Geldbeträge immer `BigDecimal`, Datum immer `LocalDate`.
- Enums mit `@Enumerated(EnumType.STRING)`.
- Namen von Klassen, Attributen und Methoden wie im Domänen-Klassendiagramm.
- Status nur über Fachmethoden ändern (z. B. `sperren()`, `einchecken()`), keine freien Setter dafür.

## Einfacher Code (sehr wichtig)
Wir sind Studierende und müssen jede Zeile in der Prüfung selbst erklären können. Code muss deshalb so geschrieben sein, dass ihn auch jemand mit wenig Programmiererfahrung versteht.
- Einfache Mittel bevorzugen: `if`/`else`, normale `for`-Schleifen, klar benannte Variablen.
- Keine komplizierten Konstrukte, wenn es einfach geht: keine verschachtelten Streams, keine langen Lambda-Ketten, keine Reflection, keine eigenen Generics, keine unnötigen Design-Patterns oder Abstraktionsschichten.
- Kurze Methoden mit einer Aufgabe. Lieber zwei einfache Methoden als eine schlaue.
- Sprechende Namen auf Deutsch wie im Klassendiagramm (z. B. `zimmerNummerIstVergeben`, nicht `chk`).
- Neue Bibliotheken oder Techniken nur nach Rückfrage einführen.
- Wenn eine Spring- oder Vaadin-Annotation zum ersten Mal vorkommt (z. B. `@Entity`, `@Service`, `@Route`), im Kommentar kurz erklären, was sie bewirkt.

## Kommentare (sehr wichtig für die Prüfung)
- Alle Kommentare auf Deutsch, in einfachen, ganzen Sätzen. Keine Fachbegriffe ohne Erklärung.
- Über jeder Klasse: wofür die Klasse da ist und zu welchen Use Cases sie gehört (z. B. `// Gehört zu UC01 LegeZimmerAn`).
- Über jeder Methode: was sie macht, was sie bekommt und was sie zurückgibt.
- In längeren Methoden die einzelnen Schritte kommentieren (z. B. `// Schritt 1: Prüfen, ob die Zimmernummer schon existiert`). Die Schritte sollen sich an den Standardablauf im Use Case anlehnen.
- Bei jeder Fachregel erklären, warum sie da ist (z. B. „Zimmernummer darf nur einmal vorkommen, sonst wären Buchungen nicht eindeutig“).
- Ziel: Jemand, der den Code nicht geschrieben hat, versteht beim Lesen der Kommentare, was passiert.

## Team und Use Cases
1. Zimmer, Preise & Kosten (Simon): UC01, UC02, UC03, UC05, UC33
2. Verfügbarkeit & Auswertung (Lukas): UC04, UC06, UC16, UC31, UC32
3. Buchung durch Mitarbeiter (Fynn): UC08, UC09, UC10
4. Gäste & Rezeption (Ben): UC11, UC12, UC13, UC22, UC23
5. Gästeportal (Giuliano): UC17, UC18, UC19, UC20, UC21
6. Benutzer, Login & Rollen (Philipp): UC14, UC15, UC28, UC29, UC30
7. Zimmerstatus & Reinigung (Leonard): UC07, UC24, UC25, UC26, UC27
(Vorläufige Verteilung, kann sich noch ändern.)

## Git-Ablauf
- Nie direkt auf `main` arbeiten. Pro Use Case ein Branch nach dem Schema `feature/…`, z. B. `feature/uc01-lege-zimmer-an`.
- Commit-Nachrichten mit Use-Case-Bezug, z. B. „UC01: Eingabemaske Zimmer anlegen“.
- Zusammenführen nur per Pull Request auf GitHub. Vor dem Arbeiten `git pull` auf `main`.
- Klein und oft committen und mergen, lange Branches machen später Konflikte.
- Ändert sich das Datenmodell, im selben Pull Request auch die Diagramme in `UML-Diagramme/` anpassen.
- Nichts committen oder pushen, ohne vorher zu fragen.
