// Scenariusz E2E etapow 42, 44, 45 i 46 w headless Chrome przez DevTools Protocol - Node.js 22+, bez zaleznosci.
// Etap 42: "Moj profil" -> "Dane do dokumentow" -> dane trafiaja do PDF projektu. Etap 44: "Usun drzewo" na karcie
// drzewa (dwa kroki, blokada bez polaczenia). Etap 45: numer umowy i cel opracowania w oknie edycji projektu.
// Etap 46: okno "Niezapisane zmiany" po kliknieciu w link z karty drzewa. Wymaga uruchomionej aplikacji z wylaczonym potwierdzaniem e-maila
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

    // --- Etap 45: numer umowy i cel opracowania w projekcie ---
    await navigate(`/projects/${project.id}`);
    await clickButton('Edytuj projekt');
    await waitFor('okno edycji projektu', 'return !!document.querySelector(".modal")');
    await typeIntoLabeled('Numer umowy', 'UM/45/2026');
    await typeIntoLabeled('Cel opracowania', 'Inwentaryzacja przed przebudową alei');
    await clickButton('Zapisz');
    await waitFor('zamkniecie okna', 'return !document.querySelector(".modal")');
    check('Naglowek projektu pokazuje numer umowy', (await bodyText()).includes('Umowa: UM/45/2026'));
    const savedProject = await api(`/api/projects/${project.id}`);
    check('Numer umowy i cel opracowania zapisane na serwerze',
      savedProject.contractNumber === 'UM/45/2026' && savedProject.documentPurpose === 'Inwentaryzacja przed przebudową alei');

    // --- Etap 46: niezapisane zmiany przy kliknieciu w link ---
    await navigate(`/projects/${project.id}/trees/${treeToKeep.id}`);
    await waitFor('formularz drzewa', 'return !!document.querySelector(".tree-field-form")');
    await typeIntoLabeled('Nazwa polska', 'lipa drobnolistna');
    await waitFor('znacznik niezapisanych zmian', 'return document.body.innerText.includes("Masz niezapisane zmiany.")');
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno niezapisanych zmian', 'return document.querySelector(".modal h2")?.textContent === "Niezapisane zmiany"');
    check('Link z niezapisanymi zmianami -> okno "Niezapisane zmiany"', true);
    await clickButton('Zostań');
    check('"Zostań" zostawia karte drzewa i wpisany tekst',
      !(await evaluate('return !!document.querySelector(".modal")')) && (await evaluate('return location.pathname')) === `/projects/${project.id}/trees/${treeToKeep.id}`
      && await evaluate('return [...document.querySelectorAll("input")].some((input) => input.value === "lipa drobnolistna")'));
    await clickButton('← Wróć do listy drzew');
    await waitFor('okno niezapisanych zmian', 'return !!document.querySelector(".modal")');
    await clickButton('Zapisz');
    await waitFor('lista drzew po zapisie', `return location.pathname === "/projects/${project.id}/trees"`);
    check('"Zapisz" zapisuje drzewo i przechodzi do listy',
      (await api(`/api/projects/${project.id}/trees/${treeToKeep.id}`)).speciesPolish === 'lipa drobnolistna');

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
