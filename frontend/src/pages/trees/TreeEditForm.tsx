import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

interface TreeFormValues {
  inventoryNumber: string;
  speciesLatin: string;
  notes: string;
}

/** Wartosci formularza z zapisanego drzewa - brakujace pola jako puste napisy (kontrolowane inputy). */
const toTreeFormValues = (tree: Tree): TreeFormValues => ({
  inventoryNumber: tree.inventoryNumber,
  speciesLatin: tree.speciesLatin ?? '',
  notes: tree.notes ?? '',
});

/** Porownanie po tych samych zasadach, co normalizacja na backendzie (trim), zeby Save nie aktywowal sie po spacji. */
const normalizedFormValues = (formValues: TreeFormValues): TreeFormValues => ({
  inventoryNumber: formValues.inventoryNumber.trim(),
  speciesLatin: formValues.speciesLatin.trim(),
  notes: formValues.notes.trim(),
});

/**
 * Tryb edycji karty drzewa. Po udanym PATCH oddaje zapisane drzewo (onTreeSaved) - bez dodatkowego GET.
 * Przy bledzie zostaje w trybie edycji z wpisanymi danymi. Cancel nie wysyla zadania i odrzuca zmiany.
 */
export function TreeEditForm({ projectId, savedTree, onTreeSaved, cancelEditing }: {
  projectId: number;
  savedTree: Tree;
  onTreeSaved: (savedTree: Tree) => void;
  cancelEditing: () => void;
}) {
  const [treeFormValues, setTreeFormValues] = useState<TreeFormValues>(() => toTreeFormValues(savedTree));
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = JSON.stringify(normalizedFormValues(treeFormValues))
    !== JSON.stringify(normalizedFormValues(toTreeFormValues(savedTree)));

  const updateFormField = (fieldName: keyof TreeFormValues) => (newValue: string) =>
    setTreeFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));

  const submitTreeChanges = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    if (!treeFormValues.inventoryNumber.trim()) {
      setErrorMessageByFieldName({ inventoryNumber: 'Inventory number is required.' });
      return;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      const normalizedValues = normalizedFormValues(treeFormValues);
      const updatedTree = await treesApi.updateTreeOfProject(projectId, savedTree.id, {
        inventoryNumber: normalizedValues.inventoryNumber,
        speciesLatin: normalizedValues.speciesLatin || undefined,
        notes: normalizedValues.notes || undefined,
      });
      onTreeSaved(updatedTree);
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
    <form onSubmit={submitTreeChanges} className="form" noValidate>
      <h2>Edit tree {savedTree.inventoryNumber}</h2>
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      <LabeledInputField label="Inventory number *" autoFocus maxLength={100} value={treeFormValues.inventoryNumber}
                         onChange={(changeEvent) => updateFormField('inventoryNumber')(changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.inventoryNumber} />
      <LabeledInputField label="Latin species" maxLength={255} placeholder="e.g. Quercus robur"
                         value={treeFormValues.speciesLatin}
                         onChange={(changeEvent) => updateFormField('speciesLatin')(changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.speciesLatin} />
      <LabeledTextAreaField label="Notes" value={treeFormValues.notes}
                            onChange={(changeEvent) => updateFormField('notes')(changeEvent.target.value)}
                            errorMessage={errorMessageByFieldName.notes} />
      <div className="form-actions">
        {/* Cancel zablokowany w trakcie zapisu - inaczej odpowiedz PATCH moglaby nadpisac porzucona edycje. */}
        <button type="button" className="btn btn-ghost" onClick={cancelEditing} disabled={isSaving}>Cancel</button>
        <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
          {isSaving ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  );
}
