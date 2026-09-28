// Usuwa poprzedni build frontendu z src/main/resources/static przed nowym "npm run build".
// Kasuje TYLKO pliki, ktore produkuje Vite (index.html, assets/, zawartosc public/) - inne pliki w static
// (np. nagranie testow Postmana) zostaja nietkniete, dlatego vite.config ma emptyOutDir: false.
import { readdirSync, rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const backendStaticDirectory = resolve(frontendDirectory, '../src/main/resources/static');

const generatedEntries = ['index.html', 'assets', ...readdirSync(resolve(frontendDirectory, 'public'))];
for (const generatedEntry of generatedEntries) {
  rmSync(resolve(backendStaticDirectory, generatedEntry), { recursive: true, force: true });
}
