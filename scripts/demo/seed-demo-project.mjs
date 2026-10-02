// Projekt demonstracyjny ArboMachine przez API (Node.js 20+, bez zaleznosci) - do pokazu MVP arborystom.
// Konto musi juz istniec (rejestracja w aplikacji). Uzycie:
//   node scripts/demo/seed-demo-project.mjs --base http://localhost:8083 --email arborysta@example.com --password ****
//        [--sheet inwentaryzacja.xlsx] [--drawing rysunek.dxf]
// Bez --sheet: 8 przykladowych drzew w parku (WGS 84) + jedna grupa drzew. Z --sheet / --drawing: import prawdziwych
// plikow (arkusz drzew, obszary z rysunku). Zawsze: waloryzacja, prace z terminami, kontrole stanu (jedna "zla").
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';

const options = Object.fromEntries(process.argv.slice(2).reduce((pairs, value, index, all) =>
  (value.startsWith('--') ? [...pairs, [value.slice(2), all[index + 1]]] : pairs), []));
const BASE = (options.base ?? 'http://localhost:8083').replace(/\/$/, '');
if (!options.email || !options.password) {
  console.error('Podaj --email i --password istniejacego konta arborysty.');
  process.exit(1);
}

async function api(path, { method = 'GET', body, form } = {}) {
  const response = await fetch(BASE + path, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(api.token ? { Authorization: `Bearer ${api.token}` } : {}) },
    body: form ?? (body ? JSON.stringify(body) : undefined),
  });
  const text = await response.text();
  const parsed = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!response.ok) throw new Error(`${method} ${path} -> HTTP ${response.status}: ${typeof parsed === 'string' ? parsed : JSON.stringify(parsed)}`);
  return parsed;
}

const upload = (path, filePath) => {
  const form = new FormData();
  form.append('file', new Blob([readFileSync(filePath)]), basename(filePath));
  return api(path, { method: 'POST', form });
};

const isoDate = (daysFromToday) => {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

api.token = (await api('/api/users/login', { method: 'POST', body: { email: options.email, password: options.password } })).accessToken;
const project = await api('/api/projects', { method: 'POST', body: {
  name: 'Projekt demonstracyjny - park', description: 'Dane przykładowe do pokazu ArboMachine.',
  clientName: 'Gmina (przykład)', siteAddress: 'Park miejski, ul. Parkowa', orderDate: isoDate(-14),
} });
console.log(`Projekt ${project.id}: ${project.name}`);

if (options.sheet) {
  const result = await upload(`/api/projects/${project.id}/imports/trees`, options.sheet);
  console.log(`Zaimportowano drzewa z arkusza: ${result.importedTreeCount}`);
} else {
  const samples = [
    ['1', 'lipa drobnolistna', 'Tilia cordata', 182, 18, 9, 52.2297, 21.0122],
    ['2', 'dąb szypułkowy', 'Quercus robur', 310, 22, 14, 52.2299, 21.0127],
    ['3', 'sosna zwyczajna', 'Pinus sylvestris', 145, 20, 6, 52.2301, 21.0119],
    ['4', 'klon zwyczajny', 'Acer platanoides', 120, 15, 8, 52.2295, 21.0131],
    ['5', 'kasztanowiec zwyczajny', 'Aesculus hippocastanum', 240, 16, 11, 52.2293, 21.0124],
    ['6', 'lipa drobnolistna', 'Tilia cordata', 96, 12, 6, 52.2298, 21.0135],
    ['7', 'świerk pospolity', 'Picea abies', 110, 19, 5, 52.2303, 21.0127],
    ['8', 'grab pospolity', 'Carpinus betulus', 88, 11, 6, 52.2291, 21.0117],
  ];
  for (const [inventoryNumber, speciesPolish, speciesLatin, trunkCircumferenceCm, heightM, crownDiameterM, latitude, longitude] of samples) {
    const tree = await api(`/api/projects/${project.id}/trees`, { method: 'POST', body: {
      inventoryNumber, speciesPolish, speciesLatin, trunkCircumferenceCm, heightM, crownDiameterM } });
    await api(`/api/projects/${project.id}/trees/${tree.id}/location`, { method: 'PUT', body: { latitude, longitude } });
  }
  await api(`/api/projects/${project.id}/stands`, { method: 'POST', body: {
    name: 'Aleja lipowa', notes: 'Przykładowa grupa drzew.', assignTreesInside: true, treeIds: [],
    geometry: { type: 'Polygon', coordinates: [[[21.0118, 52.2294], [21.0128, 52.2294], [21.0128, 52.2300], [21.0118, 52.2300], [21.0118, 52.2294]]] },
  } });
  console.log('Dodano 8 przykładowych drzew i grupę "Aleja lipowa".');
}
if (options.drawing) {
  const result = await upload(`/api/projects/${project.id}/imports/stands`, options.drawing);
  console.log(`Zaimportowano grupy drzew z rysunku: ${result.importedStandCount} (przypisane drzewa: ${result.assignedTreeCount})`);
}

const trees = await api(`/api/projects/${project.id}/trees`);
const [first, second, third] = trees;
const worksPath = (tree) => `/api/projects/${project.id}/trees/${tree.id}/works`;
await api(`/api/projects/${project.id}/trees/${first.id}/valuation`, { method: 'PUT', body: {
  overallCategory: 'HIGH', ageValue: 'Okazały, stary egzemplarz.', landscapeValue: 'Dominanta widoczna z głównej alei.' } });
await api(worksPath(first), { method: 'POST', body: { name: 'Cięcia sanitarne', description: 'Usunięcie posuszu nad alejką.',
  status: 'PLANNED', priority: 'HIGH', plannedDate: isoDate(1) } });
await api(worksPath(second), { method: 'POST', body: { name: 'Kontrola stanu', status: 'RECOMMENDED', plannedDate: isoDate(5) } });
await api(worksPath(third), { method: 'POST', body: { name: 'Wiązanie korony', status: 'COMPLETED', completedDate: isoDate(-3) } });
const monitoringPath = (tree) => `/api/projects/${project.id}/trees/${tree.id}/monitoring`;
await api(monitoringPath(first), { method: 'POST', body: { inspectionDate: isoDate(-30), healthStatus: 'GOOD', notes: 'Bez zmian.' } });
await api(monitoringPath(second), { method: 'POST', body: { inspectionDate: isoDate(-1), healthStatus: 'POOR', notes: 'Pęknięcie konaru nad ścieżką.' } });
console.log('Dodano waloryzację, prace (jutro, za 5 dni, wykonana) i kontrole stanu (jedna z oceną "Zły" - powiadomienie).');
console.log(`Gotowe: ${BASE}/projects/${project.id}`);
