// Service worker ArboMachine (etap 26) - TYLKO powloka aplikacji: strona (index.html) i pliki /assets/ zbudowane przez
// Vite, zeby aplikacja otwierala sie bez polaczenia w terenie. Dane (/api) NIGDY nie sa tu cache'owane - kopie danych
// i kolejke zmian trzyma warstwa offline w IndexedDB. Kafelki mapy (OpenStreetMap) tez nie - polityka OSM.
const CACHE_NAME = 'arbomachine-app-shell-v1';
const APP_SHELL_URL = '/index.html';

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.add(APP_SHELL_URL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys()
    .then((cacheNames) => Promise.all(cacheNames.filter((name) => name !== CACHE_NAME).map((name) => caches.delete(name))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;

  // Nawigacja (dowolna trasa Reacta): najpierw siec (zawsze swieza wersja), bez sieci - zapisana powloka.
  if (request.mode === 'navigate') {
    event.respondWith(fetch(request)
      .then((response) => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(APP_SHELL_URL, copy));
        }
        return response;
      })
      .catch(() => caches.match(APP_SHELL_URL)));
    return;
  }

  // Pliki zbudowane przez Vite maja hash w nazwie - niezmienne, wiec najpierw cache, potem siec (i zapis).
  if (url.pathname.startsWith('/assets/') || url.pathname === '/favicon.svg') {
    event.respondWith(caches.match(request).then((cached) => cached ?? fetch(request).then((response) => {
      if (response.ok) {
        const copy = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
      }
      return response;
    })));
  }
});
