// Scenariusz E2E trybu terenowego (etap 32) w headless Chrome przez DevTools Protocol - Node.js 22+, bez zaleznosci.
// Wymaga uruchomionej aplikacji (zbudowany frontend serwowany przez Spring) i konta arborysty z projektem
// demonstracyjnym (scripts/demo/seed-demo-project.mjs). Uzycie:
//   node scripts/e2e/field-mode-e2e.mjs --base http://localhost:8086 --email arborysta@example.com --password ****
//        --project 108 [--chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"] [--out katalog-zrzutow]
// To NIE jest test na prawdziwym tablecie: dotyk, aparat, GPS i brak sieci sa emulowane (pozycja podstawiona przez
// Emulation.setGeolocationOverride, brak sieci przez Network.emulateNetworkConditions na stronie i service workerze).
import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const options = Object.fromEntries(process.argv.slice(2).reduce((pairs, value, index, all) =>
  (value.startsWith('--') ? [...pairs, [value.slice(2), all[index + 1]]] : pairs), []));
const BASE = (options.base ?? 'http://localhost:8083').replace(/\/$/, '');
const PROJECT_ID = Number(options.project);
const CHROME = options.chrome ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const OUT = options.out ?? mkdtempSync(join(tmpdir(), 'arbomachine-field-e2e-'));
const DEBUG_PORT = 9333;
if (!options.email || !options.password || !PROJECT_ID) {
  console.error('Podaj --email, --password i --project.');
  process.exit(1);
}
mkdirSync(OUT, { recursive: true });

const results = [];
const check = (name, isPassed, details = '') => {
  results.push({ name, isPassed, details });
  console.log(`${isPassed ? 'PASS' : 'FAIL'}  ${name}${details ? ` - ${details}` : ''}`);
};
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// ---------- API (sprawdzanie danych po stronie serwera) ----------
let apiToken = null;
async function api(path, { method = 'GET', body } = {}) {
  const response = await fetch(BASE + path, { method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(apiToken ? { Authorization: `Bearer ${apiToken}` } : {}) },
    body: body ? JSON.stringify(body) : undefined });
  const text = await response.text();
  if (!response.ok) throw new Error(`${method} ${path} -> ${response.status} ${text}`);
  return text ? JSON.parse(text) : null;
}

// ---------- Minimalny klient CDP ----------
class Cdp {
  constructor(webSocketUrl) {
    this.socket = new WebSocket(webSocketUrl);
    this.nextId = 1;
    this.pending = new Map();
    this.listeners = [];
    this.socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      if (message.id && this.pending.has(message.id)) {
        const { resolve, reject } = this.pending.get(message.id);
        this.pending.delete(message.id);
        if (message.error) reject(new Error(JSON.stringify(message.error))); else resolve(message.result);
      } else if (message.method) {
        this.listeners.forEach((listener) => listener(message));
      }
    });
  }

  opened() { return new Promise((resolve) => this.socket.addEventListener('open', resolve, { once: true })); }

  send(method, params = {}, sessionId) {
    const id = this.nextId++;
    this.socket.send(JSON.stringify({ id, method, params, ...(sessionId ? { sessionId } : {}) }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }
}

let cdp;
let session;
let photoFilePath;
const send = (method, params) => cdp.send(method, params, session);

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression: `(async () => { ${expression} })()`, awaitPromise: true, returnByValue: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description ?? result.exceptionDetails.text);
  return result.result.value;
}

async function waitFor(description, expression, timeoutMs = 15_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const value = await evaluate(expression);
      if (value) return value;
    } catch { /* strona sie przeladowuje */ }
    await wait(200);
  }
  const visibleText = await evaluate('return document.body.innerText').catch(() => '');
  throw new Error(`Nie doczekano sie: ${description}
--- tekst strony ---
${String(visibleText).slice(0, 1500)}`);
}

async function screenshot(name) {
  const { data } = await send('Page.captureScreenshot', { format: 'png' });
  writeFileSync(join(OUT, `${name}.png`), Buffer.from(data, 'base64'));
}

async function setViewport(width, height, isMobile) {
  await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: isMobile });
  await send("Emulation.setTouchEmulationEnabled", isMobile ? { enabled: true, maxTouchPoints: 5 } : { enabled: false });
}

async function navigate(path) {
  await send('Page.navigate', { url: BASE + path });
  await wait(300);
  await waitFor('zaladowanie strony', 'return document.readyState === "complete" && !!document.querySelector("#root > *")');
}

const pathname = () => evaluate('return location.pathname + location.search');

// Wpisanie wartosci w kontrolowany input Reacta (setter z prototypu + zdarzenie input).
const typeInto = (selector, value) => evaluate(`
  const element = document.querySelector(${JSON.stringify(selector)});
  const prototype = element instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value').set.call(element, ${JSON.stringify(value)});
  element.dispatchEvent(new Event('input', { bubbles: true }));
  return true;`);

const clickButton = (text, scope = 'document') => evaluate(`
  const button = [...${scope}.querySelectorAll('button, a')].find((element) => element.textContent.trim() === ${JSON.stringify(text)} && !element.disabled);
  if (!button) throw new Error('Brak przycisku: ' + ${JSON.stringify(text)});
  button.click();
  return true;`);

const bodyText = () => evaluate('return document.body.innerText');

async function setOffline(isOffline) {
  const conditions = { offline: isOffline, latency: 0, downloadThroughput: -1, uploadThroughput: -1 };
  await send('Network.emulateNetworkConditions', conditions);
  // Service worker ma osobne polaczenie - bez tego pobieralby powloke z sieci mimo "trybu samolotowego".
  const { targetInfos } = await cdp.send('Target.getTargets');
  for (const target of targetInfos.filter((info) => info.type === 'service_worker')) {
    const { sessionId } = await cdp.send('Target.attachToTarget', { targetId: target.targetId, flatten: true });
    await cdp.send('Network.enable', {}, sessionId);
    await cdp.send('Network.emulateNetworkConditions', conditions, sessionId);
  }
}

/** Etap 33: zdjecie z "galerii" - plik ustawiony w ukrytym polu wyboru pliku panelu drzewa (bez aparatu). */
async function pickGalleryPhoto(filePath) {
  const { root } = await send('DOM.getDocument', { depth: -1 });
  const { nodeId } = await send('DOM.querySelector', { nodeId: root.nodeId,
    selector: '.field-sheet input[type=file]:not([capture])' });
  await send('DOM.setFileInputFiles', { nodeId, files: [filePath] });
  await waitFor('okno dodawania zdjecia', 'return !!document.querySelector(".modal img")');
  await clickButton('Dodaj zdjęcie', 'document.querySelector(".modal")');
  await waitFor('zdjecie na urzadzeniu', 'return !!document.querySelector(".field-sheet .tree-photo-tile-pending")');
}

const pendingPhotoTileCount = () => evaluate('return document.querySelectorAll(".field-sheet .tree-photo-tile-pending").length');

async function openNewTab(path) {
  const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' });
  const attached = await cdp.send('Target.attachToTarget', { targetId, flatten: true });
  session = attached.sessionId;
  await send('Page.enable');
  await send('Runtime.enable');
  await send('Network.enable');
  return targetId;
}

// ---------- Scenariusz ----------
async function main() {
  const profileDirectory = mkdtempSync(join(tmpdir(), 'arbomachine-chrome-'));
  // Etap 33: maly plik z sygnatura JPEG (backend sprawdza typ po pierwszych bajtach).
  photoFilePath = join(profileDirectory, 'IMG_E2E_OFFLINE.jpg');
  writeFileSync(photoFilePath, Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(2000, 7)]));
  const chrome = spawn(CHROME, ['--headless=new', `--remote-debugging-port=${DEBUG_PORT}`, `--user-data-dir=${profileDirectory}`,
    '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore' });
  try {
    let version = null;
    for (let attempt = 0; attempt < 50 && !version; attempt++) {
      try { version = await (await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/version`)).json(); } catch { await wait(200); }
    }
    cdp = new Cdp(version.webSocketDebuggerUrl);
    await cdp.opened();
    let tabId = await openNewTab();
    await cdp.send('Browser.grantPermissions', { origin: BASE, permissions: ['geolocation'] });
    await send('Emulation.setGeolocationOverride', { latitude: 52.22955, longitude: 21.01225, accuracy: 4 });

    apiToken = (await api('/api/users/login', { method: 'POST', body: { email: options.email, password: options.password } })).accessToken;
    const treesBefore = await api(`/api/projects/${PROJECT_ID}/trees`);
    const firstTree = treesBefore[0];

    // --- Czesc A: menu arborysty i stare adresy ---
    await setViewport(1280, 800, false);
    await navigate('/login');
    await typeInto('input[type=email]', options.email);
    await typeInto('input[type=password]', options.password);
    await clickButton('Zaloguj się');
    await waitFor('panel po zalogowaniu', 'return location.pathname === "/projects"');
    check('Logowanie bez zapamietanego trybu (desktop) -> /projects', true);
    const menu = await evaluate('return [...document.querySelectorAll(".nav a")].map((link) => link.textContent.trim())');
    check('Menu arborysty: Projekty, Moj profil', JSON.stringify(menu) === JSON.stringify(['Projekty', 'Mój profil']), JSON.stringify(menu));
    check('Przycisk "Tryb terenowy" w pasku', (await bodyText()).includes('Tryb terenowy'));
    await navigate('/arborist/harmonogram');
    await waitFor('przekierowanie', 'return location.pathname === "/projects"');
    check('/arborist/harmonogram -> /projects', true);

    // --- Biuro: mapa projektu ---
    await navigate(`/projects/${PROJECT_ID}/map`);
    await waitFor('mapa biurowa', 'return !!document.querySelector(".map-container canvas")');
    const officeModes = await evaluate('return [...document.querySelectorAll(".map-view-mode button")].map((button) => button.textContent.trim())');
    check('Mapa biurowa: 4 tryby widoku', officeModes.length === 4, officeModes.join(', '));
    for (const mode of officeModes) {
      await clickButton(mode, 'document.querySelector(".map-view-mode")');
      await wait(300);
      const hasLegend = await evaluate('return !!document.querySelector(".map-legend")');
      check(`Mapa biurowa: legenda trybu "${mode}"`, hasLegend);
    }
    await screenshot('01-biuro-mapa-1280');
    await navigate(`/projects/${PROJECT_ID}/map?drzewo=${firstTree.id}`);
    await waitFor('zaznaczone drzewo', `return document.querySelector(".map-selected-tree")?.textContent.includes("Drzewo ${firstTree.inventoryNumber}")`);
    check('Mapa biurowa: ?drzewo= zaznacza drzewo', true);

    // --- Przelaczanie z karty drzewa ---
    await navigate(`/projects/${PROJECT_ID}/trees/${firstTree.id}`);
    await waitFor('karta drzewa', 'return !!document.querySelector(".tree-field-form")');
    await clickButton('Tryb terenowy');
    await waitFor('tryb terenowy', 'return location.pathname.startsWith("/field/projects/")');
    check('Karta drzewa -> "Tryb terenowy" -> /field/projects/:id?drzewo=:treeId',
      (await pathname()) === `/field/projects/${PROJECT_ID}?drzewo=${firstTree.id}`, await pathname());
    await waitFor('panel drzewa', `return document.querySelector(".field-sheet h2")?.textContent === "Drzewo ${firstTree.inventoryNumber}"`);
    check('Panel drzewa otwarty po przelaczeniu', true);
    await screenshot('02-teren-panel-1280');
    await clickButton('Widok biurowy');
    await waitFor('widok biurowy', 'return location.pathname.includes("/trees/")');
    check('"Widok biurowy" z wybranym drzewem -> pelna karta drzewa', (await pathname()) === `/projects/${PROJECT_ID}/trees/${firstTree.id}`);

    // --- Niezapisane zmiany przy przelaczaniu ---
    await waitFor('formularz', 'return !!document.querySelector(".tree-field-form textarea")');
    await typeInto('.tree-field-form textarea', 'Notatka niezapisana E2E');
    await wait(200);
    await clickButton('Tryb terenowy');
    await waitFor('pytanie o zmiany', 'return document.body.innerText.includes("Niezapisane zmiany")');
    check('Przelaczenie z niezapisanymi zmianami pyta o decyzje', true);
    await clickButton('Zostań');
    await wait(200);
    const keptValue = await evaluate('return document.querySelector(".tree-field-form textarea").value');
    check('"Zostan" nie gubi wpisanych danych', keptValue === 'Notatka niezapisana E2E' && (await pathname()).includes('/trees/'));
    await clickButton('Tryb terenowy');
    await waitFor('pytanie o zmiany', 'return document.body.innerText.includes("Niezapisane zmiany")');
    await clickButton('Odrzuć zmiany');
    await waitFor('tryb terenowy', 'return location.pathname.startsWith("/field/")');
    check('"Odrzuc zmiany" przechodzi do trybu terenowego', true);
    const savedNotes = (await api(`/api/projects/${PROJECT_ID}/trees/${firstTree.id}`)).notes ?? null;
    check('Odrzucone zmiany nie trafily na serwer', savedNotes === (firstTree.notes ?? null));

    // --- Zapamietany tryb ---
    await navigate('/');
    await waitFor('start wg trybu', 'return location.pathname !== "/"');
    check('Strona "/" po wybraniu trybu terenowego -> /field', (await pathname()) === '/field', await pathname());
    await screenshot('03-teren-lista-projektow-1280');

    // --- Teren na tablecie (pion) ---
    await setViewport(768, 1024, true);
    await navigate(`/field/projects/${PROJECT_ID}`);
    await waitFor('mapa terenowa', 'return !!document.querySelector(".field-map-area canvas")');
    const layout = await evaluate(`const map = document.querySelector(".field-map-area .map-container").getBoundingClientRect();
      return { mapHeight: Math.round(map.height), scrollHeight: document.documentElement.scrollHeight, viewport: innerHeight };`);
    check('Mapa na caly ekran pod paskiem, strona sie nie przewija', layout.scrollHeight <= layout.viewport && layout.mapHeight > 900,
      JSON.stringify(layout));
    await waitFor('wskaznik GPS', 'return document.querySelector(".field-gps")?.textContent.includes("±4 m")');
    check('Wskaznik GPS w pasku pokazuje dokladnosc (±4 m)', true);
    await screenshot('04-teren-mapa-768x1024');

    // --- "+ Drzewo tutaj" ---
    await clickButton('+ Drzewo tutaj');
    await waitFor('pomiar GPS', 'return document.body.innerText.includes("Dalej: numer drzewa") && !document.querySelector(".gnss-measuring")');
    const measurementText = await evaluate('return document.querySelector(".field-overlay").innerText');
    check('Podglad pomiaru: zrodlo, dokladnosc zgloszona przez urzadzenie, czas, bez "RTK"',
      measurementText.includes('GPS urządzenia (przeglądarka)') && measurementText.includes('dokładność zgłoszona przez urządzenie')
      && measurementText.includes('pomiar o') && !measurementText.includes('RTK'), measurementText.replace(/\s+/g, ' ').slice(0, 200));
    await screenshot('05-teren-pomiar-gps');
    await clickButton('Dalej: numer drzewa');
    await waitFor('okno numeru', 'return !!document.querySelector(".modal input")');
    const suggested = await evaluate('return document.querySelector(".modal input").value');
    const largestNumeric = Math.max(...treesBefore.map((tree) => Number(tree.inventoryNumber)).filter(Number.isFinite));
    check('Proponowany numer = najwiekszy liczbowy + 1', suggested === String(largestNumeric + 1), suggested);
    await clickButton('Dodaj drzewo');
    await waitFor('panel nowego drzewa', `return document.querySelector(".field-sheet h2")?.textContent === "Drzewo ${suggested}"`);
    const newTreeId = Number(new URLSearchParams((await pathname()).split('?')[1]).get('drzewo'));
    const newTree = await api(`/api/projects/${PROJECT_ID}/trees/${newTreeId}`);
    check('"+ Drzewo tutaj" zapisuje punkt GPS ze zrodlem i dokladnoscia',
      newTree.location?.source === 'DEVICE_GNSS' && Number(newTree.location?.accuracyM) === 4, JSON.stringify(newTree.location));
    // Etap 35: czas i sposob pomiaru zapisane przy punkcie.
    check('Punkt z GPS ma zapisany czas pomiaru i sposob pomiaru (adapter przegladarki)',
      newTree.location?.adapterCode === 'browser-geolocation' && !!newTree.location?.measuredAt
        && Math.abs(Date.parse(newTree.location.measuredAt) - Date.now()) < 10 * 60 * 1000, JSON.stringify(newTree.location));
    await screenshot('06-teren-nowe-drzewo-panel');

    // --- Oznaczenia z panelu ---
    await waitFor('oznaczenia', 'return document.querySelectorAll(".field-sheet .tree-marking-group input[type=checkbox]").length > 0');
    await evaluate('document.querySelectorAll(".field-sheet .tree-marking-group input[type=checkbox]")[0].click(); return true;');
    await clickButton('Zapisz oznaczenia', 'document.querySelector(".field-sheet")');
    await waitFor('plakietka w naglowku', 'return document.querySelectorAll(".field-sheet-heading .tree-marking-badge").length === 1');
    check('Zapis oznaczen od razu widoczny w naglowku panelu (z danych mapy)', true);
    const markings = await api(`/api/projects/${PROJECT_ID}/trees/${newTreeId}/markings`);
    check('Oznaczenie zapisane na serwerze', markings.codes.length === 1, JSON.stringify(markings));

    // --- Zdjecia: aparat od razu ---
    const captureAttribute = await evaluate('return document.querySelector(".field-sheet input[type=file][capture]")?.getAttribute("capture")');
    check('"Zrob zdjecie" otwiera aparat (input capture=environment)', captureAttribute === 'environment');

    // --- "Nastepne drzewo" po zapisaniu poprzedniego ---
    await clickButton('Następne drzewo');
    await waitFor('pomiar nastepnego drzewa', 'return document.body.innerText.includes("Nowe drzewo - pomiar GPS")');
    check('"Nastepne drzewo" zamyka panel i zaczyna pomiar nowego drzewa', !(await pathname()).includes('drzewo='));
    await clickButton('Anuluj', 'document.querySelector(".field-overlay")');
    await navigate(`/field/projects/${PROJECT_ID}?drzewo=${newTreeId}`);
    await waitFor('panel drzewa', `return document.querySelector(".field-sheet h2")?.textContent === "Drzewo ${suggested}"`);

    // --- Warstwy i przygotowanie offline ---
    await clickButton('Warstwy');
    await waitFor('arkusz warstw', 'return !!document.querySelector(".field-overlay h2")');
    await screenshot('07-teren-warstwy');
    await clickButton('Przygotuj projekt do pracy bez zasięgu');
    await waitFor('podsumowanie przygotowania', 'return document.body.innerText.includes("Gotowe:")', 60_000);
    const summary = await evaluate('return [...document.querySelectorAll(".field-overlay p")].map((p) => p.innerText).find((text) => text.startsWith("Gotowe:"))');
    check('Przygotowanie projektu konczy sie podsumowaniem', summary.includes(`${treesBefore.length + 1} drzew`), summary);
    await clickButton('Zamknij', 'document.querySelector(".field-overlay")');

    // Service worker przejmuje strone dopiero po przeladowaniu - powloka i pliki trafiaja do cache.
    await navigate(`/field/projects/${PROJECT_ID}`);
    await waitFor('service worker', 'return !!navigator.serviceWorker?.controller', 20_000);
    await waitFor('mapa terenowa', 'return !!document.querySelector(".field-map-area canvas")');

    // --- Offline ---
    const editedTree = treesBefore[1];
    const offlineNote = `Notatka offline E2E ${new Date().toISOString()}`;
    await setOffline(true);
    await send('Page.reload');
    await wait(500);
    await waitFor('aplikacja offline', 'return !!document.querySelector(".field-map-area canvas")', 20_000);
    check('Aplikacja otwiera sie bez sieci (powloka z service workera) i pokazuje mape z drzewami', true);
    await navigate(`/field/projects/${PROJECT_ID}?drzewo=${editedTree.id}`);
    await waitFor('panel drzewa offline', `return document.querySelector(".field-sheet h2")?.textContent === "Drzewo ${editedTree.inventoryNumber}"`);
    await waitFor('formularz offline', 'return !!document.querySelector(".field-sheet .tree-field-form textarea")');
    check('Panel drzewa dostepny offline', true);
    check('Offline: komunikat, ze zdjecia zapisza sie na urzadzeniu', (await bodyText()).includes('zdjęcia zapisują się na tym urządzeniu'));
    // Etap 33: zdjecie istniejacego drzewa bez sieci.
    const photosOfEditedTreeBefore = (await api(`/api/projects/${PROJECT_ID}/trees/${editedTree.id}/photos`)).length;
    await pickGalleryPhoto(photoFilePath);
    check('Zdjecie bez sieci zapisane na urzadzeniu (kafelek "Czeka na wysłanie")', (await pendingPhotoTileCount()) === 1);
    const notesFieldIndex = await evaluate(`return [...document.querySelectorAll(".field-sheet .tree-field-form textarea")].length - 1`);
    await evaluate(`const fields = document.querySelectorAll(".field-sheet .tree-field-form textarea");
      fields[${notesFieldIndex}].setAttribute("data-e2e", "notes"); return true;`);
    await typeInto('[data-e2e=notes]', offlineNote);
    await clickButton('Zapisz zmiany');
    await waitFor('zapis na urzadzeniu', 'return document.body.innerText.includes("zostaną wysłane po połączeniu")');
    check('Zapis formularza offline trafia do kolejki', true);

    // Nowe drzewo offline wskazane na mapie.
    await clickButton('Zamknij', 'document.querySelector(".field-sheet-actions")');
    await clickButton('Wskaż na mapie');
    // Losowy punkt w gornej czesci mapy - kolejne przebiegi dodaja drzewa, a klik w drzewo nie wskazuje miejsca.
    const mapBox = await evaluate('const box = document.querySelector(".field-map-area canvas").getBoundingClientRect(); return { x: box.x, y: box.y, width: box.width };');
    for (let attempt = 0; attempt < 5; attempt++) {
      const x = mapBox.x + 90 + Math.random() * (mapBox.width - 260);
      const y = mapBox.y + 90 + Math.random() * 250;
      for (const type of ['mousePressed', 'mouseReleased']) {
        await send('Input.dispatchMouseEvent', { type, x, y, button: 'left', clickCount: 1 });
      }
      try {
        await waitFor('okno numeru offline', 'return !!document.querySelector(".modal input")', 2500);
        break;
      } catch (missedEmptySpot) {
        if (attempt === 4) throw missedEmptySpot;
      }
    }
    await clickButton('Dodaj drzewo');
    await waitFor('panel drzewa offline', 'return document.querySelector(".field-sheet .badge")?.textContent === "Zapisane na urządzeniu"');
    const offlineTreeId = Number(new URLSearchParams((await pathname()).split('?')[1]).get('drzewo'));
    check('Nowe drzewo offline ma tymczasowe ujemne id', offlineTreeId < 0, String(offlineTreeId));
    await waitFor('sekcja zdjec drzewa offline', 'return !!document.querySelector(".field-sheet input[type=file]:not([capture])")');
    await pickGalleryPhoto(photoFilePath);
    check('Zdjecie drzewa utworzonego offline zapisane na urzadzeniu', (await pendingPhotoTileCount()) === 1);
    await screenshot('08-teren-offline');
    const pendingBefore = await evaluate('return document.querySelector(".field-connection")?.innerText');
    check('Pasek: offline i liczba zmian w kolejce', /offline/.test(pendingBefore) && /zmian/.test(pendingBefore), pendingBefore);

    // Zamkniecie karty i ponowne otwarcie bez sieci.
    await cdp.send('Target.closeTarget', { targetId: tabId });
    tabId = await openNewTab();
    await setOffline(true);
    await send('Emulation.setGeolocationOverride', { latitude: 52.22955, longitude: 21.01225, accuracy: 4 });
    await setViewport(768, 1024, true);
    await navigate(`/field/projects/${PROJECT_ID}?drzewo=${editedTree.id}`);
    await waitFor('formularz po restarcie', 'return !!document.querySelector(".field-sheet .tree-field-form textarea")', 20_000);
    const notesAfterRestart = await evaluate('return [...document.querySelectorAll(".field-sheet .tree-field-form textarea")].map((field) => field.value).join("|")');
    check('Po zamknieciu i otwarciu offline zmiany nadal sa widoczne', notesAfterRestart.includes(offlineNote));
    const pendingAfterRestart = await evaluate('return document.querySelector(".field-connection")?.innerText');
    check('Licznik kolejki zgadza sie po ponownym otwarciu', pendingAfterRestart === pendingBefore, pendingAfterRestart);
    check('Pasek liczy zdjecia na urzadzeniu', /zdjęcia: 2/.test(pendingAfterRestart), pendingAfterRestart);
    await waitFor('zdjecie po restarcie', 'return document.querySelectorAll(".field-sheet .tree-photo-tile-pending").length === 1');
    check('Po zamknieciu i otwarciu offline zdjecie nadal czeka na urzadzeniu (z podgladem)',
      await evaluate('return !!document.querySelector(".field-sheet .tree-photo-tile-pending img")?.src.startsWith("blob:")'));

    // Powrot sieci i synchronizacja.
    await setOffline(false);
    await waitFor('synchronizacja', 'return !/zmian|zdjęcia/.test(document.querySelector(".field-connection")?.innerText ?? "zmian")', 60_000);
    check('Po wlaczeniu sieci synchronizacja konczy sie bez bledow', !(await bodyText()).includes('Nie udało się zsynchronizować'));
    const treesAfter = await api(`/api/projects/${PROJECT_ID}/trees`);
    check('Nowe drzewo offline jest na serwerze z prawdziwym id', treesAfter.length === treesBefore.length + 2,
      `${treesBefore.length} -> ${treesAfter.length}`);
    const editedAfter = treesAfter.find((tree) => tree.id === editedTree.id);
    const unchangedFields = ['inventoryNumber', 'speciesLatin', 'speciesPolish', 'trunkCircumferenceCm', 'heightM', 'crownDiameterM',
      'conditionNotes', 'hazardNotes'];
    const clearedFields = unchangedFields.filter((field) => JSON.stringify(editedAfter[field] ?? null) !== JSON.stringify(editedTree[field] ?? null));
    check('Zmienione offline drzewo: notatka zapisana, zadne inne pole nie zostalo wyczyszczone',
      editedAfter.notes === offlineNote && clearedFields.length === 0 && !!editedAfter.location, clearedFields.join(', '));
    await waitFor('prawdziwe id w adresie', 'return !location.search.includes("drzewo=-")', 10_000).catch(() => {});

    // Etap 33: zdjecia offline na serwerze, bez duplikatow - takze po ponownym otwarciu i synchronizacji.
    const offlineTreeOnServer = treesAfter.find((tree) => !treesBefore.some((before) => before.id === tree.id) && tree.id !== newTreeId);
    const photosOfEditedTreeAfter = (await api(`/api/projects/${PROJECT_ID}/trees/${editedTree.id}/photos`)).length;
    const photosOfOfflineTree = offlineTreeOnServer ? (await api(`/api/projects/${PROJECT_ID}/trees/${offlineTreeOnServer.id}/photos`)).length : -1;
    check('Zdjecie istniejacego drzewa wyslane po polaczeniu (dokladnie jedno)', photosOfEditedTreeAfter === photosOfEditedTreeBefore + 1,
      `${photosOfEditedTreeBefore} -> ${photosOfEditedTreeAfter}`);
    check('Zdjecie drzewa utworzonego offline wyslane pod prawdziwe id drzewa', photosOfOfflineTree === 1, String(photosOfOfflineTree));
    await navigate(`/field/projects/${PROJECT_ID}?drzewo=${editedTree.id}`);
    await waitFor('panel po synchronizacji', 'return !!document.querySelector(".field-sheet .tree-photo-tile")');
    await wait(1500);
    check('Po synchronizacji brak zdjec czekajacych i brak duplikatow po ponownym otwarciu',
      (await pendingPhotoTileCount()) === 0
        && (await api(`/api/projects/${PROJECT_ID}/trees/${editedTree.id}/photos`)).length === photosOfEditedTreeAfter);

    // --- Ekrany ---
    await navigate(`/field/projects/${PROJECT_ID}?drzewo=${newTreeId}`);
    for (const [width, height, isMobile, name] of [[390, 844, true, '09-telefon-390x844'], [768, 1024, true, '10-tablet-768x1024'],
      [1024, 768, true, '11-tablet-1024x768'], [1280, 800, false, '12-laptop-1280x800']]) {
      await setViewport(width, height, isMobile);
      await wait(600);
      const fits = await evaluate('return document.documentElement.scrollHeight <= innerHeight && document.documentElement.scrollWidth <= innerWidth');
      check(`Ekran ${width}x${height}: bez przewijania strony`, fits);
      await screenshot(name);
    }
  } finally {
    chrome.kill();
  }
  const failed = results.filter((result) => !result.isPassed);
  console.log(`\nWynik: ${results.length - failed.length}/${results.length} PASS. Zrzuty ekranu: ${OUT}`);
  process.exitCode = failed.length ? 1 : 0;
}

main().catch((error) => {
  console.error(error);
  const failed = results.filter((result) => !result.isPassed);
  console.log(`\nPrzerwano po ${results.length} krokach (${failed.length} FAIL).`);
  process.exitCode = 1;
});
