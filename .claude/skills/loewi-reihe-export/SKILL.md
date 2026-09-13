---
name: loewi-reihe-export
description: >
  Übersetzt eine mit Claude im Gespräch geplante Unterrichtsreihe bzw. ein
  Unterrichtsvorhaben (Fach, Reihentitel, Jahrgang, einzelne Stunden/Einheiten mit
  Lernzielen, Material, Methoden, Differenzierung usw.) verlustfrei in die JSON-Datei,
  die sich per "Geteilte Reihe einfügen" direkt in die Unterrichtsdokumentations-App
  Loewi importieren lässt. IMMER verwenden, wenn der Nutzer nach dem gemeinsamen Planen
  einer Unterrichtsreihe/eines Unterrichtsvorhabens sagt, er wolle das Ergebnis "für
  Loewi exportieren", "in Loewi importieren", "als Loewi-Datei", "ins Loewi-Format
  übersetzen" haben, oder kurz "exportier das", "mach mir die Datei dafür", "überführ
  das in Loewi" o.ä. sagt — auch wenn er den Skill-Namen nicht kennt. Gilt unabhängig
  davon, ob gerade in Claude Code, claude.ai oder einer anderen Claude-Oberfläche
  gearbeitet wird.
---

# Loewi-Reihe-Export

Loewi ist eine Offline-Unterrichtsdokumentations-App (eine einzige `index.html`, Daten
nur lokal auf dem Gerät). Sie hat eine eingebaute Import-Funktion für "geteilte
Unterrichtsreihen": eine JSON-Datei in einem exakten, festen Format. Dieser Skill
erzeugt genau diese Datei aus dem, was der Nutzer zuvor mit Claude an Unterricht
geplant hat — damit die Planung ohne Abtippen direkt in Loewi landet.

## Ablauf

1. **Gespräch auswerten.** Sieh den gesamten bisherigen Verlauf durch: Fach, Titel der
   Reihe, Jahrgang/Klassenstufe, und pro Stunde/Einheit alles, was zu den Feldern unten
   passt — auch wenn im Gespräch andere Begriffe fielen (siehe Zuordnungstabelle).
2. **Nur bei den zwei Kernfeldern nachfragen, sonst nicht.** Wenn **Reihentitel** oder
   **Fach** aus dem Gespräch nicht eindeutig hervorgehen, kurz nachfragen. Für alle
   anderen Felder gilt: nichts erfinden oder erraten — was im Gespräch nicht vorkam,
   bleibt ein leerer String `""`. Loewi zeigt leere Felder einfach als "nicht
   ausgefüllt" an; das ist ein normaler, kein fehlerhafter Zustand. Der Wert dieses
   Skills liegt darin, sofort zu liefern, nicht darin, ein perfekt vollständiges
   Formular zu erzwingen.
3. **JSON nach dem Schema unten bauen** (Referenz mit Beispiel:
   `references/beispiel.json`).
4. **Validieren, bevor du die Datei ausgibst:**
   ```
   python3 scripts/validate_reihe.py <pfad-zur-datei>.json
   ```
   Das Skript prüft die Pflichtfelder und harten Formatregeln (siehe unten) und meldet
   jeden Verstoß konkret. Erst ausliefern, wenn es fehlerfrei durchläuft — ein Format-
   fehler bedeutet, dass Loewi die Datei beim Import komplett zurückweist
   ("Das ist keine geteilte Unterrichtsreihe"), nicht nur einzelne Felder verwirft.
5. **Datei benennen** wie Loewis eigener Export: `Unterrichtsreihe_<Titel>.json`, Titel
   dabei auf `[\wäöüÄÖÜß-]` reduziert und auf 40 Zeichen gekürzt (Leerzeichen etc. zu
   `_`). Das ist keine Import-Voraussetzung, sondern nur Konsistenz mit der App.
6. **Ausliefern, je nach Umgebung:**
   - **Claude Code:** Datei ins aktuelle Arbeitsverzeichnis schreiben (oder dorthin,
     wo der Nutzer sie haben will) und den Pfad nennen.
   - **claude.ai / Umgebungen ohne Dateisystemzugriff für den Nutzer:** Datei als
     Download/Artifact bereitstellen.
7. **Kurz den Import-Weg nennen:** In Loewi zum Tab *Unterricht* wechseln → Fach wählen
   (oder neu anlegen) → unten *„Geteilte Reihe einfügen"* → die erzeugte Datei
   auswählen. Loewi zeigt dann einen Bestätigungsdialog mit Titel, Anzahl Einheiten und
   Zielfach — der Nutzer bestätigt einmal, fertig.

## Zielschema

```json
{
  "typ": "klassendoku-reihe",
  "version": 1,
  "erstellt": "YYYY-MM-DD",
  "fach": "Sport",
  "reihe": { "titel": "Werfen und Fangen", "jahrgang": "3/4" },
  "einheiten": [
    { "nr": 1, "titel": "...", "ziel": "...", "inhalt": "...", "kompetenz": "...",
      "methode": "...", "aufwaermen": "...", "hauptteil": "...", "abschluss": "...",
      "material": "...", "link": "...", "sicherheit": "...", "diff": "...",
      "reflexion": "...", "anpassung": "..." }
  ]
}
```

Vollständiges, ausgefülltes Beispiel: `references/beispiel.json`.

### Harte Formatregeln — Loewis Import prüft genau das

- `typ` muss exakt der String `"klassendoku-reihe"` sein.
- `reihe` muss als Objekt vorhanden sein (mindestens `{}`) — fehlt es, weist Loewi die
  ganze Datei ab.
- Alles andere ist weich: `erstellt` und `fach` werden nur informativ im
  Bestätigungsdialog angezeigt, nicht geprüft. `reihe.titel` wird beim Einlesen auf 120
  Zeichen gekürzt, `reihe.jahrgang` auf 8 Zeichen — beides unkritisch, solange die
  Werte vorher nicht absurd lang sind.
- `nr` in den Einheiten wird beim Import **ignoriert** — Loewi vergibt die Stundennummer
  automatisch aus der Reihenfolge im `einheiten`-Array. Die Array-Reihenfolge ist also
  die tatsächliche Unterrichtsfolge, nicht der `nr`-Wert. Trag die Einheiten trotzdem
  mit `nr` ein (Lesbarkeit für Menschen, die die Datei später öffnen), aber sortiere das
  Array selbst in der gewünschten Stundenfolge.
- Für `jahrgang` gibt es in Loewis eigener Oberfläche eine feste Auswahl (`""`, `1`,
  `2`, `3`, `4`, `1/2`, `3/4`, `1–4`). Der Import selbst validiert das nicht, aber
  verwende nach Möglichkeit einen dieser Werte, damit die Reihe in Loewi genauso
  aussieht wie dort angelegte.

## Zuordnungstabelle: Gesprächsinhalt → Loewi-Feld

Diese Bezeichnungen sind die tatsächlichen Beschriftungen, die der Nutzer später in
Loewi sieht (aus der App selbst übernommen) — ordne Gesprächsinhalte danach zu, auch
wenn im Gespräch andere Wörter fielen.

| Feld | Beschriftung in Loewi | Typische Gesprächsbegriffe |
|---|---|---|
| `titel` | Thema der Einheit | "Thema", "worum es in der Stunde geht" |
| `ziel` | Stundenziel / Lernziel | "Lernziel", "Ziel der Stunde" |
| `inhalt` | Inhaltsbereich | "Inhaltsbereich", "Themenfeld", Lehrplanbezug |
| `kompetenz` | Kompetenzerwartung | "Kompetenz", Bezug zu Kernlehrplan/Bildungsstandards |
| `methode` | Methode / Sozialform | "Methode", "Sozialform", "Partnerarbeit", "Stationenlernen" |
| `aufwaermen` | Aufwärmen / Einstieg | "Einstieg", "Warm-up" (bei Sport wörtlich, sonst als Einstieg nutzen) |
| `hauptteil` | Hauptteil | "Hauptteil", "Erarbeitung" |
| `abschluss` | Abschluss / Cool-down | "Abschluss", "Sicherung", "Cool-down" (bei Sport wörtlich) |
| `material` | Material & Geräte | "Material", "benötigte Materialien", "Geräte" |
| `link` | Link zu den Materialien | genannte OneDrive-/Drive-/Cloud-Links |
| `sicherheit` | Sicherheit & Organisation | "Sicherheitshinweise", organisatorische Hinweise |
| `diff` | Differenzierung | "Differenzierung", "Förderung und Forderung" |
| `reflexion` | Reflexion – wie lief es? | nur relevant, wenn die Stunde schon gehalten wurde |
| `anpassung` | Anpassung fürs nächste Mal | dito, meist bei reiner Vorab-Planung leer |

`reflexion` und `anpassung` bleiben bei einer Planung, die noch nicht gehalten wurde,
fast immer leer — das ist erwartet, kein fehlendes Detail.

## Falls die Planung nicht in "Einheiten" gedacht war

Manche Gespräche planen eine Reihe nicht explizit stundenweise, sondern als
durchlaufendes Thema mit Phasen. Zerlege sie dann sinnvoll in einzelne Einheiten (eine
pro Unterrichtsstunde), auch wenn der Nutzer sie nicht so benannt hat — Loewi organisiert
Reihen grundsätzlich stundenweise.
