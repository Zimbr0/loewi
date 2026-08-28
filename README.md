# Loewi

**Unterrichtsdokumentation für Lehrkräfte. Eine einzige HTML-Datei, die offline im Browser läuft — ohne Konto, ohne Server, ohne Cloud.**

➡️ **[Loewi öffnen](https://zimbr0.github.io/loewi/)**

---

## Warum mit Nummern statt Namen

Loewi ist so gebaut, dass in der Regel **keine personenbezogenen Schülerdaten** verarbeitet werden: Kinder erscheinen als Nummer der Klassenliste. Optional lässt sich zu jeder Nummer ein kurzes Kürzel hinterlegen (etwa Initialen) — das ist bewusst eine freiwillige Zutat und bleibt, wie alles andere, ausschließlich auf dem eigenen Gerät.

Wer mit vollen Namen arbeitet, verarbeitet personenbezogene Daten und braucht dafür in Nordrhein-Westfalen die Genehmigung der Schulleitung nach der VO-DV I. Dieses Programm nimmt niemandem die Prüfung ab; es ist nur so entworfen, dass sie im Regelfall gar nicht nötig wird.

## Wo die Daten liegen

Im `localStorage` des Browsers, mit dem Loewi geöffnet wurde — sonst nirgends. Es gibt keine Anmeldung, keinen Server, keine Übertragung. Daraus folgt zweierlei:

- **Immer über dieselbe Adresse öffnen.** Der Browser knüpft den Speicher an den Ort, von dem die Seite kommt. Dieselbe Datei von der Festplatte geöffnet ist für den Browser ein anderer Ort — die Eintragungen wären dann nicht zu sehen.
- **Regelmäßig sichern.** Unter *Daten* gibt es „Sicherung erstellen“ (eine JSON-Datei) und den Excel-Export. Löscht man die Browserdaten oder die App vom Startbildschirm, sind die Eintragungen weg. Loewi erinnert von sich aus, wenn die letzte Sicherung zu lange her ist.

## Was drin ist

| | |
|---|---|
| **Stundenplan** | Einmal eintragen, gilt jede Woche. Antippen einer Stunde stellt Klasse und Fach um, legt die Stunde für dieses Datum an und springt direkt ins Eintragen. Tages- und Wochenansicht. |
| **Stunden erfassen** | Mitarbeit als `+ / o / –`, dazu *krank* und *vergessen*, Notiz je Kind. „Alle anderen auf +“ für den Regelfall. Ausfall mit einem Griff. |
| **Klassen & Fächer** | Getrennt voneinander; Stunden hängen an Klasse *und* Fach. Auswahl über das Feld unten in der Mitte. |
| **Planung** | Unterrichtsreihen gehören zum Fach und sind klassenunabhängig, mit Einheiten, Stundenzielen, Material und Link (OneDrive, Drive …). Reihen lassen sich als Datei weitergeben. |
| **Tests** | Beobachtungsbogen mit frei wählbaren Kriterien — Skala 1–6 oder Textfeld (etwa die Höhe beim Hochsprung). Bewertung vom Kind aus, Bemerkungsfeld. Der Testschnitt bleibt vom Mitarbeitsschnitt getrennt. |
| **Übersicht** | Kennzahlen je Klasse, Liste aller Kinder, Verlauf je Kind. |
| **Teams** | Mannschaften nach Leistungsstufe und Geschlecht gleichmäßig verteilt; „K“ nimmt kranke Kinder heraus, Befreite stehen automatisch auf K. |
| **Berichte** | Excel-Mappe mit fünf Blättern; PDF-Bericht je Kind oder für die ganze Klasse — beides im Programm selbst erzeugt, ohne fremde Bibliothek. |
| **Zahlencode** | Sperrt die Ansicht gegen neugierige Blicke. Er verschlüsselt nichts und ersetzt keine Gerätesperre. |

## Aufs Handy legen

Die Adresse im Browser öffnen und „Zum Startbildschirm hinzufügen“ wählen (iPhone: Teilen-Menü, Android: Menü ⋮). Loewi verhält sich dann wie eine App und läuft ohne Netz weiter. Der Speicher bleibt derselbe wie im Browser-Tab.

## Ausprobieren

Unter [`demo/`](demo/) liegt eine ausgedachte Sicherungsdatei mit vier Klassen, acht Fächern, Unterrichtsreihen, gut hundert Stunden und mehreren Tests. Herunterladen, in Loewi unter *Daten → Sicherung einlesen* auswählen — und alles ist gefüllt. **Achtung:** Das ersetzt die vorhandenen Daten, also vorher selbst sichern.

Alle Namen darin sind erfunden.

## Technisches

Eine Datei, kein Bauwerkzeug, keine Abhängigkeiten. `index.html` enthält Aufbau, Gestaltung und Programm; dazu kommen nur der Offline-Speicher (`sw.js`), das Manifest und die Symbole. Der Excel-Schreiber (ZIP + OOXML) und der PDF-Schreiber (PDF 1.4 mit WinAnsi-Kodierung, damit Umlaute stimmen) sind von Hand geschrieben, damit nichts nachgeladen werden muss.

Zum Ändern genügt ein Texteditor. Zum Prüfen liegen Playwright-Läufe außerhalb dieses Repositorys.

## Lizenz

[MIT](LICENSE) — benutzen, ändern und weitergeben ausdrücklich erwünscht, gern auch im Kollegium.
