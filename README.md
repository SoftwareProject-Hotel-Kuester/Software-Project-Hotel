# Hotelverwaltung Sparrenhof

## Stand

Das Projektgerüst steht. Es gibt noch keine Fachfunktionen, die App startet aber und zeigt eine Testseite mit der Überschrift "Sparrenhof läuft". Ab jetzt arbeitet jeder in seinem eigenen Paket an seinen Use Cases.

Die Sicherheitskonfiguration (`SecurityConfig`) ist angelegt, die eigentliche Anmeldung folgt mit dem Login.

## Einmalig einrichten

1. Git und JDK 25 installieren.
2. Docker Desktop installieren und starten. Unter Windows braucht er WSL2, die Installation fragt danach.
3. Den aktuellen Stand holen. Wer das Repo schon geklont hat, führt im Projektordner aus:

```
git pull
```

Solange das Gerüst noch nicht auf main liegt, vorher auf den Branch wechseln:

```
git fetch
git switch projekt-geruest
```

## App starten

Docker Desktop muss laufen. Im Projektordner:

```
.\mvnw spring-boot:run
```

Beim ersten Start dauert es länger, weil Docker die Images lädt und Vaadin seine Oberfläche baut. Danach öffnet sich der Browser von selbst und zeigt die Testseite. Wenn nicht, ist die App unter http://localhost:8080 erreichbar.

Datenbank und Test-Mailserver startet Spring Boot automatisch (siehe `compose.yaml`). Docker-Befehle musst du nicht eintippen.

Jeder hat seine eigene lokale Datenbank. Testdaten werden deshalb im Code angelegt, damit alle dasselbe sehen.

## E-Mails ansehen

Die App verschickt keine echten Mails. Alles landet in Mailpit: http://localhost:8025

## Zusammenarbeit

- Nichts direkt auf main pushen. Pro Use Case einen Branch anlegen, zum Beispiel `feature/login-registrierung`, und per Pull Request mergen.
- Vor dem Arbeiten `git pull` auf main, damit der Branch auf dem aktuellen Stand beginnt.
- Klein und oft committen und mergen. Lange Branches machen später Konflikte.

## Ordner

- `src`: Quellcode der Anwendung
- `UML-Diagramme`: Sequenz- und Klassendiagramme als PlantUML-Dateien