#!/usr/bin/env python3
"""Validiert eine JSON-Datei gegen Loewis Import-Format fuer "geteilte Reihen".

Prueft exakt das, was Loewis eigener Import-Code (reiheEinfuegen, index.html)
verlangt bzw. sinnvoll erwartet. Exit-Code 0 = ok, 1 = mindestens ein Fehler.
Warnungen (Exit-Code bleibt 0) weisen auf Dinge hin, die Loewi zwar klaglos
schluckt, aber stillschweigend veraendert oder ignoriert.
"""
import json
import sys

REIHE_TYP = "klassendoku-reihe"
TEIL_FELDER = ["nr", "titel", "ziel", "inhalt", "kompetenz", "methode", "aufwaermen",
               "hauptteil", "abschluss", "material", "link", "sicherheit", "diff",
               "reflexion", "anpassung"]
STRING_FELDER = [f for f in TEIL_FELDER if f != "nr"]
JAHRGAENGE = ["", "1", "2", "3", "4", "1/2", "3/4", "1–4"]


def main():
    if len(sys.argv) != 2:
        print("Aufruf: validate_reihe.py <pfad-zur-datei>.json")
        sys.exit(1)

    pfad = sys.argv[1]
    errors = []
    warnings = []

    try:
        with open(pfad, "r", encoding="utf-8") as fh:
            text = fh.read()
    except OSError as e:
        print(f"FEHLER: Datei nicht lesbar: {e}")
        sys.exit(1)

    try:
        paket = json.loads(text)
    except json.JSONDecodeError as e:
        print(f"FEHLER: Kein gueltiges JSON ({e}). Loewi wuerde diese Datei mit "
              '"Datei nicht lesbar" ablehnen.')
        sys.exit(1)

    if not isinstance(paket, dict):
        errors.append("Die Datei muss auf oberster Ebene ein JSON-Objekt sein, kein Array/Wert.")
    else:
        if paket.get("typ") != REIHE_TYP:
            errors.append(
                f'"typ" muss exakt "{REIHE_TYP}" sein (gefunden: {paket.get("typ")!r}). '
                'Ohne exakte Uebereinstimmung weist Loewi die Datei komplett ab '
                '("Das ist keine geteilte Unterrichtsreihe").'
            )
        if "reihe" not in paket or not isinstance(paket.get("reihe"), dict):
            errors.append('"reihe" muss als Objekt vorhanden sein (mind. {}). '
                           "Fehlt es, weist Loewi die Datei komplett ab.")
        else:
            reihe = paket["reihe"]
            titel = reihe.get("titel", "")
            if not isinstance(titel, str) or not titel.strip():
                warnings.append('"reihe.titel" ist leer. Loewi zeigt dann "Ohne Titel" an.')
            elif len(titel) > 120:
                warnings.append(
                    f'"reihe.titel" ist {len(titel)} Zeichen lang, Loewi kuerzt beim '
                    "Import auf 120 Zeichen."
                )
            jahrgang = reihe.get("jahrgang", "")
            if jahrgang and jahrgang not in JAHRGAENGE:
                warnings.append(
                    f'"reihe.jahrgang" = {jahrgang!r} ist keiner von Loewis eigenen '
                    f"Auswahlwerten {JAHRGAENGE}. Wird trotzdem importiert (auf 8 "
                    "Zeichen gekuerzt), sieht in der App aber anders aus als dort "
                    "selbst angelegte Reihen."
                )
            if len(str(jahrgang)) > 8:
                warnings.append('"reihe.jahrgang" ist laenger als 8 Zeichen, wird beim '
                                 "Import abgeschnitten.")

        if "einheiten" not in paket:
            warnings.append('"einheiten" fehlt ganz — die Reihe wird ohne Stunden angelegt.')
        elif not isinstance(paket["einheiten"], list):
            errors.append('"einheiten" muss ein Array sein.')
        else:
            for i, e in enumerate(paket["einheiten"]):
                pos = f"einheiten[{i}]"
                if not isinstance(e, dict):
                    errors.append(f"{pos} muss ein Objekt sein.")
                    continue
                for feld in STRING_FELDER:
                    if feld in e and not isinstance(e[feld], str):
                        errors.append(
                            f'{pos}["{feld}"] muss ein String sein (gefunden: '
                            f"{type(e[feld]).__name__}). Loewi ignoriert nicht-string-"
                            "Werte beim Import kommentarlos, das Feld bliebe dann leer."
                        )
                unbekannt = [k for k in e.keys() if k not in TEIL_FELDER]
                if unbekannt:
                    warnings.append(
                        f"{pos} enthaelt unbekannte Felder {unbekannt} — Loewi ignoriert "
                        "sie beim Import stillschweigend."
                    )
            if not paket["einheiten"]:
                warnings.append('"einheiten" ist ein leeres Array — die Reihe wird ohne '
                                 "Stunden angelegt.")

    if warnings:
        print("Warnungen (Loewi importiert die Datei trotzdem, aber beachte Folgendes):")
        for w in warnings:
            print(f"  - {w}")

    if errors:
        print("\nFEHLER (Import wuerde fehlschlagen):" if warnings else
              "FEHLER (Import wuerde fehlschlagen):")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)

    print(("\n" if warnings else "") + "OK: Datei entspricht Loewis Import-Format.")
    sys.exit(0)


if __name__ == "__main__":
    main()
