/* Kopiert nur die App-Dateien nach www/ – das ist der Ordner, den Capacitor
   in die APK packt. Die Web-Fassung (GitHub Pages) bleibt unverändert im Wurzelordner. */
import { rmSync, mkdirSync, copyFileSync } from "node:fs";

const DATEIEN = [
  "index.html", "manifest.webmanifest",
  "icon-192.png", "icon-512.png", "apple-touch-icon.png", "loewe-tour.png", "logo.png"
];
rmSync("www", { recursive: true, force: true });
mkdirSync("www", { recursive: true });
for (const d of DATEIEN) copyFileSync(d, "www/" + d);
console.log("www/ bereit (" + DATEIEN.length + " Dateien)");
