// Etap 32: przelaczanie widok biurowy <-> tryb terenowy z zachowaniem kontekstu (projekt, wybrane drzewo).
// Modul bez React - testowany przez node --test. Parametr ?drzewo= jest tym samym, ktorego uzywa mapa biurowa
// ("Pokaz na mapie projektu").
import { parsePositiveIdRouteParam } from '../routing/parsePositiveIdRouteParam.ts';
import { parseTreeIdRouteParam } from '../routing/parseTreeIdRouteParam.ts';

export const FIELD_MODE_HOME_PATH = '/field';
export const OFFICE_MODE_HOME_PATH = '/projects';
/** Nazwa parametru adresu z wybranym drzewem (wspolna z mapa biurowa). */
export const SELECTED_TREE_SEARCH_PARAM = 'drzewo';

/** Wybrane drzewo z ?drzewo=; ujemne id (drzewo utworzone offline) przechodzi bez zmian, reszta = null. */
export function selectedTreeIdOf(search: string): number | null {
  return parseTreeIdRouteParam(new URLSearchParams(search).get(SELECTED_TREE_SEARCH_PARAM) ?? undefined);
}

export const fieldProjectPath = (projectId: number, treeId: number | null = null) =>
  `${FIELD_MODE_HOME_PATH}/projects/${projectId}${treeId === null ? '' : `?${SELECTED_TREE_SEARCH_PARAM}=${treeId}`}`;

/** Odcinki sciezki bez pustych (ukosnik na koncu nie zmienia znaczenia adresu). */
const segmentsOf = (pathname: string) => pathname.split('/').filter(Boolean);

/**
 * Adres widoku biurowego -> adres trybu terenowego: karta drzewa i mapa z ?drzewo= -> mapa terenowa z otwartym
 * drzewem, kazda inna podstrona projektu -> mapa terenowa projektu, wszystko inne (profil, lista) -> lista projektow.
 */
export function toFieldModePath(officePathname: string, search = ''): string {
  const [first, projectIdText, subpage, treeIdText] = segmentsOf(officePathname);
  if (first !== 'projects') return FIELD_MODE_HOME_PATH;
  const projectId = parsePositiveIdRouteParam(projectIdText);
  if (projectId === null) return FIELD_MODE_HOME_PATH;
  if (subpage === 'trees' && treeIdText !== undefined) {
    return fieldProjectPath(projectId, parseTreeIdRouteParam(treeIdText));
  }
  if (subpage === 'map') return fieldProjectPath(projectId, selectedTreeIdOf(search));
  return fieldProjectPath(projectId);
}

/**
 * Adres trybu terenowego -> widok biurowy: wybrane drzewo -> pelna karta drzewa, sam projekt -> mapa projektu,
 * lista i nieznane adresy -> lista projektow.
 */
export function toOfficeModePath(fieldPathname: string, search = ''): string {
  const [first, projects, projectIdText, ...rest] = segmentsOf(fieldPathname);
  if (first !== 'field' || projects !== 'projects' || rest.length > 0) return OFFICE_MODE_HOME_PATH;
  const projectId = parsePositiveIdRouteParam(projectIdText);
  if (projectId === null) return OFFICE_MODE_HOME_PATH;
  const treeId = selectedTreeIdOf(search);
  return treeId === null ? `/projects/${projectId}/map` : `/projects/${projectId}/trees/${treeId}`;
}
