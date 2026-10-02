import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../../components/LabeledTextAreaField';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import { toTreeMapPoints } from '../../trees/map/treeMapPoints';
import type { Tree } from '../../trees/treeDataTypes';
import { treesApi } from '../../trees/treesApi';
import type { StandPolygonGeometry, TreeStand } from './standDataTypes';
import { StandEditorMap } from './StandEditorMap';
import { formatStandArea } from './standGeometry';
import { treeStandsApi } from './treeStandsApi';

type PageState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; stands: TreeStand[]; trees: Tree[] };

interface EditorState {
  standId: number | null;
  name: string;
  notes: string;
  geometry: StandPolygonGeometry | null;
  initialGeometry: StandPolygonGeometry | null;
  treeIds: Set<number>;
  assignTreesInside: boolean;
  redrawToken: number;
}

const errorText = (error: unknown) => (error instanceof ApiRequestError ? error.message : 'Nieoczekiwany błąd.');

/**
 * Sekcja "Grupy drzew" projektu (/projects/:projectId/stands, etap 29): obszary (wielokaty) z przypisanymi drzewami -
 * szpalery, zadrzewienia, skupiska krzewow. Rysowanie obszaru na mapie, nazwa i uwagi, wybor drzew z listy albo
 * "drzewa w obszarze". Import obszarow z plikow (DXF, SHP, GeoJSON, GeoPackage) - w "Import i eksport".
 */
export function ProjectStandsPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [pageState, setPageState] = useState<PageState>({ status: 'loading' });
  const [editor, setEditor] = useState<EditorState | null>(null);
  const [isWorking, setIsWorking] = useState(false);
  const [message, setMessage] = useState<{ variant: 'success' | 'error'; text: string } | null>(null);
  const [standAwaitingDeletion, setStandAwaitingDeletion] = useState<TreeStand | null>(null);

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    Promise.all([treeStandsApi.listStands(projectId), treesApi.listTreesOfProject(projectId)])
      .then(([stands, trees]) => { if (isResponseStillWanted) setPageState({ status: 'loaded', stands, trees }); })
      .catch((loadingError) => { if (isResponseStillWanted) setPageState({ status: 'failed', errorMessage: errorText(loadingError) }); });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const loaded = pageState.status === 'loaded' ? pageState : null;
  const treeMapPoints = useMemo(() => (loaded ? toTreeMapPoints(loaded.trees) : []), [loaded]);
  const numberOfTree = useMemo(() => new Map((loaded?.trees ?? []).map((tree) => [tree.id, tree.inventoryNumber])), [loaded]);

  if (projectId === null) return null;
  if (pageState.status === 'loading') return <div className="center"><LoadingSpinner /></div>;
  if (pageState.status === 'failed') {
    return (
      <div className="stack">
        <h2>Grupy drzew</h2>
        <Alert variant="error">Nie udało się wczytać grup drzew. {pageState.errorMessage}</Alert>
      </div>
    );
  }

  const replaceStands = (change: (stands: TreeStand[]) => TreeStand[]) => setPageState((current) => (current.status === 'loaded'
    ? { ...current, stands: change(current.stands).sort((first, second) => first.name.localeCompare(second.name, 'pl')) } : current));

  const startEditing = (stand: TreeStand | null) => {
    setMessage(null);
    setStandAwaitingDeletion(null);
    setEditor({
      standId: stand?.id ?? null, name: stand?.name ?? '', notes: stand?.notes ?? '', geometry: stand?.geometry ?? null,
      initialGeometry: stand?.geometry ?? null, treeIds: new Set(stand?.treeIds ?? []), assignTreesInside: stand === null, redrawToken: 0,
    });
  };

  const submit = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (!editor || isWorking) return;
    if (!editor.name.trim()) {
      setMessage({ variant: 'error', text: 'Podaj nazwę grupy drzew.' });
      return;
    }
    if (!editor.geometry) {
      setMessage({ variant: 'error', text: 'Narysuj obszar grupy na mapie (co najmniej 3 punkty).' });
      return;
    }
    setIsWorking(true);
    setMessage(null);
    try {
      const request = { name: editor.name.trim(), notes: editor.notes.trim() || undefined, geometry: editor.geometry,
        treeIds: [...editor.treeIds], assignTreesInside: editor.assignTreesInside };
      const saved = editor.standId === null
        ? await treeStandsApi.createStand(projectId, request)
        : await treeStandsApi.updateStand(projectId, editor.standId, request);
      replaceStands((stands) => [...stands.filter((stand) => stand.id !== saved.id), saved]);
      setMessage({ variant: 'success', text: `Zapisano grupę „${saved.name}” (drzewa: ${saved.treeIds.length}).` });
      setEditor(null);
    } catch (savingError) {
      setMessage({ variant: 'error', text: `Nie udało się zapisać grupy drzew. ${errorText(savingError)}` });
    } finally {
      setIsWorking(false);
    }
  };

  const assignInside = async (stand: TreeStand) => {
    setIsWorking(true);
    setMessage(null);
    try {
      const updated = await treeStandsApi.assignTreesInside(projectId, stand.id);
      replaceStands((stands) => stands.map((item) => (item.id === updated.id ? updated : item)));
      setMessage({ variant: 'success', text: `Grupa „${updated.name}”: przypisane drzewa ${updated.treeIds.length} (dodano ${updated.treeIds.length - stand.treeIds.length}).` });
    } catch (assignError) {
      setMessage({ variant: 'error', text: `Nie udało się przypisać drzew. ${errorText(assignError)}` });
    } finally {
      setIsWorking(false);
    }
  };

  const deleteStand = async (stand: TreeStand) => {
    setIsWorking(true);
    try {
      await treeStandsApi.deleteStand(projectId, stand.id);
      replaceStands((stands) => stands.filter((item) => item.id !== stand.id));
      setMessage({ variant: 'success', text: `Usunięto grupę „${stand.name}”. Drzewa zostały w projekcie.` });
      setStandAwaitingDeletion(null);
    } catch (deletionError) {
      setMessage({ variant: 'error', text: `Nie udało się usunąć grupy. ${errorText(deletionError)}` });
    } finally {
      setIsWorking(false);
    }
  };

  const toggleTree = (treeId: number) => setEditor((current) => {
    if (!current) return current;
    const treeIds = new Set(current.treeIds);
    if (treeIds.has(treeId)) treeIds.delete(treeId); else treeIds.add(treeId);
    return { ...current, treeIds };
  });

  const sortedTrees = [...pageState.trees].sort((first, second) => first.inventoryNumber.localeCompare(second.inventoryNumber, 'pl', { numeric: true }));

  return (
    <div className="stack">
      <div className="section-header-row">
        <h2>Grupy drzew</h2>
        {!editor && <button type="button" className="btn btn-primary" onClick={() => startEditing(null)}>Dodaj grupę drzew</button>}
      </div>
      <p className="muted small">
        Obszar na mapie z przypisanymi drzewami - np. szpaler, zadrzewienie, skupisko krzewów. Pole powierzchni jest przybliżone.
        Obszary z plików (DXF, Shapefile, GeoJSON, GeoPackage) zaimportujesz w <Link to="../import-export" relative="path">Import i eksport</Link>.
      </p>
      {message && <Alert variant={message.variant}>{message.text}</Alert>}

      {editor && (
        <form className="form stand-editor card" onSubmit={submit} noValidate>
          <h3>{editor.standId === null ? 'Nowa grupa drzew' : 'Edycja grupy drzew'}</h3>
          <LabeledInputField label="Nazwa *" value={editor.name} maxLength={255} disabled={isWorking}
                             placeholder="np. Szpaler lip przy drodze"
                             onChange={(changeEvent) => setEditor({ ...editor, name: changeEvent.target.value })} />
          <LabeledTextAreaField label="Uwagi" value={editor.notes} rows={2} disabled={isWorking}
                                onChange={(changeEvent) => setEditor({ ...editor, notes: changeEvent.target.value })} />
          <div className="stack-tight">
            <span className="field-label">Obszar</span>
            <span className="muted small">
              {editor.geometry ? 'Przeciągnij wierzchołki, aby poprawić obszar.'
                : 'Klikaj na mapie kolejne punkty obrysu; zakończ dwuklikiem albo klikając pierwszy punkt.'}
            </span>
            <StandEditorMap trees={treeMapPoints} redrawToken={editor.redrawToken} initialGeometry={editor.initialGeometry}
                            otherStands={pageState.stands.filter((stand) => stand.id !== editor.standId)}
                            onGeometryChange={(geometry) => setEditor((current) => (current ? { ...current, geometry } : current))} />
            <div className="form-actions">
              <button type="button" className="btn btn-secondary btn-small" disabled={isWorking}
                      onClick={() => setEditor({ ...editor, geometry: null, redrawToken: editor.redrawToken + 1 })}>
                Rysuj od nowa
              </button>
            </div>
          </div>
          <label className="checkbox-row">
            <input type="checkbox" checked={editor.assignTreesInside} disabled={isWorking}
                   onChange={(changeEvent) => setEditor({ ...editor, assignTreesInside: changeEvent.target.checked })} />
            <span>Przypisz też drzewa leżące w obszarze (z lokalizacją)</span>
          </label>
          {sortedTrees.length > 0 && (
            <fieldset className="stand-tree-picker">
              <legend>Drzewa w grupie ({editor.treeIds.size})</legend>
              <div className="stand-tree-options">
                {sortedTrees.map((tree) => (
                  <label key={tree.id} className="checkbox-row">
                    <input type="checkbox" checked={editor.treeIds.has(tree.id)} disabled={isWorking} onChange={() => toggleTree(tree.id)} />
                    <span>{tree.inventoryNumber}{tree.speciesPolish ? ` · ${tree.speciesPolish}` : ''}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setEditor(null)} disabled={isWorking}>Anuluj</button>
            <button type="submit" className="btn btn-primary" disabled={isWorking}>{isWorking ? 'Zapisywanie…' : 'Zapisz grupę'}</button>
          </div>
        </form>
      )}

      {!editor && pageState.stands.length === 0 && (
        <p className="muted">W projekcie nie ma jeszcze grup drzew.</p>
      )}

      {pageState.stands.length > 0 && (
        <ul className="stand-list">
          {pageState.stands.map((stand) => (
            <li key={stand.id} className="stand-item card">
              <div className="stand-item-header">
                <strong>{stand.name}</strong>
                <span className="muted small">{formatStandArea(stand.areaSquareMeters)} · drzewa: {stand.treeIds.length}
                  {stand.source === 'IMPORT' ? ' · z importu' : ''}</span>
              </div>
              {stand.treeIds.length > 0 && (
                <span className="small">Numery: {stand.treeIds.map((treeId) => numberOfTree.get(treeId) ?? `#${treeId}`).join(', ')}</span>
              )}
              {stand.notes && <span className="muted small">{stand.notes}</span>}
              {standAwaitingDeletion?.id === stand.id ? (
                <div className="photo-annotation-confirmation" role="alert">
                  <span>Usunąć grupę „{stand.name}”? Drzewa zostaną w projekcie.</span>
                  <div className="form-actions">
                    <button type="button" className="btn btn-ghost" onClick={() => setStandAwaitingDeletion(null)} disabled={isWorking}>Anuluj</button>
                    <button type="button" className="btn btn-danger" onClick={() => void deleteStand(stand)} disabled={isWorking}>Usuń grupę</button>
                  </div>
                </div>
              ) : !editor && (
                <div className="form-actions">
                  <button type="button" className="btn btn-secondary btn-small" onClick={() => startEditing(stand)}>Edytuj</button>
                  <button type="button" className="btn btn-secondary btn-small" disabled={isWorking} onClick={() => void assignInside(stand)}>
                    Przypisz drzewa w obszarze
                  </button>
                  <button type="button" className="btn btn-danger-outline btn-small" onClick={() => setStandAwaitingDeletion(stand)}>Usuń</button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
