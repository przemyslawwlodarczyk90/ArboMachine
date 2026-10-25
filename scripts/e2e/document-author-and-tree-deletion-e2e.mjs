// Scenariusz E2E etapow 42, 44-54 w headless Chrome przez DevTools Protocol - Node.js 22+, bez zaleznosci.
// Etap 42: "Moj profil" -> "Dane do dokumentow" -> dane trafiaja do PDF projektu. Etap 44: "Usun drzewo" na karcie
// drzewa (dwa kroki, blokada bez polaczenia). Etap 45: numer umowy i cel opracowania w oknie edycji projektu.
// Etapy 46-47: okno "Niezapisane zmiany" po kliknieciu w link i po "Wstecz" przegladarki. Etap 48: to samo okno
// dla pracy, waloryzacji i kontroli stanu (z nazwa sekcji w pytaniu). Etap 49: oznaczenia, "Dane do dokumentow"
// i okno edycji projektu. Etap 50: okno "Nowy projekt" (bez "Zapisz" w pytaniu) i dane konta. Etap 51: termin
// nastepnej kontroli. Etap 52: podpis zdjecia i wylaczenie zdjecia z dokumentu PDF. Wymaga uruchomionej aplikacji z wylaczonym potwierdzaniem e-maila
// (--app.registration.email-confirmation-required=false) - konto zaklada sam. Uzycie:
//   node scripts/e2e/document-author-and-tree-deletion-e2e.mjs --base http://localhost:8086
//        [--chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"]
import { spawn } from 'node:child_process';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const options = Object.fromEntries(process.argv.slice(2).reduce((pairs, value, index, all) =>
  (value.startsWith('--') ? [...pairs, [value.slice(2), all[index + 1]]] : pairs), []));
const BASE = (options.base ?? 'http://localhost:8083').replace(/\/$/, '');
const CHROME = options.chrome ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const DEBUG_PORT = 9334;
const EMAIL = `autor.e2e.${Date.now()}@example.com`;
const PASSWORD = 'Haslo12345';

const results = [];
const check = (name, isPassed, details = '') => {
  results.push({ name, isPassed, details });
  console.log(`${isPassed ? 'PASS' : 'FAIL'}  ${name}${details ? ` - ${details}` : ''}`);
};
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

let apiToken = null;
async function api(path, { method = 'GET', body } = {}) {
  const response = await fetch(BASE + path, { method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(apiToken ? { Authorization: `Bearer ${apiToken}` } : {}) },
    body: body ? JSON.stringify(body) : undefined });
  const text = await response.text();
  if (!response.ok) throw new Error(`${method} ${path} -> ${response.status} ${text}`);
  try { return text ? JSON.parse(text) : null; } catch { return text; }
}

class Cdp {
  constructor(webSocketUrl) {
    this.socket = new WebSocket(webSocketUrl);
    this.nextId = 1;
    this.pending = new Map();
    this.socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      if (message.id && this.pending.has(message.id)) {
        const { resolve, reject } = this.pending.get(message.id);
        this.pending.delete(message.id);
        if (message.error) reject(new Error(JSON.stringify(message.error))); else resolve(message.result);
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
  throw new Error(`Nie doczekano sie: ${description}\n--- tekst strony ---\n${String(visibleText).slice(0, 1500)}`);
}

async function navigate(path) {
  await send('Page.navigate', { url: BASE + path });
  await wait(300);
  await waitFor('zaladowanie strony', 'return document.readyState === "complete" && !!document.querySelector("#root > *")');
}

// Wpisanie wartosci w kontrolowane pole Reacta (setter z prototypu + zdarzenie input).
const typeIntoLabeled = (labelText, value) => evaluate(`
  const label = [...document.querySelectorAll('label')].find((element) => element.textContent.trim() === ${JSON.stringify(labelText)});
  if (!label) throw new Error('Brak pola: ' + ${JSON.stringify(labelText)});
  const element = document.getElementById(label.htmlFor);
  const prototype = element instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value').set.call(element, ${JSON.stringify(value)});
  element.dispatchEvent(new Event('input', { bubbles: true }));
  return true;`);

const typeInto = (selector, value) => evaluate(`
  const element = document.querySelector(${JSON.stringify(selector)});
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(element, ${JSON.stringify(value)});
  element.dispatchEvent(new Event('input', { bubbles: true }));
  return true;`);

const clickButton = (text) => evaluate(`
  const button = [...document.querySelectorAll('button, a')].find((element) => element.textContent.trim() === ${JSON.stringify(text)} && !element.disabled);
  if (!button) throw new Error('Brak aktywnego przycisku: ' + ${JSON.stringify(text)});
  button.click();
  return true;`);

const isButtonDisabled = (text) => evaluate(`
  const button = [...document.querySelectorAll('button')].find((element) => element.textContent.trim() === ${JSON.stringify(text)});
  return button ? button.disabled : null;`);

const bodyText = () => evaluate('return document.body.innerText');

async function setOffline(isOffline) {
  await send('Network.emulateNetworkConditions', { offline: isOffline, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
}

// Tresc PDF (autor, wykonawca, metody) sprawdza DocumentAuthorProfileApiIntegrationTest przez PDFBox - tu tylko,
// ze dokument z zapisanymi danymi autora powstaje na dzialajacej aplikacji.
async function isPdfGenerated(projectId) {
  const response = await fetch(`${BASE}/api/projects/${projectId}/documents/project-report`, { headers: { Authorization: `Bearer ${apiToken}` } });
  const bytes = new Uint8Array(await response.arrayBuffer());
  return response.ok && bytes.length > 1000 && String.fromCharCode(...bytes.slice(0, 5)) === '%PDF-';
}

async function main() {
  const profileDirectory = mkdtempSync(join(tmpdir(), 'arbomachine-chrome-'));
  const chrome = spawn(CHROME, ['--headless=new', `--remote-debugging-port=${DEBUG_PORT}`, `--user-data-dir=${profileDirectory}`,
    '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore' });
  try {
    let version = null;
    for (let attempt = 0; attempt < 50 && !version; attempt++) {
      try { version = await (await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/version`)).json(); } catch { await wait(200); }
    }
    cdp = new Cdp(version.webSocketDebuggerUrl);
    await cdp.opened();
    const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' });
    session = (await cdp.send('Target.attachToTarget', { targetId, flatten: true })).sessionId;
    await send('Page.enable');
    await send('Runtime.enable');
    await send('Network.enable');
    await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });

    await api('/api/users/register', { method: 'POST', body: {
      profile: { firstName: 'Anna', lastName: 'Dębowa', email: EMAIL }, password: PASSWORD, accountType: 'ARBORIST' } });
    apiToken = (await api('/api/users/login', { method: 'POST', body: { email: EMAIL, password: PASSWORD } })).accessToken;
    const project = await api('/api/projects', { method: 'POST', body: { name: 'Park E2E autora' } });
    const treeToDelete = await api(`/api/projects/${project.id}/trees`, { method: 'POST', body: { inventoryNumber: '7' } });
    const treeToKeep = await api(`/api/projects/${project.id}/trees`, { method: 'POST', body: { inventoryNumber: '8' } });
    await api(`/api/projects/${project.id}/trees/${treeToDelete.id}/works`, { method: 'POST', body: { name: 'Cięcie' } });

    await navigate('/login');
    await typeInto('input[type=email]', EMAIL);
    await typeInto('input[type=password]', PASSWORD);
    await clickButton('Zaloguj się');
    await waitFor('panel po zalogowaniu', 'return location.pathname === "/projects"');

    // --- Etap 42: dane do dokumentow ---
    await navigate('/arborist/profile');
    await waitFor('sekcja danych do dokumentow', 'return !!document.querySelector(".document-author-profile-section form")');
    check('Moj profil: sekcja "Dane do dokumentow" z 5 polami',
      await evaluate('return document.querySelectorAll(".document-author-profile-section .field").length') === 5);
    check('Zapis zablokowany bez zmian', await isButtonDisabled('Zapisz dane do dokumentów') === true);
    await typeIntoLabeled('Autor opracowania', 'mgr inż. Anna Dębowa');
    await typeIntoLabeled('Kwalifikacje autora', 'Certyfikowany Inspektor Drzew');
    await typeIntoLabeled('Wykonawca (firma)', 'Zieleń E2E');
    await typeIntoLabeled('Metody i techniki pracy', 'Pomiar obwodu na wysokości 130 cm.');
    await clickButton('Zapisz dane do dokumentów');
    await waitFor('komunikat zapisu', 'return document.body.innerText.includes("Dane do dokumentów zostały zapisane.")');
    const savedProfile = await api('/api/users/me/document-author-profile');
    check('Dane autora zapisane na serwerze', savedProfile.hasProfile && savedProfile.authorFullName === 'mgr inż. Anna Dębowa'
      && savedProfile.companyDetails === null, JSON.stringify(savedProfile));
    await navigate('/arborist/profile');
    await waitFor('formularz po odswiezeniu', 'return !!document.querySelector(".document-author-profile-section form")');
    check('Po odswiezeniu formularz pokazuje zapisane dane',
      await evaluate('return [...document.querySelectorAll(".document-author-profile-section input")].some((input) => input.value === "Zieleń E2E")'));
    check('Dokument PDF projektu powstaje po zapisaniu danych autora', await isPdfGenerated(project.id));

    await navigate(`/projects/${project.id}/documentation`);
    await waitFor('karta dokumentu', 'return !!document.querySelector(".project-report-card")');
    check('Karta dokumentu wskazuje "Dane do dokumentow" w profilu', (await bodyText()).includes('„Dane do dokumentów”'));
    // Etap 54: wybor zakresu kart drzew i generowanie PDF z przegladarki.
    await evaluate(`const select = document.getElementById('project-report-tree-cards');
      Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value').set.call(select, 'NONE');
      select.dispatchEvent(new Event('change', { bubbles: true })); return true;`);
    await clickButton('Generuj dokument PDF');
    await waitFor('wygenerowany dokument', 'return document.body.innerText.includes("Dokument został wygenerowany")', 30_000);
    check('Etap 54: dokument "Bez kart drzew" generuje sie z przegladarki', true);

    // --- Etap 45: numer umowy i cel opracowania w projekcie ---
    await navigate(`/projects/${project.id}`);
    await clickButton('Edytuj projekt');
    await waitFor('okno edycji projektu', 'return !!document.querySelector(".modal")');
    await typeIntoLabeled('Numer umowy', 'UM/45/2026');
    await typeIntoLabeled('Cel opracowania', 'Inwentaryzacja przed przebudową alei');
    await typeIntoLabeled('Podstawa prawna', 'Art. 83 ustawy o ochronie przyrody');
    await clickButton('Zapisz');
    await waitFor('zamkniecie okna', 'return !document.querySelector(".modal")');
    check('Naglowek projektu pokazuje numer umowy', (await bodyText()).includes('Umowa: UM/45/2026'));
    const savedProject = await api(`/api/projects/${project.id}`);
    check('Numer umowy i cel opracowania zapisane na serwerze',
      savedProject.contractNumber === 'UM/45/2026' && savedProject.documentPurpose === 'Inwentaryzacja przed przebudową alei');
    check('Etap 53: podstawa prawna zapisana na serwerze', savedProject.legalBasis === 'Art. 83 ustawy o ochronie przyrody');

    // --- Etap 46: niezapisane zmiany przy kliknieciu w link ---
    await navigate(`/projects/${project.id}/trees/${treeToKeep.id}`);
    await waitFor('formularz drzewa', 'return !!document.querySelector(".tree-field-form")');
    await typeIntoLabeled('Nazwa polska', 'lipa drobnolistna');
    await waitFor('znacznik niezapisanych zmian', 'return document.body.innerText.includes("Masz niezapisane zmiany.")');
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno niezapisanych zmian', 'return document.querySelector(".modal h2")?.textContent === "Niezapisane zmiany"');
    check('Link z niezapisanymi zmianami -> okno "Niezapisane zmiany"', true);
    await clickButton('Zostań');
    // Router zamyka okno (blocker.reset) w kolejnym renderze - czekamy na to, zamiast sprawdzac od razu po kliknieciu.
    await waitFor('zamkniecie okna po "Zostań"', 'return !document.querySelector(".modal")').catch(() => {});
    check('"Zostań" zostawia karte drzewa i wpisany tekst',
      !(await evaluate('return !!document.querySelector(".modal")')) && (await evaluate('return location.pathname')) === `/projects/${project.id}/trees/${treeToKeep.id}`
      && await evaluate('return [...document.querySelectorAll("input")].some((input) => input.value === "lipa drobnolistna")'));
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno niezapisanych zmian', 'return !!document.querySelector(".modal")');
    await clickButton('Zapisz');
    await waitFor('lista drzew po zapisie', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Zapisz" zapisuje drzewo i przechodzi do listy',
      (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}`)).speciesPolish === 'lipa drobnolistna');

    // --- Etap 47: przycisk "Wstecz" przegladarki przy niezapisanych zmianach ---
    // Wejscie na karte w aplikacji (klik z listy), zeby "Wstecz" bylo przejsciem routera, a nie przeladowaniem strony.
    await navigate(`/projects/${project.id}/trees`);
    await waitFor('link do drzewa', `return !!document.querySelector('a[href="/projects/${project.id}/trees/${treeToKeep.id}"]')`);
    await evaluate(`document.querySelector('a[href="/projects/${project.id}/trees/${treeToKeep.id}"]').click(); return true;`);
    await waitFor('formularz drzewa', 'return !!document.querySelector(".tree-field-form")');
    await typeIntoLabeled('Nazwa łacińska', 'Tilia cordata');
    await waitFor('znacznik niezapisanych zmian', 'return document.body.innerText.includes("Masz niezapisane zmiany.")');
    await evaluate('history.back(); return true;');
    await waitFor('okno po "Wstecz"', 'return document.querySelector(".modal h2")?.textContent === "Niezapisane zmiany"');
    check('"Wstecz" przegladarki z niezapisanymi zmianami -> okno "Niezapisane zmiany"',
      (await evaluate('return location.pathname')) === `/projects/${project.id}/trees/${treeToKeep.id}`);
    await clickButton('Odrzuć zmiany');
    await waitFor('lista drzew po odrzuceniu', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Odrzuć zmiany" wraca do poprzedniej strony bez zapisu',
      (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}`)).speciesLatin == null);

    // --- Etap 48: niezapisane zmiany w pracach, waloryzacji i kontroli stanu ---
    const treePath = `/projects/${project.id}/trees/${treeToKeep.id}`;
    const modalText = () => evaluate('return document.querySelector(".modal")?.innerText ?? ""');
    await navigate(treePath);
    await waitFor('sekcja prac', 'return document.body.innerText.includes("Dodaj pracę")');
    await clickButton('Dodaj pracę');
    await waitFor('formularz pracy', 'return !!document.querySelector(".tree-work-form")');
    await typeIntoLabeled('Nazwa *', 'Podkrzesanie korony');
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno przy pracy', 'return !!document.querySelector(".modal")');
    check('Niezapisana praca -> okno z "(prace)"', (await modalText()).includes('Masz niezapisane zmiany (prace)'), await modalText());
    await clickButton('Zapisz');
    await waitFor('lista drzew po zapisie pracy', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Zapisz" z okna zapisuje prace',
      (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}/works`)).some((work) => work.name === 'Podkrzesanie korony'));

    await navigate(treePath);
    await waitFor('sekcja waloryzacji', 'return document.body.innerText.includes("Dodaj waloryzację")');
    await clickButton('Dodaj waloryzację');
    await waitFor('formularz waloryzacji', 'return !!document.querySelector(".valuation-form")');
    await typeIntoLabeled('Wartość wiekowa', 'drzewo ok. 120-letnie');
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno przy waloryzacji', 'return !!document.querySelector(".modal")');
    check('Niezapisana waloryzacja -> okno z "(waloryzacja)"', (await modalText()).includes('(waloryzacja)'), await modalText());
    await clickButton('Odrzuć zmiany');
    await waitFor('lista drzew po odrzuceniu waloryzacji', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Odrzuć zmiany" nie zapisuje waloryzacji',
      !(await api(`/api/projects/${project.id}/trees/${treeToKeep.id}/valuation`)).hasValuation);

    await navigate(treePath);
    await waitFor('sekcja kontroli', 'return document.body.innerText.includes("Dodaj kontrolę")');
    await clickButton('Dodaj kontrolę');
    await waitFor('formularz kontroli', 'return !!document.querySelector(".monitoring-form")');
    // "Uwagi" ma tez formularz drzewa - pole szukane tylko w formularzu kontroli.
    await evaluate(`const area = document.querySelector('.monitoring-form textarea');
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set.call(area, 'posusz w koronie');
      area.dispatchEvent(new Event('input', { bubbles: true })); return true;`);
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno przy kontroli', 'return !!document.querySelector(".modal")');
    check('Niezapisana kontrola -> okno z "(kontrola stanu)"', (await modalText()).includes('(kontrola stanu)'), await modalText());
    await clickButton('Zostań');
    await waitFor('zamkniecie okna po "Zostań"', 'return !document.querySelector(".modal")').catch(() => {});
    check('"Zostań" zostawia formularz kontroli z wpisanym tekstem',
      (await evaluate('return location.pathname')) === treePath
      && await evaluate('return [...document.querySelectorAll(".monitoring-form textarea")].some((area) => area.value === "posusz w koronie")'));
    await clickButton('Anuluj');
    await clickButton('← Wróć do listy drzew');
    await waitFor('lista drzew po anulowaniu kontroli', `return location.pathname === "/projects/${project.id}/trees"`);
    check('Po "Anuluj" w formularzu link dziala bez pytania', !(await evaluate('return !!document.querySelector(".modal")')));

    // --- Etap 51: termin nastepnej kontroli ---
    await navigate(treePath);
    await waitFor('sekcja kontroli', 'return document.body.innerText.includes("Dodaj kontrolę")');
    await clickButton('Dodaj kontrolę');
    await waitFor('formularz kontroli', 'return !!document.querySelector(".monitoring-form")');
    await typeIntoLabeled('Data kontroli', '2026-10-20');
    await typeIntoLabeled('Termin następnej kontroli', '2026-10-19');
    await clickButton('Zapisz kontrolę');
    await waitFor('blad terminu', 'return document.body.innerText.includes("nie może być wcześniejszy niż data kontroli")');
    check('Termin nastepnej kontroli przed data kontroli -> blad pod polem', true);
    await typeIntoLabeled('Termin następnej kontroli', '2027-04-15');
    await clickButton('Zapisz kontrolę');
    await waitFor('kontrola z terminem na liscie', 'return document.body.innerText.includes("Następna kontrola: ")');
    const monitoringRecords = await api(`/api/projects/${project.id}/trees/${treeToKeep.id}/monitoring`);
    check('Termin nastepnej kontroli zapisany na serwerze i widoczny na liscie',
      monitoringRecords.some((record) => record.nextInspectionDate === '2027-04-15'), JSON.stringify(monitoringRecords));

    // --- Etap 52: podpis zdjecia i wybor do dokumentu ---
    // Zdjecie 1x1 PNG wysylane przez API (wybor pliku w headless Chrome pomijamy).
    const pngBytes = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');
    const photoForm = new FormData();
    photoForm.append('file', new Blob([pngBytes], { type: 'image/png' }), 'IMG_E2E.png');
    const uploadedPhoto = await (await fetch(`${BASE}/api/projects/${project.id}/trees/${treeToKeep.id}/photos`, {
      method: 'POST', headers: { Authorization: `Bearer ${apiToken}` }, body: photoForm })).json();
    await navigate(treePath);
    await waitFor('kafelek zdjecia', 'return !!document.querySelector(".tree-photo-tile")');
    await clickButton('Podpis');
    await waitFor('formularz podpisu', 'return !!document.querySelector(".tree-photo-document-settings")');
    await typeIntoLabeled('Podpis w dokumencie', 'Ubytek w nasadzie pnia');
    await evaluate('document.querySelector(".tree-photo-document-settings input[type=checkbox]").click(); return true;');
    await evaluate(`[...document.querySelectorAll(".tree-photo-document-settings button")].find((button) => button.textContent.trim() === "Zapisz").click(); return true;`);
    await waitFor('plakietka "Poza dokumentem"', 'return document.body.innerText.includes("Poza dokumentem")');
    const savedPhoto = (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}/photos`)).find((photo) => photo.id === uploadedPhoto.id);
    check('Podpis zdjecia i wylaczenie z dokumentu zapisane na serwerze i widoczne na kafelku',
      savedPhoto?.caption === 'Ubytek w nasadzie pnia' && savedPhoto?.includedInDocument === false
      && (await bodyText()).includes('Ubytek w nasadzie pnia'), JSON.stringify(savedPhoto));

    // --- Etap 49: oznaczenia, dane do dokumentow, okno edycji projektu ---
    // Okno pytania moze lezec nad innym oknem (edycja projektu) - przyciski i tekst brane z ostatniego okna.
    const lastModalText = () => evaluate('return [...document.querySelectorAll(".modal")].pop()?.innerText ?? ""');
    const clickInLastModal = (text) => evaluate(`
      const modal = [...document.querySelectorAll(".modal")].pop();
      const button = [...modal.querySelectorAll("button")].find((element) => element.textContent.trim() === ${JSON.stringify(text)} && !element.disabled);
      if (!button) throw new Error('Brak przycisku w oknie: ' + ${JSON.stringify(text)});
      button.click();
      return true;`);

    await navigate(treePath);
    await waitFor('oznaczenia', 'return !!document.querySelector("section[aria-labelledby=tree-markings-heading] input[type=checkbox]")');
    await evaluate('document.querySelector("section[aria-labelledby=tree-markings-heading] input[type=checkbox]").click(); return true;');
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno przy oznaczeniach', 'return !!document.querySelector(".modal")');
    check('Niezapisane oznaczenia -> okno z "(oznaczenia)"', (await lastModalText()).includes('(oznaczenia)'), await lastModalText());
    await clickInLastModal('Zapisz');
    await waitFor('lista drzew po zapisie oznaczen', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Zapisz" z okna zapisuje oznaczenia',
      (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}/markings`)).codes.length === 1);

    await navigate('/arborist/profile');
    await waitFor('dane do dokumentow', 'return !!document.querySelector(".document-author-profile-section form")');
    await typeIntoLabeled('Wykonawca (firma)', 'Zieleń E2E zmieniona');
    await evaluate('document.querySelector(\'a[href="/projects"]\').click(); return true;');
    await waitFor('okno przy danych do dokumentow', 'return !!document.querySelector(".modal")');
    check('Niezapisane dane do dokumentow -> okno z "(dane do dokumentów)"',
      (await lastModalText()).includes('(dane do dokumentów)'), await lastModalText());
    await clickInLastModal('Odrzuć zmiany');
    await waitFor('lista projektow po odrzuceniu', 'return location.pathname === "/projects"');
    check('"Odrzuć zmiany" nie zapisuje danych do dokumentow',
      !JSON.stringify(await api('/api/users/me/document-author-profile')).includes('Zieleń E2E zmieniona'));

    await waitFor('link do projektu', `return !!document.querySelector('a[href="/projects/${project.id}"]')`);
    await evaluate(`document.querySelector('a[href="/projects/${project.id}"]').click(); return true;`);
    await waitFor('strona projektu', 'return document.body.innerText.includes("Edytuj projekt")');
    await clickButton('Edytuj projekt');
    await waitFor('okno edycji projektu', 'return !!document.querySelector(".modal")');
    await typeIntoLabeled('Numer umowy', 'UM/49/2026');
    await evaluate('history.back(); return true;');
    await waitFor('okno po "Wstecz" nad edycja projektu', 'return document.querySelectorAll(".modal").length === 2');
    check('"Wstecz" przy edycji projektu -> okno z "(dane projektu)"', (await lastModalText()).includes('(dane projektu)'), await lastModalText());
    await clickInLastModal('Zostań');
    await waitFor('zamkniecie pytania po "Zostań"', 'return document.querySelectorAll(".modal").length === 1').catch(() => {});
    check('"Zostań" zostawia okno edycji projektu z wpisanym numerem',
      (await evaluate('return location.pathname')) === `/projects/${project.id}`
      && await evaluate('return document.querySelectorAll(".modal").length === 1 && [...document.querySelectorAll(".modal input")].some((input) => input.value === "UM/49/2026")'));
    await evaluate('history.back(); return true;');
    await waitFor('ponowne pytanie', 'return document.querySelectorAll(".modal").length === 2');
    await clickInLastModal('Zapisz');
    await waitFor('lista projektow po zapisie', 'return location.pathname === "/projects"');
    check('"Zapisz" z pytania zapisuje projekt i wykonuje "Wstecz"',
      (await api(`/api/projects/${project.id}`)).contractNumber === 'UM/49/2026');

    // --- Etap 50: okno "Nowy projekt" i dane konta ---
    await waitFor('przycisk nowego projektu', 'return document.body.innerText.includes("Nowy projekt")');
    await clickButton('Nowy projekt');
    await waitFor('okno nowego projektu', 'return !!document.querySelector(".modal")');
    await typeIntoLabeled('Nazwa projektu *', 'Projekt porzucony E2E');
    await evaluate('document.querySelector(\'a[href="/arborist/profile"]\').click(); return true;');
    await waitFor('pytanie nad nowym projektem', 'return document.querySelectorAll(".modal").length === 2');
    check('Niezapisany nowy projekt -> okno z "(nowy projekt)" bez przycisku "Zapisz"',
      (await lastModalText()).includes('(nowy projekt)') && !(await lastModalText()).includes('Zapisz'), await lastModalText());
    await clickInLastModal('Odrzuć zmiany');
    await waitFor('profil po odrzuceniu', 'return location.pathname === "/arborist/profile"');
    check('"Odrzuć zmiany" zamyka okno i nie tworzy projektu',
      !(await evaluate('return !!document.querySelector(".modal")'))
      && !(await api('/api/projects')).some((item) => item.name === 'Projekt porzucony E2E'));

    await waitFor('formularz konta', 'return [...document.querySelectorAll("label")].some((label) => label.textContent.trim() === "Telefon")');
    await typeIntoLabeled('Telefon', '600700800');
    await evaluate('document.querySelector(\'a[href="/projects"]\').click(); return true;');
    await waitFor('pytanie przy danych konta', 'return !!document.querySelector(".modal")');
    check('Niezapisane dane konta -> okno z "(dane konta)"', (await lastModalText()).includes('(dane konta)'), await lastModalText());
    await clickInLastModal('Zapisz');
    await waitFor('lista projektow po zapisie konta', 'return location.pathname === "/projects"');
    check('"Zapisz" z okna zapisuje dane konta', (await api('/api/users/me')).phoneNumber === '600700800');

    // --- Etap 44: usuwanie drzewa ---
    await navigate(`/projects/${project.id}/trees/${treeToDelete.id}`);
    await waitFor('sekcja usuwania', 'return !!document.querySelector(".tree-deletion-section")');
    await setOffline(true);
    await evaluate('window.dispatchEvent(new Event("offline")); return true;');
    await waitFor('blokada bez polaczenia', 'return document.querySelector(".tree-deletion-section").innerText.includes("wymaga połączenia")');
    check('Bez polaczenia przycisk "Usun drzewo" zablokowany', await isButtonDisabled('Usuń drzewo') === true);
    await setOffline(false);
    await evaluate('window.dispatchEvent(new Event("online")); return true;');
    await waitFor('odblokowanie po powrocie polaczenia', `return [...document.querySelectorAll('button')].some((button) => button.textContent.trim() === 'Usuń drzewo' && !button.disabled)`, 20_000);
    await clickButton('Usuń drzewo');
    await waitFor('potwierdzenie', 'return document.body.innerText.includes("Czy na pewno usunąć drzewo")');
    await clickButton('Anuluj');
    check('Anuluj nie usuwa drzewa', (await api(`/api/projects/${project.id}/trees`)).length === 2);
    await clickButton('Usuń drzewo');
    await clickButton('Tak, usuń drzewo 7');
    await waitFor('powrot do listy drzew', `return location.pathname === "/projects/${project.id}/trees"`);
    const treesAfter = await api(`/api/projects/${project.id}/trees`);
    check('Po usunieciu: lista na serwerze ma tylko drzewo 8', treesAfter.length === 1 && treesAfter[0].id === treeToKeep.id,
      treesAfter.map((tree) => tree.inventoryNumber).join(', '));
    await waitFor('lista drzew bez drzewa 7', `return !!document.querySelector('a[href="/projects/${project.id}/trees/${treeToKeep.id}"]')`);
    check('Lista drzew na ekranie nie zawiera usunietego drzewa',
      await evaluate(`return !document.querySelector('a[href="/projects/${project.id}/trees/${treeToDelete.id}"]')`));
    await navigate(`/projects/${project.id}/trees/${treeToDelete.id}`);
    await waitFor('karta usunietego drzewa', 'return document.body.innerText.includes("To drzewo nie istnieje")');
    check('Adres usunietego drzewa -> "To drzewo nie istnieje"', true);
  } finally {
    chrome.kill();
  }
  const failed = results.filter((result) => !result.isPassed);
  console.log(`\nWynik: ${results.length - failed.length}/${results.length} PASS.`);
  process.exitCode = failed.length ? 1 : 0;
}

main().catch((error) => {
  console.error(error);
  const failed = results.filter((result) => !result.isPassed);
  console.log(`\nPrzerwano po ${results.length} krokach (${failed.length} FAIL).`);
  process.exitCode = 1;
});
