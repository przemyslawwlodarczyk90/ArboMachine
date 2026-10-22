import assert from 'node:assert/strict';
import { test } from 'node:test';
import { selectedTreeIdOf, toFieldModePath, toOfficeModePath } from './fieldModePaths.ts';

// Uruchamianie: npm test (node --test). Przelaczanie widok biurowy <-> tryb terenowy (etap 32).

test('biuro -> teren: lista projektow, podstrony projektu, karta drzewa i mapa z ?drzewo=', () => {
  assert.equal(toFieldModePath('/projects'), '/field');
  assert.equal(toFieldModePath('/projects/'), '/field');
  assert.equal(toFieldModePath('/projects/5'), '/field/projects/5');
  for (const subpage of ['trees', 'map', 'stands', 'management', 'documentation', 'import-export', 'integrations']) {
    assert.equal(toFieldModePath(`/projects/5/${subpage}`), '/field/projects/5', subpage);
  }
  assert.equal(toFieldModePath('/projects/5/trees/12'), '/field/projects/5?drzewo=12');
  assert.equal(toFieldModePath('/projects/5/map', '?drzewo=12'), '/field/projects/5?drzewo=12');
  assert.equal(toFieldModePath('/projects/5/map', '?inne=1&drzewo=12'), '/field/projects/5?drzewo=12');
});

test('biuro -> teren: ujemne id drzewa utworzonego offline przechodzi bez zmian', () => {
  assert.equal(toFieldModePath('/projects/5/trees/-179088095947800'), '/field/projects/5?drzewo=-179088095947800');
  assert.equal(toFieldModePath('/projects/5/map', '?drzewo=-42'), '/field/projects/5?drzewo=-42');
});

test('biuro -> teren: nieprawidlowe adresy i inne strony panelu -> lista projektow albo projekt bez drzewa', () => {
  assert.equal(toFieldModePath('/arborist/profile'), '/field');
  assert.equal(toFieldModePath('/arborist/harmonogram'), '/field');
  assert.equal(toFieldModePath('/'), '/field');
  assert.equal(toFieldModePath('/projects/abc'), '/field');
  assert.equal(toFieldModePath('/projects/-5/map'), '/field');
  assert.equal(toFieldModePath('/projects/5/trees/abc'), '/field/projects/5');
  assert.equal(toFieldModePath('/projects/5/trees/0'), '/field/projects/5');
  assert.equal(toFieldModePath('/projects/5/map', '?drzewo=x'), '/field/projects/5');
  assert.equal(toFieldModePath('/projects/5/trees', '?drzewo=12'), '/field/projects/5');
});

test('teren -> biuro: drzewo -> pelna karta, sam projekt -> mapa, lista -> projekty', () => {
  assert.equal(toOfficeModePath('/field'), '/projects');
  assert.equal(toOfficeModePath('/field/'), '/projects');
  assert.equal(toOfficeModePath('/field/projects/5'), '/projects/5/map');
  assert.equal(toOfficeModePath('/field/projects/5', '?drzewo=12'), '/projects/5/trees/12');
  assert.equal(toOfficeModePath('/field/projects/5', '?drzewo=-42'), '/projects/5/trees/-42');
});

test('teren -> biuro: nieprawidlowe adresy', () => {
  assert.equal(toOfficeModePath('/field/projects'), '/projects');
  assert.equal(toOfficeModePath('/field/projects/abc'), '/projects');
  assert.equal(toOfficeModePath('/field/projects/5/cos'), '/projects');
  assert.equal(toOfficeModePath('/projects/5'), '/projects');
  assert.equal(toOfficeModePath('/field/projects/5', '?drzewo=0'), '/projects/5/map');
  assert.equal(toOfficeModePath('/field/projects/5', '?drzewo=1.5'), '/projects/5/map');
});

test('obie strony sa odwrotne dla karty drzewa i projektu', () => {
  assert.equal(toOfficeModePath(...splitUrl(toFieldModePath('/projects/5/trees/12'))), '/projects/5/trees/12');
  assert.equal(toOfficeModePath(...splitUrl(toFieldModePath('/projects/5/map'))), '/projects/5/map');
  assert.equal(selectedTreeIdOf(''), null);
});

function splitUrl(url: string): [string, string] {
  const index = url.indexOf('?');
  return index < 0 ? [url, ''] : [url.slice(0, index), url.slice(index)];
}
