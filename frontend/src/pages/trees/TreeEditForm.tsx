import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import type { Tree } from './treeDataTypes';
import { TreeFormFields } from './TreeFormFields';
import {
  areTreeFormValuesEquivalent, toTreeEditableFieldsRequest, toTreeFormValues, validateTreeFormValues,
  type TreeFormValues,
} from './treeFormValues';
import { treesApi } from './treesApi';

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

  const hasUnsavedChanges = !areTreeFormValuesEquivalent(treeFormValues, toTreeFormValues(savedTree));

  const changeFormField = (fieldName: keyof TreeFormValues, newValue: string) =>
    setTreeFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));

  const submitTreeChanges = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    const clientSideErrors = validateTreeFormValues(treeFormValues);
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) return;

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      const updatedTree = await treesApi.updateTreeOfProject(projectId, savedTree.id,
        toTreeEditableFieldsRequest(treeFormValues));
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
      <TreeFormFields treeFormValues={treeFormValues} errorMessageByFieldName={errorMessageByFieldName}
                      changeFormField={changeFormField} />
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
