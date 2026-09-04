/* Loewi – Offline-Speicher.
   Die App selbst wird zuerst aus dem Netz geholt, damit eine neue Fassung
   ankommt; ohne Netz kommt sie aus dem Zwischenspeicher. Die Symbole werden
   umgekehrt zuerst aus dem Zwischenspeicher genommen, weil sie sich nie ändern.
   Die Unterrichtsdaten liegen NICHT hier, sondern im localStorage des Browsers. */

const CACHE = "loewi-v3";
const DATEIEN = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icon-192.png",
  "./icon-512.png",
  "./apple-touch-icon.png",
  "./loewe-tour.png"
];

self.addEventListener("install", ev => {
  ev.waitUntil(
    caches.open(CACHE)
      .then(c => c.addAll(DATEIEN))
      .then(() => self.skipWaiting())
      .catch(() => self.skipWaiting())
  );
});

self.addEventListener("activate", ev => {
  ev.waitUntil(
    caches.keys()
      .then(namen => Promise.all(namen.filter(x => x !== CACHE).map(x => caches.delete(x))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", ev => {
  const anfrage = ev.request;
  if(anfrage.method !== "GET") return;
  const url = new URL(anfrage.url);
  if(url.origin !== self.location.origin) return;

  const istSeite = anfrage.mode === "navigate" ||
                   url.pathname.endsWith("/") ||
                   url.pathname.endsWith("index.html");

  if(istSeite){
    /* Netz zuerst, Zwischenspeicher als Rückfallebene */
    ev.respondWith(
      fetch(anfrage)
        .then(antwort => {
          const kopie = antwort.clone();
          caches.open(CACHE).then(c => c.put("./index.html", kopie)).catch(() => {});
          return antwort;
        })
        .catch(() => caches.match("./index.html").then(a => a || caches.match("./")))
    );
    return;
  }

  /* Zwischenspeicher zuerst */
  ev.respondWith(
    caches.match(anfrage).then(treffer => treffer || fetch(anfrage).then(antwort => {
      if(antwort && antwort.status === 200 && antwort.type === "basic"){
        const kopie = antwort.clone();
        caches.open(CACHE).then(c => c.put(anfrage, kopie)).catch(() => {});
      }
      return antwort;
    }))
  );
});
