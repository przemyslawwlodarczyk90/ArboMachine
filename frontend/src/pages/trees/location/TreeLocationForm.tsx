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
 * Reczne wpisanie wspolrzednych - manual coordinate input is V1 fallback. Docelowo podstawowym sposobem
 * ustawiania lokalizacji bedzie mapa i/lub GNSS; wtedy dojda obok nich osobne komponenty (np. TreeLocationMap),
 * ktore wysla ten sam request (treesApi.setTreeLocation). Ten formularz niczego o nich nie zaklada.
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
        setFormErrorMessage('Unexpected error.');
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
        <LabeledInputField label="Latitude *" autoFocus autoComplete="off" placeholder="e.g. 52.2297"
                           value={locationFormValues.latitude}
                           hintText="Decimal degrees, from -90 to 90."
                           onChange={(changeEvent) => changeFormField('latitude', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.latitude} />
        <LabeledInputField label="Longitude *" autoComplete="off" placeholder="e.g. 21.0122"
                           value={locationFormValues.longitude}
                           hintText="Decimal degrees, from -180 to 180."
                           onChange={(changeEvent) => changeFormField('longitude', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.longitude} />
      </div>
      <div className="form-actions">
        {/* Cancel zablokowany w trakcie zapisu - inaczej odpowiedz PUT moglaby nadpisac porzucona edycje. */}
        <button type="button" className="btn btn-ghost" onClick={cancelEditing} disabled={isSaving}>Cancel</button>
        <button type="submit" className="btn btn-primary" disabled={isSaving}>
          {isSaving ? 'Saving…' : 'Save location'}
        </button>
      </div>
    </form>
  );
}
