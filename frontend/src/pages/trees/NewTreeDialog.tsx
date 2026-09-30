import { useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { ModalDialog } from '../../components/ModalDialog';
import { treesApi } from './treesApi';

/**
 * Okno dodawania drzewa - celowo minimalne (sam numer), bo reszte danych uzupelnia sie w formularzu terenowym
 * karty drzewa: "Dodaj drzewo -> numer -> karta". Po sukcesie przechodzi od razu do karty nowego drzewa.
 * Przy bledzie okno zostaje otwarte z wpisanym numerem.
 */
export function NewTreeDialog({ projectId, closeDialog }: { projectId: number; closeDialog: () => void }) {
  const navigate = useNavigate();
  const [inventoryNumber, setInventoryNumber] = useState('');
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
      setErrorMessageByFieldName({ inventoryNumber: 'Numer drzewa jest wymagany.' });
      return;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsAddingTree(true);
    try {
      const createdTree = await treesApi.createTreeInProject(projectId, { inventoryNumber: inventoryNumber.trim() });
      navigate(`/projects/${projectId}/trees/${createdTree.id}`);
      closeDialog();
    } catch (creationError) {
      if (creationError instanceof ApiRequestError) {
        setErrorMessageByFieldName(creationError.errorMessageByFieldName);
        setFormErrorMessage(`Nie udało się dodać drzewa. ${creationError.message}`);
      } else {
        setFormErrorMessage('Nie udało się dodać drzewa. Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsAddingTree(false);
    }
  };

  return (
    <ModalDialog title="Dodaj drzewo" closeDialog={closeDialog} isClosingBlocked={isAddingTree}>
      <form onSubmit={submitNewTree} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <LabeledInputField label="Numer drzewa *" autoFocus autoComplete="off" maxLength={100} value={inventoryNumber}
                           hintText="Pozostałe dane uzupełnisz w karcie drzewa, która otworzy się po dodaniu."
                           onChange={(changeEvent) => setInventoryNumber(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.inventoryNumber} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isAddingTree}>Anuluj</button>
          <button type="submit" className="btn btn-primary" disabled={isAddingTree}>
            {isAddingTree ? 'Dodawanie…' : 'Dodaj i otwórz kartę'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
