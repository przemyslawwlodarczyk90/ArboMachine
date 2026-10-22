import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { ToBeClarifiedNote } from '../../../components/ToBeClarifiedNote';
import { TreeMarkingBadge } from './TreeMarkingBadge';
import type { TreeMarkingDefinition } from './treeMarkingDataTypes';
import { treeMarkingsApi } from './treeMarkingsApi';
import { useTreeMarkingCatalog } from './useTreeMarkingCatalog';

type SavedMarkingsState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; codes: string[]; offlinePending: boolean };

/** Katalog pogrupowany wg kategorii w kolejnosci katalogu. */
function groupByCategory(markings: TreeMarkingDefinition[]) {
  const groups: { category: string; categoryDisplayName: string; markings: TreeMarkingDefinition[] }[] = [];
  for (const marking of markings) {
    const group = groups.find((existing) => existing.category === marking.category);
    if (group) group.markings.push(marking);
    else groups.push({ category: marking.category, categoryDisplayName: marking.categoryDisplayName, markings: [marking] });
  }
  return groups;
}

/**
 * Sekcja "Oznaczenia drzewa" karty drzewa (etap 31): wiele symboli graficznych naraz (wartosciowe, do usuniecia,
 * zlom...) - zaznaczanie w grupach i zapis kompletu (PUT .../markings, dziala tez offline). Na mapie projektu
 * oznaczenia widac w trybie "Oznaczenia drzew".
 */
export function TreeMarkingsSection({ projectId, treeId, syncGeneration = 0, onMarkingsSaved }: {
  projectId: number;
  treeId: number;
  syncGeneration?: number;
  /** etap 32 (tryb terenowy): zapisane kody - mapa zmienia wyglad drzewa od razu, bez przeladowania */
  onMarkingsSaved?: (codes: string[]) => void;
}) {
  const { catalogState, markingByCode } = useTreeMarkingCatalog();
  const [savedState, setSavedState] = useState<SavedMarkingsState>({ status: 'loading' });
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResponseStillWanted = true;
    treeMarkingsApi.getTreeMarkings(projectId, treeId)
      .then((markings) => {
        if (!isResponseStillWanted) return;
        setSavedState({ status: 'loaded', codes: markings.codes, offlinePending: !!markings.offlinePending });
        setSelectedCodes(markings.codes);
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setSavedState({ status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId, syncGeneration]);

  const savedCodes = savedState.status === 'loaded' ? savedState.codes : [];
  const hasChanges = savedState.status === 'loaded'
    && (selectedCodes.length !== savedCodes.length || selectedCodes.some((code) => !savedCodes.includes(code)));

  const toggleCode = (code: string) => setSelectedCodes((codes) =>
    (codes.includes(code) ? codes.filter((selected) => selected !== code) : [...codes, code]));

  const saveMarkings = async () => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const saved = await treeMarkingsApi.setTreeMarkings(projectId, treeId, selectedCodes);
      setSavedState({ status: 'loaded', codes: saved.codes, offlinePending: !!saved.offlinePending });
      setSelectedCodes(saved.codes);
      onMarkingsSaved?.(saved.codes);
    } catch (savingError) {
      setErrorMessage(`Nie udało się zapisać oznaczeń. ${savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <section className="tree-card-section" aria-labelledby="tree-markings-heading">
      <div className="section-header-row">
        <h3 id="tree-markings-heading">Oznaczenia drzewa</h3>
        {savedState.status === 'loaded' && savedState.offlinePending && <span className="badge">czeka na synchronizację</span>}
      </div>
      <p className="muted small">
        Dodatkowe symbole przy punkcie drzewa na mapie (widok „Oznaczenia drzew”). Można zaznaczyć kilka naraz.
      </p>
      {savedState.status === 'loaded' && savedCodes.length > 0 && (
        <div className="tree-marking-list" aria-label="Zapisane oznaczenia">
          {savedCodes.map((code) => markingByCode.get(code)).filter((marking) => marking !== undefined)
            .map((marking) => <TreeMarkingBadge key={marking.code} marking={marking} withName />)}
        </div>
      )}
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      {(catalogState.status === 'loading' || savedState.status === 'loading') && <div className="center"><LoadingSpinner /></div>}
      {catalogState.status === 'failed' && <Alert variant="error">Nie udało się pobrać listy oznaczeń. {catalogState.errorMessage}</Alert>}
      {savedState.status === 'failed' && <Alert variant="error">Nie udało się pobrać oznaczeń drzewa. {savedState.errorMessage}</Alert>}
      {catalogState.status === 'loaded' && savedState.status === 'loaded' && (
        <>
          <div className="tree-marking-groups">
            {groupByCategory(catalogState.markings).map((group) => (
              <fieldset key={group.category} className="tree-marking-group">
                <legend>{group.categoryDisplayName}</legend>
                {group.markings.map((marking) => (
                  <label key={marking.code} className="checkbox-row">
                    <input type="checkbox" checked={selectedCodes.includes(marking.code)} disabled={isSaving}
                           onChange={() => toggleCode(marking.code)} />
                    <TreeMarkingBadge marking={marking} withName />
                  </label>
                ))}
              </fieldset>
            ))}
          </div>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" disabled={isSaving || !hasChanges}
                    onClick={() => setSelectedCodes(savedCodes)}>
              Cofnij zmiany
            </button>
            <button type="button" className="btn btn-primary" disabled={isSaving || !hasChanges} onClick={saveMarkings}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz oznaczenia'}
            </button>
          </div>
        </>
      )}
      <ToBeClarifiedNote>
        Lista oznaczeń, skróty i kolory to propozycja V1. Potrzebna lista arborysty (własna symbolika?) oraz decyzja,
        czy np. „wartościowe” i „do usunięcia” mają wynikać automatycznie z waloryzacji i zaplanowanych prac.
      </ToBeClarifiedNote>
    </section>
  );
}
