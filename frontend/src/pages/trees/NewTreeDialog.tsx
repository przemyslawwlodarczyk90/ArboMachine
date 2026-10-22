import { useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { ModalDialog } from '../../components/ModalDialog';
import { formatTreeLocationForDisplay } from './location/formatTreeLocationForDisplay';
import type { Tree, TreeLocation, TreeLocationRequest } from './treeDataTypes';
import { treesApi } from './treesApi';

/**
 * Okno dodawania drzewa - celowo minimalne (sam numer), bo reszte danych uzupelnia sie w formularzu terenowym
 * karty drzewa: "Dodaj drzewo -> numer -> karta". Po sukcesie przechodzi od razu do karty nowego drzewa.
 * Przy bledzie okno zostaje otwarte z wpisanym numerem.
 * Etap 31: initialLocation - punkt wskazany na mapie projektu ("punkt -> karta"); zapisywany zaraz po utworzeniu
 * drzewa (zrodlo MAP). Oba zapisy dzialaja tez offline (kolejka).
 * Etap 32 (tryb terenowy): initialLocation moze niesc zrodlo, dokladnosc i wysokosc pomiaru GPS (inaczej zrodlo MAP),
 * onTreeCreated oddaje drzewo zamiast przejscia do karty biurowej, a suggestedInventoryNumber wypelnia pole propozycja.
 */
export function NewTreeDialog({ projectId, closeDialog, initialLocation = null, onTreeCreated, suggestedInventoryNumber = '' }: {
  projectId: number;
  closeDialog: () => void;
  initialLocation?: TreeLocation | null;
  /** etap 32: drzewo (z lokalizacja, jesli byla) po utworzeniu - bez nawigacji do karty biurowej */
  onTreeCreated?: (tree: Tree) => void;
  /** etap 32 (V1): proponowany numer, ktory mozna zmienic */
  suggestedInventoryNumber?: string;
}) {
  const navigate = useNavigate();
  const [inventoryNumber, setInventoryNumber] = useState(suggestedInventoryNumber);
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
      const treeWithLocation = initialLocation
        ? await treesApi.setTreeLocation(projectId, createdTree.id, toLocationRequest(initialLocation))
        : createdTree;
      if (onTreeCreated) {
        onTreeCreated(treeWithLocation);
      } else {
        navigate(`/projects/${projectId}/trees/${createdTree.id}`);
      }
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
        {initialLocation && (
          <p className="muted small">
            {initialLocation.source === 'DEVICE_GNSS' ? 'Punkt z GPS' : 'Punkt z mapy'}: <strong>{formatTreeLocationForDisplay(initialLocation)}</strong>
          </p>
        )}
        <LabeledInputField label="Numer drzewa *" autoFocus autoComplete="off" maxLength={100} value={inventoryNumber}
                           hintText={onTreeCreated ? (suggestedInventoryNumber
                             ? 'Proponowany numer: największy numer w projekcie + 1 - możesz go zmienić.'
                             : 'Pozostałe dane uzupełnisz w panelu drzewa.')
                             : 'Pozostałe dane uzupełnisz w karcie drzewa, która otworzy się po dodaniu.'}
                           onChange={(changeEvent) => setInventoryNumber(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.inventoryNumber} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isAddingTree}>Anuluj</button>
          <button type="submit" className="btn btn-primary" disabled={isAddingTree}>
            {isAddingTree ? 'Dodawanie…' : onTreeCreated ? 'Dodaj drzewo' : 'Dodaj i otwórz kartę'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}

/** Punkt nowego drzewa: zrodlo, dokladnosc i wysokosc z pomiaru (etap 32), a bez zrodla - wskazanie na mapie (MAP). */
function toLocationRequest(location: TreeLocation): TreeLocationRequest {
  return {
    latitude: location.latitude,
    longitude: location.longitude,
    source: location.source && location.source !== 'IMPORT' ? location.source : 'MAP',
    ...(location.accuracyM !== null && location.accuracyM !== undefined ? { accuracyM: location.accuracyM } : {}),
    ...(location.altitudeM !== null && location.altitudeM !== undefined ? { altitudeM: location.altitudeM } : {}),
    // Etap 35: czas i sposob pomiaru GNSS (punkt wskazany na mapie ich nie ma).
    ...(location.measuredAt ? { measuredAt: location.measuredAt } : {}),
    ...(location.adapterCode ? { adapterCode: location.adapterCode } : {}),
    ...(location.deviceLabel ? { deviceLabel: location.deviceLabel } : {}),
  };
}
