import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import { ModalDialog } from '../../components/ModalDialog';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

const optionalTextOrUndefined = (fieldValue: string) => (fieldValue.trim() ? fieldValue.trim() : undefined);

/**
 * Okno dodawania drzewa do projektu. Po sukcesie oddaje utworzone drzewo wywolujacemu (onTreeCreated),
 * ktory dopisuje je do listy. Przy bledzie okno zostaje otwarte z wpisanymi danymi.
 */
export function NewTreeDialog({ projectId, onTreeCreated, closeDialog }: {
  projectId: number;
  onTreeCreated: (createdTree: Tree) => void;
  closeDialog: () => void;
}) {
  const [inventoryNumber, setInventoryNumber] = useState('');
  const [speciesLatin, setSpeciesLatin] = useState('');
  const [notes, setNotes] = useState('');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isAddingTree, setIsAddingTree] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const submitNewTree = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    if (!inventoryNumber.trim()) {
      setErrorMessageByFieldName({ inventoryNumber: 'Inventory number is required.' });
      return;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsAddingTree(true);
    try {
      const createdTree = await treesApi.createTreeInProject(projectId, {
        inventoryNumber: inventoryNumber.trim(),
        speciesLatin: optionalTextOrUndefined(speciesLatin),
        notes: optionalTextOrUndefined(notes),
      });
      onTreeCreated(createdTree);
    } catch (creationError) {
      if (creationError instanceof ApiRequestError) {
        setErrorMessageByFieldName(creationError.errorMessageByFieldName);
        setFormErrorMessage(creationError.message);
      } else {
        setFormErrorMessage('Unexpected error.');
      }
      isSubmissionInProgress.current = false;
      setIsAddingTree(false);
    }
  };

  return (
    <ModalDialog title="Add tree" closeDialog={closeDialog} isClosingBlocked={isAddingTree}>
      <form onSubmit={submitNewTree} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <LabeledInputField label="Inventory number *" autoFocus value={inventoryNumber} maxLength={100}
                           hintText="Your own identifier, e.g. 1, A-12, PARK-15."
                           onChange={(changeEvent) => setInventoryNumber(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.inventoryNumber} />
        <LabeledInputField label="Latin species" value={speciesLatin} maxLength={255} placeholder="e.g. Quercus robur"
                           onChange={(changeEvent) => setSpeciesLatin(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.speciesLatin} />
        <LabeledTextAreaField label="Notes" value={notes}
                              onChange={(changeEvent) => setNotes(changeEvent.target.value)}
                              errorMessage={errorMessageByFieldName.notes} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isAddingTree}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={isAddingTree}>
            {isAddingTree ? 'Adding…' : 'Add tree'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
