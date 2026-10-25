import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import { useUnsavedChangesSource } from '../../../fieldmode/useUnsavedChangesSource';
import { useOfflineState } from '../../../offline/useOfflineState';
import {
  arePhotoDocumentSettingsEquivalent, MAXIMUM_PHOTO_CAPTION_LENGTH, toPhotoDocumentSettingsRequest,
  toPhotoDocumentSettingsValues, validatePhotoDocumentSettings, type PhotoDocumentSettingsValues,
} from './photoDocumentSettings';
import type { TreePhoto } from './treePhotoDataTypes';
import { treePhotosApi } from './treePhotosApi';

/**
 * Etap 52: podpis zdjecia w dokumencie PDF i wybor "w dokumencie". Tylko z polaczeniem (V1 - zmiana nie trafia do
 * kolejki offline). Po zapisie oddaje zdjecie z odpowiedzi backendu.
 */
export function TreePhotoDocumentSettingsEditor({ projectId, treeId, treePhoto, onTreePhotoChanged, closeEditor }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  onTreePhotoChanged: (changedTreePhoto: TreePhoto) => void;
  closeEditor: () => void;
}) {
  const { isOnline } = useOfflineState();
  const [initialValues] = useState(() => toPhotoDocumentSettingsValues(treePhoto));
  const [values, setValues] = useState<PhotoDocumentSettingsValues>(initialValues);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = !arePhotoDocumentSettingsEquivalent(values, initialValues);

  /** true = zapisano; false = brak polaczenia, blad walidacji / zapisu albo zapis w toku. */
  const saveSettings = async (): Promise<boolean> => {
    if (isSubmissionInProgress.current) return false;
    const validationError = validatePhotoDocumentSettings(values);
    if (validationError) {
      setErrorMessage(validationError);
      return false;
    }
    if (!isOnline) {
      setErrorMessage('Podpis i wybór do dokumentu zapiszesz po połączeniu z internetem.');
      return false;
    }
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      onTreePhotoChanged(await treePhotosApi.updateTreePhotoDocumentSettings(projectId, treeId, treePhoto.id,
        toPhotoDocumentSettingsRequest(values)));
      closeEditor();
      return true;
    } catch (savingError) {
      setErrorMessage(`Nie udało się zapisać. ${savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
      return false;
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const submit = (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    void saveSettings();
  };

  useUnsavedChangesSource(`photo-document-settings-${treePhoto.id}`, hasUnsavedChanges, 'podpis zdjęcia',
    saveSettings, closeEditor);

  return (
    <form className="form tree-photo-document-settings" onSubmit={submit} noValidate>
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      <LabeledInputField label="Podpis w dokumencie" value={values.caption} maxLength={MAXIMUM_PHOTO_CAPTION_LENGTH}
                         disabled={isSaving} placeholder="np. Ubytek w nasadzie pnia"
                         hintText="Pusty podpis = nazwa pliku."
                         onChange={(changeEvent) => setValues({ ...values, caption: changeEvent.target.value })} />
      <label className="checkbox-row small">
        <input type="checkbox" checked={values.includedInDocument} disabled={isSaving}
               onChange={(changeEvent) => setValues({ ...values, includedInDocument: changeEvent.target.checked })} />
        Zdjęcie w dokumencie PDF
      </label>
      {!isOnline && <p className="muted small">Bez połączenia nie można zapisać podpisu.</p>}
      <div className="form-actions">
        <button type="button" className="btn btn-ghost btn-small" onClick={closeEditor} disabled={isSaving}>Anuluj</button>
        <button type="submit" className="btn btn-primary btn-small" disabled={isSaving || !hasUnsavedChanges || !isOnline}>
          {isSaving ? 'Zapisywanie…' : 'Zapisz'}
        </button>
      </div>
    </form>
  );
}
