import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import {
  toTreeLocationFormValues, toTreeLocationRequest, validateTreeLocationFormValues, type TreeLocationFormValues,
} from './treeLocationFormValues';

/**
 * Reczne wpisanie wspolrzednych - manual coordinate input is V1 fallback. Obok niego jest wskazanie punktu na mapie
 * (TreeLocationMap w TreeLocationSection), ktore wysyla ten sam request (treesApi.setTreeLocation).
 * Ten formularz niczego o mapie nie zaklada.
 * Po udanym zapisie oddaje drzewo z odpowiedzi (onTreeLocationSaved) - bez dodatkowego GET.
 */
export function TreeLocationForm({ projectId, tree, onTreeLocationSaved, cancelEditing }: {
  projectId: number;
  tree: Tree;
  onTreeLocationSaved: (savedTree: Tree) => void;
  cancelEditing: () => void;
}) {
  const [locationFormValues, setLocationFormValues] = useState<TreeLocationFormValues>(
    () => toTreeLocationFormValues(tree.location));
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const changeFormField = (fieldName: keyof TreeLocationFormValues, newValue: string) =>
    setLocationFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));

  const submitLocation = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    const clientSideErrors = validateTreeLocationFormValues(locationFormValues);
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) return;

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      const savedTree = await treesApi.setTreeLocation(projectId, tree.id, toTreeLocationRequest(locationFormValues));
      onTreeLocationSaved(savedTree);
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setErrorMessageByFieldName(savingError.errorMessageByFieldName);
        setFormErrorMessage(savingError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <form onSubmit={submitLocation} className="form" noValidate>
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      <div className="grid-2">
        {/* Zwykla klawiatura zamiast inputMode="decimal" - klawiatura dziesietna iOS nie ma znaku minus. */}
        <LabeledInputField label="Szerokość geograficzna *" autoFocus autoComplete="off" placeholder="np. 52.2297"
                           value={locationFormValues.latitude}
                           hintText="Stopnie dziesiętne, od -90 do 90."
                           onChange={(changeEvent) => changeFormField('latitude', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.latitude} />
        <LabeledInputField label="Długość geograficzna *" autoComplete="off" placeholder="np. 21.0122"
                           value={locationFormValues.longitude}
                           hintText="Stopnie dziesiętne, od -180 do 180."
                           onChange={(changeEvent) => changeFormField('longitude', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.longitude} />
      </div>
      <div className="form-actions">
        {/* Anuluj zablokowane w trakcie zapisu - inaczej odpowiedz PUT moglaby nadpisac porzucona edycje. */}
        <button type="button" className="btn btn-ghost" onClick={cancelEditing} disabled={isSaving}>Anuluj</button>
        <button type="submit" className="btn btn-primary" disabled={isSaving}>
          {isSaving ? 'Zapisywanie…' : 'Zapisz lokalizację'}
        </button>
      </div>
    </form>
  );
}
