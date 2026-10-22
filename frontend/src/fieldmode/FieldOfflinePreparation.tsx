import { useEffect, useRef, useState } from 'react';
import { formatTimestampForDisplay } from '../formatting/formatTimestampForDisplay';
import { mapSymbolsApi } from '../map/mapSymbolsApi';
import { offlineService } from '../offline/offlineService';
import { photoContentCacheKey } from '../offline/photoContentCache';
import { useOfflineState } from '../offline/useOfflineState';
import { projectsApi } from '../pages/projects/projectsApi';
import { referenceLayersApi } from '../pages/projects/referencelayers/referenceLayersApi';
import { treeStandsApi } from '../pages/projects/stands/treeStandsApi';
import { treeMarkingsApi } from '../pages/trees/markings/treeMarkingsApi';
import { treePhotosApi } from '../pages/trees/photos/treePhotosApi';
import type { Tree } from '../pages/trees/treeDataTypes';
import { treesApi } from '../pages/trees/treesApi';
import { treeValuationsApi } from '../pages/trees/valuation/treeValuationsApi';
import { treeWorksApi } from '../pages/trees/works/treeWorksApi';
import { browserStorageOrNull } from './fieldModePreference';
import {
  describePreparationSummary, describeStorageEstimate, LARGE_PROJECT_TREE_COUNT, MAX_CONCURRENT_REQUESTS, planDetailTasks,
  planPhotoContentTasks, PROJECT_LEVEL_TASKS, readLastPreparationTime, readPreparePhotosPreference, runTasksWithConcurrency,
  saveLastPreparationTime, savePreparePhotosPreference, TREE_DETAIL_KINDS, type OfflinePreparationTask, type PhotoPreparationSummary,
  type PreparablePhoto,
} from './offlinePreparation';

type PreparationState =
  | { status: 'idle' }
  | { status: 'running'; doneCount: number; totalCount: number }
  | { status: 'finished'; message: string }
  | { status: 'cancelled' };

/**
 * "Przygotuj projekt do pracy bez zasiegu" (etap 32) w arkuszu "Warstwy": pobiera przez istniejace funkcje *Api
 * wszystko, czego uzywa tryb terenowy (trafia to do pamieci urzadzenia przez httpClient), z paskiem postepu
 * i anulowaniem. Bez podkladu mapy. Etap 40: listy zdjec drzew zawsze, tresc zdjec na zyczenie ("takze zdjecia",
 * w limicie miejsca na urzadzeniu).
 */
export function FieldOfflinePreparation({ projectId, trees }: { projectId: number; trees: Tree[] }) {
  const { isOnline } = useOfflineState();
  const [preparationState, setPreparationState] = useState<PreparationState>({ status: 'idle' });
  const [lastPreparedAt, setLastPreparedAt] = useState(() => readLastPreparationTime(browserStorageOrNull(), projectId));
  const isCancelledRef = useRef(false);
  const [storageDescription, setStorageDescription] = useState<{ text: string; isLow: boolean } | null>(null);
  const [isStoragePersistent, setIsStoragePersistent] = useState<boolean | null>(null);
  const [preparesPhotos, setPreparesPhotos] = useState(() => readPreparePhotosPreference(browserStorageOrNull()));
  const changePreparesPhotos = (checked: boolean) => {
    setPreparesPhotos(checked);
    savePreparePhotosPreference(browserStorageOrNull(), checked);
  };

  // Etap 33: zdjecia offline zajmuja miejsce - pokazujemy wolne miejsce i sprawdzamy, czy przegladarka nie usunie
  // danych aplikacji przy braku miejsca (trwale przechowywanie).
  useEffect(() => {
    const storageManager = typeof navigator !== 'undefined' ? navigator.storage : undefined;
    if (!storageManager?.estimate) return;
    storageManager.estimate().then((estimate) => setStorageDescription(describeStorageEstimate(estimate))).catch(() => {});
    storageManager.persisted?.().then(setIsStoragePersistent).catch(() => {});
  }, [preparationState.status]);

  const runTask = async (task: OfflinePreparationTask): Promise<unknown> => {
    switch (task.kind) {
      case 'PROJECTS': return projectsApi.listProjectsOfCurrentUser();
      case 'PROJECT': return projectsApi.getProjectOfCurrentUser(projectId);
      case 'TREES': return treesApi.listTreesOfProject(projectId);
      case 'PROJECT_WORKS': return treeWorksApi.listProjectWorks(projectId);
      case 'PROJECT_MARKINGS': return treeMarkingsApi.listProjectMarkings(projectId);
      case 'MARKING_CATALOG': return treeMarkingsApi.getCatalog();
      case 'SYMBOL_CATALOG': return mapSymbolsApi.getMapSymbols();
      case 'VALUATIONS': return treeValuationsApi.listProjectValuations(projectId);
      case 'STANDS': return treeStandsApi.listStands(projectId);
      case 'REFERENCE_LAYERS': return referenceLayersApi.listLayers(projectId);
      case 'REFERENCE_LAYER_GEOJSON': return referenceLayersApi.getLayerGeoJson(projectId, task.layerId);
      case 'TREE': return treesApi.getTreeOfProject(projectId, task.treeId);
      case 'TREE_MARKINGS': return treeMarkingsApi.getTreeMarkings(projectId, task.treeId);
      case 'TREE_WORKS': return treeWorksApi.listTreeWorks(projectId, task.treeId);
      case 'TREE_PHOTOS': return treePhotosApi.listTreePhotos(projectId, task.treeId);
      case 'PROJECT_PHOTOS': return treePhotosApi.listProjectPhotos(projectId);
      case 'PHOTO_CONTENT': return offlineService.loadPhotoContent(
        { key: photoContentCacheKey(projectId, task.treeId, task.photoId, 'documentation'), projectId, contentVersion: task.contentVersion },
        () => treePhotosApi.getTreePhotoDocumentationContent(projectId, task.treeId, task.photoId));
    }
  };

  const prepare = async () => {
    isCancelledRef.current = false;
    const projectLevelTasks: OfflinePreparationTask[] = preparesPhotos ? [...PROJECT_LEVEL_TASKS, { kind: 'PROJECT_PHOTOS' }] : PROJECT_LEVEL_TASKS;
    // Szacunek do paska postepu: dane projektu + zapytania na drzewo (warstwy i zdjecia dochodza po etapie 1).
    let totalCount = projectLevelTasks.length + trees.length * TREE_DETAIL_KINDS.length;
    setPreparationState({ status: 'running', doneCount: 0, totalCount });
    let treeIds = trees.filter((tree) => tree.id > 0).map((tree) => tree.id);
    let layerIds: number[] = [];
    let projectPhotos: PreparablePhoto[] = [];
    const firstStage = await runTasksWithConcurrency(projectLevelTasks, MAX_CONCURRENT_REQUESTS, async (task) => {
      const result = await runTask(task);
      // Swieze listy z serwera wyznaczaja drzewa i warstwy do pobrania (drzewa utworzone offline juz sa na urzadzeniu).
      if (task.kind === 'TREES') treeIds = (result as Tree[]).filter((tree) => tree.id > 0).map((tree) => tree.id);
      if (task.kind === 'REFERENCE_LAYERS') layerIds = (result as { id: number }[]).map((layer) => layer.id);
      if (task.kind === 'PROJECT_PHOTOS') projectPhotos = result as PreparablePhoto[];
    }, { isCancelled: () => isCancelledRef.current,
      onProgress: (doneCount) => setPreparationState({ status: 'running', doneCount, totalCount }) });
    if (firstStage.isCancelled) {
      setPreparationState({ status: 'cancelled' });
      return;
    }
    const detailTasks = planDetailTasks(treeIds, layerIds);
    const photoPlan = planPhotoContentTasks(projectPhotos);
    totalCount = projectLevelTasks.length + detailTasks.length + photoPlan.tasks.length;
    const secondStage = await runTasksWithConcurrency(detailTasks, MAX_CONCURRENT_REQUESTS, runTask, {
      isCancelled: () => isCancelledRef.current,
      onProgress: (doneCount) => setPreparationState({ status: 'running', doneCount: projectLevelTasks.length + doneCount, totalCount }),
    });
    if (secondStage.isCancelled) {
      setPreparationState({ status: 'cancelled' });
      return;
    }
    // Etap 3 (etap 40): tresc zdjec - na koncu, zeby dane drzew byly gotowe, nawet gdy zdjecia potrwaja.
    let downloadedBytes = 0;
    const thirdStage = await runTasksWithConcurrency(photoPlan.tasks, MAX_CONCURRENT_REQUESTS, async (task) => {
      downloadedBytes += ((await runTask(task)) as Blob).size;
    }, {
      isCancelled: () => isCancelledRef.current,
      onProgress: (doneCount) => setPreparationState({
        status: 'running', doneCount: projectLevelTasks.length + detailTasks.length + doneCount, totalCount }),
    });
    if (thirdStage.isCancelled) {
      setPreparationState({ status: 'cancelled' });
      return;
    }
    const photoSummary: PhotoPreparationSummary | null = preparesPhotos
      ? { downloadedCount: thirdStage.completedCount, downloadedBytes, skippedCount: photoPlan.skippedCount }
      : null;
    // Prosba o trwale przechowywanie (zdjecia i zmiany offline nie znikna przy braku miejsca); przegladarka moze odmowic.
    await navigator.storage?.persist?.().then(setIsStoragePersistent).catch(() => {});
    const preparedAt = new Date().toISOString();
    saveLastPreparationTime(browserStorageOrNull(), projectId, preparedAt);
    setLastPreparedAt(preparedAt);
    setPreparationState({ status: 'finished',
      message: describePreparationSummary(treeIds.length, layerIds.length,
        firstStage.failedCount + secondStage.failedCount + thirdStage.failedCount, photoSummary) });
  };

  return (
    <section aria-label="Praca bez zasięgu">
      <h3>Praca bez zasięgu</h3>
      <p className="muted small">
        Pobiera na to urządzenie dane projektu, karty, oznaczenia, prace i listy zdjęć wszystkich drzew oraz warstwy
        projektu - żeby były dostępne bez internetu. Podkład mapy nie jest pobierany; nowe zdjęcia zrobione bez zasięgu
        zapisują się na urządzeniu.
        {lastPreparedAt ? ` Ostatnio przygotowano: ${formatTimestampForDisplay(lastPreparedAt)}.` : ' Projekt nie był jeszcze przygotowany na tym urządzeniu.'}
      </p>
      {storageDescription && (
        <p className={`small${storageDescription.isLow ? '' : ' muted'}`} role={storageDescription.isLow ? 'alert' : undefined}>
          {storageDescription.text}
          {isStoragePersistent === false && ' Przeglądarka może usunąć dane aplikacji przy braku miejsca - wyślij zdjęcia, gdy tylko będzie zasięg.'}
        </p>
      )}
      {trees.length > LARGE_PROJECT_TREE_COUNT && preparationState.status !== 'running' && (
        <p className="small">Projekt ma {trees.length} drzew - pobieranie może potrwać kilka minut. Najlepiej zrobić to przez Wi-Fi.</p>
      )}
      <label className="checkbox-row small">
        <input type="checkbox" checked={preparesPhotos} disabled={preparationState.status === 'running'}
               onChange={(changeEvent) => changePreparesPhotos(changeEvent.target.checked)} />
        <span>Pobierz także wcześniej wysłane zdjęcia (podgląd bez zasięgu; zajmuje miejsce - najwyżej 300 MB na tym urządzeniu)</span>
      </label>
      {preparationState.status === 'running' ? (
        <>
          <progress className="field-progress" value={preparationState.doneCount} max={preparationState.totalCount} />
          <span className="small">Pobrano {preparationState.doneCount} z {preparationState.totalCount}</span>
          <button type="button" className="btn btn-secondary" onClick={() => { isCancelledRef.current = true; }}>Anuluj</button>
        </>
      ) : (
        <button type="button" className="btn btn-primary" onClick={() => void prepare()} disabled={!isOnline}>
          Przygotuj projekt do pracy bez zasięgu
        </button>
      )}
      {!isOnline && preparationState.status !== 'running' && (
        <p className="muted small">Wymaga połączenia z internetem.</p>
      )}
      {preparationState.status === 'finished' && <p className="small" role="status">{preparationState.message}</p>}
      {preparationState.status === 'cancelled' && <p className="small" role="status">Przerwano - część danych może nie być dostępna bez zasięgu.</p>}
    </section>
  );
}
