package com.hsbi.hotelverwaltung.preis;

import com.hsbi.hotelverwaltung.domain.Belegungsart;
import com.hsbi.hotelverwaltung.domain.Zimmer;
import com.hsbi.hotelverwaltung.domain.Zimmerklasse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Fachregeln rund um Preise. Gehört zu UC03 PflegePreise und UC05 BerechnePreis.
// Ablauf: View → PreisService → PreisRepository → Datenbank.
//
// @Service: Spring erzeugt beim Start genau ein Objekt dieser Klasse
// und gibt es jeder Klasse, die es im Konstruktor verlangt.
@Service
public class PreisService {

    // Über das Repository lesen und speichern wir Preise in der Datenbank.
    private final PreisRepository preisRepository;

    // Konstruktor: Spring übergibt das Repository automatisch.
    // Bekommt: das PreisRepository. Gibt nichts zurück.
    public PreisService(PreisRepository preisRepository) {
        this.preisRepository = preisRepository;
    }

    // Sucht den Preis, der an einem bestimmten Tag für Zimmerklasse und Belegungsart gilt.
    // Gebraucht für UC03, Schritt 4 (welcher Preis gilt am Start des neuen Preises?)
    // und für UC05, Schritt 2 (welcher Preis gilt in dieser Nacht?).
    // Bekommt: Zimmerklasse, Belegungsart und das Datum.
    // Gibt zurück: den gültigen Preis oder null, wenn an diesem Tag kein Preis gilt.
    public Preis preisAmTag(Zimmerklasse zimmerklasse, Belegungsart belegungsart, LocalDate datum) {
        // Schritt 1: Alle Preise dieser Kombination aus der Datenbank holen.
        List<Preis> preise = preisRepository.findByZimmerklasseAndBelegungsartOrderByGueltigAbAsc(
                zimmerklasse, belegungsart);

        // Schritt 2: Jeden Preis prüfen. Der erste, der an diesem Tag gilt, ist der richtige.
        // Weil sich Preise nie überschneiden (dafür sorgt UC03), gibt es höchstens einen.
        for (Preis preis : preise) {
            if (preis.giltAm(datum)) {
                return preis;
            }
        }

        // Schritt 3: Kein Preis gefunden, z. B. weil die Hotelleitung noch keinen gepflegt hat.
        return null;
    }

    // Legt einen neuen Preis an (UC03, Schritte 4 bis 7).
    // Gilt am Startdatum schon ein alter Preis, wird dieser am Vortag beendet (UC03, E1 Schritt 7).
    // Die Rückfrage aus E1 ("alten Preis beenden?") stellt die Seite vorher selbst.
    // Bekommt: Zimmerklasse, Belegungsart, Nachtpreis und den ersten gültigen Tag.
    // Gibt zurück: den gespeicherten neuen Preis.
    // Wirft eine IllegalArgumentException bei ungültigen Eingaben oder wenn schon ein Preis
    // am selben Tag oder später beginnt.
    //
    // @Transactional: Die Methode speichert zweimal (alten Preis beenden, neuen Preis speichern).
    // Die Annotation sorgt dafür, dass entweder beide Änderungen gespeichert werden oder keine.
    // Geht unterwegs etwas schief, wird alles zurückgenommen und der alte Preis bleibt wie er war.
    @Transactional
    public Preis preisAnlegen(Zimmerklasse zimmerklasse, Belegungsart belegungsart,
                              BigDecimal nachtpreis, LocalDate gueltigAb) {
        // Schritt 1: Eingaben prüfen (Pflichtfelder, Preis größer als 0).
        pruefeEingaben(zimmerklasse, belegungsart, nachtpreis, gueltigAb);

        // Schritt 2: Beginnt schon ein Preis am selben Tag oder später? Dann wird abgelehnt.
        pruefeNeuenStart(zimmerklasse, belegungsart, gueltigAb);

        // Schritt 3: Gilt am Startdatum noch ein alter Preis (UC03, Schritt 4)?
        // Dann endet er am Vortag, damit an jedem Tag genau ein Preis gilt (UC03, E1 Schritt 7).
        Preis alterPreis = preisAmTag(zimmerklasse, belegungsart, gueltigAb);
        if (alterPreis != null) {
            alterPreis.beenden(gueltigAb.minusDays(1));
            preisRepository.save(alterPreis);
        }

        // Schritt 4: Den neuen Preis speichern und zurückgeben (UC03, Schritt 5).
        // Bestehende Buchungen ändern sich dadurch nicht, weil jede Buchung ihren Preis selbst speichert.
        Preis neuerPreis = new Preis(zimmerklasse, belegungsart, nachtpreis, gueltigAb);
        return preisRepository.save(neuerPreis);
    }

    // Prüft, ob ein neuer Preis an diesem Tag beginnen darf (UC03, Schritt 4).
    // Regel: Beginnt schon ein Preis am selben Tag oder später, wird abgelehnt.
    // Grund: Den bestehenden Preis könnte man nicht am Vortag beenden, weil er dann
    // vor seinem eigenen Start enden würde. Es würden sonst zwei Preise am selben Tag gelten.
    // Die Seite ruft die Methode schon beim Klick auf "Bestätigen" auf, damit der Fehler
    // sofort kommt und nicht erst nach der Rückfrage.
    // Bekommt: Zimmerklasse, Belegungsart und den gewünschten Starttag.
    // Gibt nichts zurück. Wirft eine IllegalArgumentException, wenn der Starttag nicht erlaubt ist.
    public void pruefeNeuenStart(Zimmerklasse zimmerklasse, Belegungsart belegungsart, LocalDate gueltigAb) {
        List<Preis> preise = preisRepository.findByZimmerklasseAndBelegungsartOrderByGueltigAbAsc(
                zimmerklasse, belegungsart);

        // Jeden bestehenden Preis prüfen: Beginnt er nicht vor dem neuen Starttag, gibt es einen Konflikt.
        for (Preis preis : preise) {
            if (!preis.getGueltigAb().isBefore(gueltigAb)) {
                throw new IllegalArgumentException("Es gibt schon einen Preis ab "
                        + preis.getGueltigAb().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        + ". Der neue Preis muss nach diesem Tag beginnen.");
            }
        }
    }

    // Berechnet den Gesamtpreis eines Aufenthalts (UC05 BerechnePreis).
    // Jede Nacht wird mit dem Preis berechnet, der an diesem Tag gilt (UC05, E1).
    // So stimmt der Preis auch, wenn sich der Nachtpreis mitten im Aufenthalt ändert.
    // Wird von anderen Gruppen aufgerufen: Buchung durch Mitarbeiter (UC08, UC09),
    // Zimmersuche und Buchung durch Gäste (UC16, UC17).
    // Bekommt: das Zimmer, den Anreisetag und den Abreisetag.
    // Gibt zurück: den Gesamtpreis aller Nächte.
    // Wirft eine IllegalArgumentException, wenn die Abreise nicht nach der Anreise liegt
    // oder wenn für eine Nacht kein Preis hinterlegt ist (UC05, E2).
    public BigDecimal berechnePreis(Zimmer zimmer, LocalDate anreise, LocalDate abreise) {
        // Schritt 1: Vorbedingung 2 prüfen. Ohne mindestens eine Nacht gibt es nichts zu berechnen.
        if (anreise == null || abreise == null || !abreise.isAfter(anreise)) {
            throw new IllegalArgumentException("Die Abreise muss nach der Anreise liegen.");
        }

        // Schritt 2: Jede Nacht einzeln durchgehen (UC05, Schritt 2).
        // Eine Nacht wird nach dem Anreisetag benannt: Anreise 05.01., Abreise 07.01. = Nächte 05.01. und 06.01.
        // Der Abreisetag selbst ist keine Nacht mehr, deshalb läuft die Schleife nur bis zum Vortag.
        BigDecimal gesamtpreis = BigDecimal.ZERO;
        LocalDate nacht = anreise;
        while (nacht.isBefore(abreise)) {
            // Den Preis holen, der in dieser Nacht für Klasse und Belegungsart des Zimmers gilt.
            Preis preis = preisAmTag(zimmer.getZimmerklasse(), zimmer.getBelegungsart(), nacht);

            // UC05, E2: Kein Preis hinterlegt → Fehler. Die Buchung darf dann nicht stattfinden,
            // die Hotelleitung muss den Preis erst pflegen (UC03).
            if (preis == null) {
                throw new IllegalArgumentException("Für " + zimmer.getZimmerklasse().getBezeichnung() + " / "
                        + zimmer.getBelegungsart().getBezeichnung() + " ist am "
                        + nacht.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        + " kein Preis hinterlegt. Bitte die Hotelleitung informieren.");
            }

            // Den Nachtpreis zur Summe addieren (UC05, Schritt 3).
            // BigDecimal ist unveränderlich: add gibt eine neue Zahl zurück, deshalb die Zuweisung.
            gesamtpreis = gesamtpreis.add(preis.getNachtpreis());

            // Zur nächsten Nacht weitergehen.
            nacht = nacht.plusDays(1);
        }

        // Schritt 3: Gesamtpreis zurückgeben. Gespeichert wird er erst mit der Buchung (Nachbedingung UC05).
        return gesamtpreis;
    }

    // Sucht den Preis, der direkt nach einem Preis kommt (für die Spalte "Folgepreis" in der Preisliste).
    // Ein Preis hat nur dann ein Enddatum, wenn ein neuer Preis eingetragen wurde (UC03, E1).
    // Der Folgepreis beginnt deshalb genau einen Tag nach diesem Enddatum.
    // Bekommt: den Preis, dessen Nachfolger gesucht wird.
    // Gibt zurück: den Folgepreis oder null, wenn der Preis unbegrenzt gilt (also keinen Nachfolger hat).
    public Preis folgePreis(Preis preis) {
        // Ohne Enddatum gibt es keinen Nachfolger.
        if (preis.getGueltigBis() == null) {
            return null;
        }
        // Der Nachfolger ist der Preis, der am Tag nach dem Ende gilt.
        LocalDate ersterTagDanach = preis.getGueltigBis().plusDays(1);
        return preisAmTag(preis.getZimmerklasse(), preis.getBelegungsart(), ersterTagDanach);
    }

    // Lädt alle Preise für die Preisliste der Hotelleitung (UC03, Schritt 2).
    // Bekommt: nichts.
    // Gibt zurück: alle Preise, sortiert vom einfachsten zum luxuriösesten Zimmer
    // (Economy, Standard, Premium), darin Einzel-, Doppel-, Familienzimmer und dann nach Startdatum.
    // Die Liste ist leer, wenn es noch keine Preise gibt.
    public List<Preis> allePreise() {
        List<Preis> allePreise = new ArrayList<>();

        // values() liefert die Enum-Werte in der Reihenfolge, in der sie im Enum stehen:
        // ECONOMY, STANDARD, PREMIUM und EINZEL, DOPPEL, FAMILIE.
        // Zwei Schleifen gehen jede Kombination in dieser Reihenfolge durch.
        // Die Datenbank allein würde alphabetisch sortieren (Economy, Premium, Standard).
        for (Zimmerklasse zimmerklasse : Zimmerklasse.values()) {
            for (Belegungsart belegungsart : Belegungsart.values()) {
                // Preise dieser Kombination, schon nach Startdatum sortiert, hinten anhängen.
                allePreise.addAll(preisRepository.findByZimmerklasseAndBelegungsartOrderByGueltigAbAsc(
                        zimmerklasse, belegungsart));
            }
        }
        return allePreise;
    }

    // Prüft, ob alle Eingaben für einen neuen Preis gültig sind (UC03, Schritt 3).
    // Bekommt: Zimmerklasse, Belegungsart, Nachtpreis und den ersten gültigen Tag.
    // Gibt nichts zurück. Wirft eine IllegalArgumentException, wenn eine Eingabe fehlt oder ungültig ist.
    private void pruefeEingaben(Zimmerklasse zimmerklasse, Belegungsart belegungsart,
                                BigDecimal nachtpreis, LocalDate gueltigAb) {
        // Klasse und Belegungsart braucht man, damit klar ist, für welche Zimmer der Preis gilt.
        if (zimmerklasse == null) {
            throw new IllegalArgumentException("Bitte eine Zimmerklasse auswählen.");
        }
        if (belegungsart == null) {
            throw new IllegalArgumentException("Bitte eine Belegungsart auswählen.");
        }
        // Ein Preis von 0 € oder weniger ergibt keinen Sinn, Zimmer werden nicht verschenkt.
        // compareTo vergleicht BigDecimal-Werte: Ergebnis <= 0 heißt "kleiner oder gleich 0".
        if (nachtpreis == null || nachtpreis.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Der Nachtpreis muss größer als 0 sein.");
        }
        // Ohne Startdatum weiß das System nicht, ab wann der Preis gilt.
        if (gueltigAb == null) {
            throw new IllegalArgumentException("Bitte angeben, ab wann der Preis gilt.");
        }
    }
}
