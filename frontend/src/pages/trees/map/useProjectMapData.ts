import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { useMapSymbolCatalog } from '../../../map/useMapSymbolCatalog';
import { useOfflineState } from '../../../offline/useOfflineState';
import type { TreeStand } from '../../projects/stands/standDataTypes';
import { treeStandsApi } from '../../projects/stands/treeStandsApi';
import { useProjectReferenceLayers } from '../../projects/referencelayers/useProjectReferenceLayers';
import type { TreeMarkings } from '../markings/treeMarkingDataTypes';
import { treeMarkingsApi } from '../markings/treeMarkingsApi';
import { useTreeMarkingCatalog } from '../markings/useTreeMarkingCatalog';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import { treeValuationsApi } from '../valuation/treeValuationsApi';
import type { TreeValuation } from '../valuation/valuationDataTypes';
import { valuationCategoryByTreeId } from '../valuation/valuationFilter';
import type { TreeWork } from '../works/treeWorkDataTypes';
import { treeWorksApi } from '../works/treeWorksApi';
import { toTreeMapPoints } from './treeMapPoints';

export type ProjectMapDataState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; trees: Tree[]; works: TreeWork[] };

/**
 * Dane mapy projektu (etap 32 - wspolne dla mapy biurowej i trybu terenowego): drzewa i prace projektu (bez nich
 * mapa sie nie pokaze), a obok - osobne zapytania, ktorych blad NIE blokuje mapy: waloryzacje (null = blad),
 * oznaczenia (null = blad), grupy drzew (blad = brak warstwy). Do tego katalogi symboli i oznaczen oraz warstwy
 * referencyjne. Laczenie w punkty mapy w pamieci po treeId (toTreeMapPoints).
 * Lokalne aktualizacje (upsertTree, replaceTreeMarkings, replaceTreeWorks) zmieniaja mape od razu po zapisie
 * w panelu drzewa - bez ponownego pobierania calosci; dzialaja tez offline (kolejka zwraca obiekty).
 * Po synchronizacji zmian offline (syncGeneration) dane sa pobierane ponownie, bez migniecia ekranu ladowania.
 */
export function useProjectMapData(projectId: number | null) {
  const [mapDataState, setMapDataState] = useState<ProjectMapDataState>({ status: 'loading' });
  const [valuations, setValuations] = useState<TreeValuation[] | null>(null);
  const [projectMarkings, setProjectMarkings] = useState<TreeMarkings[] | null>(null);
  const [stands, setStands] = useState<TreeStand[]>([]);
  const [reloadNumber, setReloadNumber] = useState(0);
  const { syncGeneration } = useOfflineState();
  const symbolCatalog = useMapSymbolCatalog();
  const markingCatalog = useTreeMarkingCatalog();
  const referenceLayers = useProjectReferenceLayers(projectId ?? 0);

  // Zmiana projektu = ekran ladowania; ponowne pobranie (synchronizacja, reload) zostawia stare dane do odpowiedzi.
  useEffect(() => { setMapDataState({ status: 'loading' }); }, [projectId]);

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    treeValuationsApi.listProjectValuations(projectId)
      .then((loadedValuations) => { if (isResponseStillWanted) setValuations(loadedValuations); })
      .catch(() => { if (isResponseStillWanted) setValuations(null); });
    treeMarkingsApi.listProjectMarkings(projectId)
      .then((loadedMarkings) => { if (isResponseStillWanted) setProjectMarkings(loadedMarkings); })
      .catch(() => { if (isResponseStillWanted) setProjectMarkings(null); });
    treeStandsApi.listStands(projectId)
      .then((loadedStands) => { if (isResponseStillWanted) setStands(loadedStands); })
      .catch(() => { if (isResponseStillWanted) setStands([]); });
    Promise.all([treesApi.listTreesOfProject(projectId), treeWorksApi.listProjectWorks(projectId)])
      .then(([trees, works]) => {
        if (isResponseStillWanted) setMapDataState({ status: 'loaded', trees, works });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setMapDataState((currentState) => (currentState.status === 'loaded' ? currentState : {
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        }));
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, syncGeneration, reloadNumber]);

  const loadedData = mapDataState.status === 'loaded' ? mapDataState : null;
  // Stabilne tablice - mapa dopasowuje widok tylko wtedy, gdy zmieniaja sie dane projektu.
  const allTreeMapPoints = useMemo(
    () => (loadedData ? toTreeMapPoints(loadedData.trees, loadedData.works, valuationCategoryByTreeId(valuations ?? []),
      new Map((projectMarkings ?? []).map((markings) => [markings.treeId, markings.codes]))) : []),
    [loadedData, valuations, projectMarkings]);

  /** Drzewo po zapisie (formularz, lokalizacja) albo nowe drzewo - podmiana albo dopisanie na koncu listy. */
  const upsertTree = useCallback((tree: Tree) => setMapDataState((currentState) => {
    if (currentState.status !== 'loaded') return currentState;
    const exists = currentState.trees.some((existing) => existing.id === tree.id);
    return { ...currentState, trees: exists
      ? currentState.trees.map((existing) => (existing.id === tree.id ? tree : existing))
      : [...currentState.trees, tree] };
  }), []);

  /** Drzewo utworzone offline dostalo prawdziwe id po synchronizacji. */
  const replaceTreeId = useCallback((localTreeId: number, serverTreeId: number) => {
    setMapDataState((currentState) => (currentState.status === 'loaded' ? { ...currentState,
      trees: currentState.trees.map((tree) => (tree.id === localTreeId ? { ...tree, id: serverTreeId } : tree)),
      works: currentState.works.map((work) => (work.treeId === localTreeId ? { ...work, treeId: serverTreeId } : work)) }
      : currentState));
    setProjectMarkings((current) => current?.map((markings) => (markings.treeId === localTreeId
      ? { ...markings, treeId: serverTreeId } : markings)) ?? current);
  }, []);

  // Blad pobrania oznaczen (null) zostaje bledem - jedno zapisane drzewo nie udaje kompletu oznaczen projektu.
  const replaceTreeMarkings = useCallback((treeId: number, codes: string[]) => setProjectMarkings((current) => (current === null
    ? current : [...current.filter((markings) => markings.treeId !== treeId), { treeId, codes }])), []);

  const replaceTreeWorks = useCallback((treeId: number, works: TreeWork[]) => setMapDataState((currentState) =>
    (currentState.status === 'loaded'
      ? { ...currentState, works: [...currentState.works.filter((work) => work.treeId !== treeId), ...works] }
      : currentState)), []);

  const reload = useCallback(() => setReloadNumber((number) => number + 1), []);

  return {
    mapDataState, valuations, projectMarkings, stands, allTreeMapPoints, symbolCatalog, markingCatalog, referenceLayers,
    upsertTree, replaceTreeId, replaceTreeMarkings, replaceTreeWorks, reload,
  };
}
