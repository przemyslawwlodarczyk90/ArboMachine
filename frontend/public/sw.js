// Service worker ArboMachine (etap 26) - powloka aplikacji i (etap 29-30) powiadomienia Web Push. Powloka: strona (index.html) i pliki /assets/ zbudowane przez
// Vite, zeby aplikacja otwierala sie bez polaczenia w terenie. Dane (/api) NIGDY nie sa tu cache'owane - kopie danych
// i kolejke zmian trzyma warstwa offline w IndexedDB. Kafelki mapy (OpenStreetMap) tez nie - polityka OSM.
const CACHE_NAME = 'arbomachine-app-shell-v2';
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

// ---------- Web Push (etapy 29-30) ----------
// Tresc wiadomosci (JSON: title, body, url, tag) jest odszyfrowana przez przegladarke. Klikniecie otwiera trase
// aplikacji z "url" (tylko sciezka tej samej aplikacji) albo strone glowna.
self.addEventListener('push', (event) => {
  let message = {};
  try {
    message = event.data ? event.data.json() : {};
  } catch {
    message = { body: event.data ? event.data.text() : '' };
  }
  event.waitUntil(self.registration.showNotification(message.title || 'ArboMachine', {
    body: message.body || '',
    tag: message.tag || undefined,
    icon: '/favicon.svg',
    data: { url: typeof message.url === 'string' && message.url.startsWith('/') ? message.url : '/' },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = new URL(event.notification.data?.url || '/', self.location.origin).href;
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
    const existing = windowClients.find((client) => client.url.startsWith(self.location.origin));
    if (existing) return existing.navigate(targetUrl).then((client) => (client ?? existing).focus());
    return self.clients.openWindow(targetUrl);
  }));
});
