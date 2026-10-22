import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { VoiceEnabledTextAreaField } from '../../speech/VoiceEnabledTextAreaField';
import type { Tree } from './treeDataTypes';
import {
  areTreeFormValuesEquivalent, toTreeFormValues, toTreeUpdateRequest, validateTreeFormValues, type TreeFormValues,
} from './treeFormValues';
import { treesApi } from './treesApi';
import { unsavedChangesStore } from '../../fieldmode/unsavedChangesStore';

/** Sekcja formularza terenowego - naglowek + pola. Kolejnosc sekcji to uklad V1, nie potwierdzony workflow. */
function TreeFieldFormSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="tree-field-form-section">
      <legend>{title}</legend>
      {children}
    </fieldset>
  );
}

/**
 * Formularz terenowy drzewa (V1) - zawsze edytowalny na karcie drzewa, tablet-first. Sekcje w kolejnosci pracy przy
 * drzewie: Identyfikacja -> Gatunek -> Pomiary -> Stan drzewa -> Zagrozenia -> Notatki (uklad do weryfikacji
 * z arborysta, docs/DO_DOPRECYZOWANIA.md). Nic nie zapisuje sie samo: dopiero "Zapisz zmiany" wysyla PATCH, a odpowiedz
 * backendu staje sie nowym stanem. "Anuluj" przywraca zapisane wartosci bez zapytania.
 * Pomiary sa tekstem az do wyslania - mozna wpisac przecinek dziesietny (12,5), do API trafia liczba (12.5).
 */
export function TreeFieldForm({ projectId, savedTree, onTreeSaved, onUnsavedChangesChange }: {
  projectId: number;
  savedTree: Tree;
  onTreeSaved: (savedTree: Tree) => void;
  /** informacja dla karty drzewa, np. zeby nie przechodzic do nastepnego drzewa z niezapisanymi zmianami */
  onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
}) {
  const [treeFormValues, setTreeFormValues] = useState<TreeFormValues>(() => toTreeFormValues(savedTree));
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [wasJustSaved, setWasJustSaved] = useState(false);
  // Etap 37: dane drzewa zmieniono w miedzyczasie na innym urzadzeniu - zapis wstrzymany do decyzji uzytkownika.
  const [hasEditConflict, setHasEditConflict] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = !areTreeFormValuesEquivalent(treeFormValues, toTreeFormValues(savedTree));

  useEffect(() => {
    onUnsavedChangesChange?.(hasUnsavedChanges);
  }, [hasUnsavedChanges, onUnsavedChangesChange]);

  // Etap 32: niezapisane zmiany w rejestrze aplikacji - przelaczenie trybu albo inne drzewo w terenie pyta
  // "Zapisz / Odrzuc / Zostan". Zapis z rejestru to ten sam zapis co przycisk "Zapisz zmiany" (najnowsza wersja z refa).
  const saveTreeChangesRef = useRef<() => Promise<boolean>>(async () => false);
  const discardChangesRef = useRef<() => void>(() => {});
  useEffect(() => {
    const sourceId = `tree-field-form-${savedTree.id}`;
    unsavedChangesStore.setSource(sourceId, hasUnsavedChanges
      ? { save: () => saveTreeChangesRef.current(), discard: () => discardChangesRef.current() }
      : null);
    return () => unsavedChangesStore.setSource(sourceId, null);
  }, [hasUnsavedChanges, savedTree.id]);

  const changeFormField = (fieldName: keyof TreeFormValues, newValue: string) => {
    setWasJustSaved(false);
    setTreeFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));
  };
  const bindTextField = (fieldName: keyof TreeFormValues) => ({
    value: treeFormValues[fieldName],
    onChange: (changeEvent: { target: { value: string } }) => changeFormField(fieldName, changeEvent.target.value),
    errorMessage: errorMessageByFieldName[fieldName],
  });

  // Pola opisowe z dyktowaniem (etap 18) - numer, nazwa lacinska i pomiary celowo bez glosu.
  const bindVoiceTextField = (fieldName: 'conditionNotes' | 'hazardNotes' | 'notes') => ({
    value: treeFormValues[fieldName],
    onValueChange: (newValue: string) => changeFormField(fieldName, newValue),
    errorMessage: errorMessageByFieldName[fieldName],
  });

  const discardChanges = () => {
    setTreeFormValues(toTreeFormValues(savedTree));
    setErrorMessageByFieldName({});
    setFormErrorMessage(null);
  };

  const submitTreeChanges = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    await saveTreeChanges();
  };

  /**
   * Zapis formularza; true = zapisano (takze na urzadzeniu offline), false = blad walidacji / zapisu, konflikt albo
   * zapis w toku. overwrite (etap 37) = "Zapisz mimo to" po konflikcie.
   */
  const saveTreeChanges = async (overwrite = false): Promise<boolean> => {
    if (isSubmissionInProgress.current) return false;
    setFormErrorMessage(null);
    setWasJustSaved(false);
    setHasEditConflict(false);

    const clientSideErrors = validateTreeFormValues(treeFormValues);
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) {
      setFormErrorMessage('Popraw zaznaczone pola.');
      return false;
    }

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      const updatedTree = await treesApi.updateTreeOfProject(projectId, savedTree.id,
        toTreeUpdateRequest(treeFormValues, savedTree, overwrite));
      // Odpowiedz backendu jest zrodlem prawdy (np. 18.50 zamiast wpisanego 18,5; przyciete spacje).
      setTreeFormValues(toTreeFormValues(updatedTree));
      onTreeSaved(updatedTree);
      setWasJustSaved(true);
      return true;
    } catch (savingError) {
      if (savingError instanceof ApiRequestError && savingError.httpStatus === 409) {
        setHasEditConflict(true);
      } else if (savingError instanceof ApiRequestError) {
        setErrorMessageByFieldName(savingError.errorMessageByFieldName);
        setFormErrorMessage(`Nie udało się zapisać zmian. ${savingError.message}`);
      } else {
        setFormErrorMessage('Nie udało się zapisać zmian. Nieoczekiwany błąd.');
      }
      return false;
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  saveTreeChangesRef.current = () => saveTreeChanges();

  // Etap 37: po konflikcie - wczytanie danych z serwera (wpisane zmiany przepadaja) zamiast nadpisania.
  const loadCurrentTreeData = async () => {
    try {
      const currentTree = await treesApi.getTreeOfProject(projectId, savedTree.id);
      setTreeFormValues(toTreeFormValues(currentTree));
      setHasEditConflict(false);
      onTreeSaved(currentTree);
    } catch {
      setFormErrorMessage('Nie udało się pobrać aktualnych danych drzewa.');
    }
  };
  discardChangesRef.current = discardChanges;

  return (
    <form onSubmit={submitTreeChanges} className="form tree-field-form" noValidate aria-labelledby="tree-data-heading">
      <h3 id="tree-data-heading">Dane drzewa</h3>
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      {hasEditConflict && (
        <div className="stack" role="alert">
          <Alert variant="error">
            Dane tego drzewa zmieniono w międzyczasie (np. na innym urządzeniu). Twoje zmiany nie zostały zapisane.
          </Alert>
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => void loadCurrentTreeData()}>
              Wczytaj aktualne dane (moje zmiany przepadną)
            </button>
            <button type="button" className="btn btn-danger-outline" onClick={() => void saveTreeChanges(true)}>
              Zapisz mimo to (nadpisz tamte zmiany)
            </button>
          </div>
        </div>
      )}

      <TreeFieldFormSection title="Identyfikacja">
        <LabeledInputField label="Numer drzewa *" maxLength={100} autoComplete="off"
                           hintText="Własny identyfikator, np. 1, A-12, PARK-15." {...bindTextField('inventoryNumber')} />
      </TreeFieldFormSection>

      <TreeFieldFormSection title="Gatunek">
        <div className="grid-2">
          <LabeledInputField label="Nazwa polska" maxLength={255} autoComplete="off" placeholder="np. dąb szypułkowy"
                             {...bindTextField('speciesPolish')} />
          <LabeledInputField label="Nazwa łacińska" maxLength={255} autoComplete="off" placeholder="np. Quercus robur"
                             {...bindTextField('speciesLatin')} />
        </div>
      </TreeFieldFormSection>

      <TreeFieldFormSection title="Pomiary">
        {/* Tekst z klawiatura numeryczna zamiast type="number": type="number" zle obsluguje przecinek i pozwala na "e". */}
        <div className="grid-3">
          <LabeledInputField label="Obwód pnia [cm]" inputMode="numeric" autoComplete="off" placeholder="np. 314"
                             {...bindTextField('trunkCircumferenceCm')} />
          <LabeledInputField label="Wysokość drzewa [m]" inputMode="decimal" autoComplete="off" placeholder="np. 18,5"
                             {...bindTextField('heightM')} />
          <LabeledInputField label="Średnica korony [m]" inputMode="decimal" autoComplete="off" placeholder="np. 12"
                             {...bindTextField('crownDiameterM')} />
        </div>
      </TreeFieldFormSection>

      <TreeFieldFormSection title="Stan drzewa">
        <VoiceEnabledTextAreaField label="Opis stanu" rows={3}
                                   hintText="Stan zdrowotny i statyczny własnymi słowami, np. posusz, ubytki, pęknięcia."
                                   {...bindVoiceTextField('conditionNotes')} />
      </TreeFieldFormSection>

      <TreeFieldFormSection title="Zagrożenia">
        <VoiceEnabledTextAreaField label="Opis zagrożeń" rows={3}
                                   hintText="Co i komu zagraża, np. konar nad chodnikiem." {...bindVoiceTextField('hazardNotes')} />
      </TreeFieldFormSection>

      <TreeFieldFormSection title="Notatki">
        <VoiceEnabledTextAreaField label="Notatki" rows={4} {...bindVoiceTextField('notes')} />
      </TreeFieldFormSection>

      {wasJustSaved && !hasUnsavedChanges && (
        <Alert variant="success">{savedTree.offlinePending
          ? 'Zmiany zapisano na tym urządzeniu - zostaną wysłane po połączeniu z internetem.'
          : 'Zmiany zostały zapisane.'}</Alert>
      )}
      <div className="form-actions tree-field-form-actions">
        {hasUnsavedChanges && <span className="muted small tree-field-form-dirty">Masz niezapisane zmiany.</span>}
        {/* Anuluj zablokowane w trakcie zapisu - inaczej odpowiedz PATCH nadpisalaby porzucona edycje. */}
        <button type="button" className="btn btn-ghost" onClick={discardChanges} disabled={!hasUnsavedChanges || isSaving}>
          Anuluj
        </button>
        <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
          {isSaving ? 'Zapisywanie…' : 'Zapisz zmiany'}
        </button>
      </div>
    </form>
  );
}
