---
description: Sitzungsstart – GitHub-Stand prüfen und Überblick geben
---
Mach zum Start einen kurzen Überblick für mich:

1. Führe `git fetch` aus und zeig, auf welchem Branch ich bin und ob es lokale, nicht committete Änderungen gibt (`git status`).
2. Zeig, welche neuen Commits es auf `origin/main` gibt, die ich lokal noch nicht habe (`git log --oneline HEAD..origin/main`), und welche Dateien sich dadurch geändert haben (`git diff --stat HEAD...origin/main`).
3. Fasse in 3–5 Stichpunkten auf Deutsch zusammen, was sich geändert hat. Sag besonders, ob sich etwas an `Zimmer`, `Preis`, `Kostenposten`, `Kostensatz`, den Diagrammen in `UML-Diagramme/` oder der Doku in `docs/` geändert hat, weil das meine Use Cases betrifft.
4. Frag mich, ob ich die Änderungen mit `git pull` holen will. Führe pull nicht selbst aus, ohne zu fragen.
5. Nenne zum Schluss den nächsten sinnvollen Schritt für meine Use Cases aus CLAUDE.local.md.
