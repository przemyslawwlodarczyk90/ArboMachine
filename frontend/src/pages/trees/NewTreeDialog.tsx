import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { ModalDialog } from '../../components/ModalDialog';
import type { Tree } from './treeDataTypes';
import { TreeFormFields } from './TreeFormFields';
import {
  emptyTreeFormValues, toTreeEditableFieldsRequest, validateTreeFormValues, type TreeFormValues,
} from './treeFormValues';
import { treesApi } from './treesApi';

/**
 * Okno dodawania drzewa do projektu. Po sukcesie oddaje utworzone drzewo wywolujacemu (onTreeCreated),
 * ktory dopisuje je do listy. Przy bledzie okno zostaje otwarte z wpisanymi danymi.
 */
export function NewTreeDialog({ projectId, onTreeCreated, closeDialog }: {
  projectId: number;
  onTreeCreated: (createdTree: Tree) => void;
  closeDialog: () => void;
}) {
  const [treeFormValues, setTreeFormValues] = useState<TreeFormValues>(emptyTreeFormValues);
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isAddingTree, setIsAddingTree] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const changeFormField = (fieldName: keyof TreeFormValues, newValue: string) =>
    setTreeFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));

  const submitNewTree = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    const clientSideErrors = validateTreeFormValues(treeFormValues);
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) return;

    isSubmissionInProgress.current = true;
    setIsAddingTree(true);
    try {
      const createdTree = await treesApi.createTreeInProject(projectId, toTreeEditableFieldsRequest(treeFormValues));
      onTreeCreated(createdTree);
    } catch (creationError) {
      if (creationError instanceof ApiRequestError) {
        setErrorMessageByFieldName(creationError.errorMessageByFieldName);
        setFormErrorMessage(creationError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsAddingTree(false);
    }
  };

  return (
    <ModalDialog title="Dodaj drzewo" closeDialog={closeDialog} isClosingBlocked={isAddingTree}>
      <form onSubmit={submitNewTree} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <TreeFormFields treeFormValues={treeFormValues} errorMessageByFieldName={errorMessageByFieldName}
                        changeFormField={changeFormField} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isAddingTree}>
            Anuluj
          </button>
          <button type="submit" className="btn btn-primary" disabled={isAddingTree}>
            {isAddingTree ? 'Dodawanie…' : 'Dodaj drzewo'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
