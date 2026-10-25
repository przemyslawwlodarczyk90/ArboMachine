import { useRef, useState, type FormEvent } from 'react';
import { useUnsavedChangesSource } from '../../fieldmode/useUnsavedChangesSource';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import { ModalDialog } from '../../components/ModalDialog';
import type { Project } from './projectDataTypes';
import { ProjectOrderFields, toProjectOrderFieldValues, type ProjectOrderFieldValues } from './ProjectOrderFields';
import { projectsApi } from './projectsApi';

/**
 * Okno edycji nazwy, opisu i danych zlecenia projektu (PATCH). Po sukcesie oddaje zapisany projekt (onProjectSaved) - bez GET.
 * Przy bledzie okno zostaje otwarte z wpisanymi danymi.
 */
export function EditProjectDialog({ savedProject, onProjectSaved, closeDialog }: {
  savedProject: Project;
  onProjectSaved: (savedProject: Project) => void;
  closeDialog: () => void;
}) {
  const [projectName, setProjectName] = useState(savedProject.name);
  const [projectDescription, setProjectDescription] = useState(savedProject.description ?? '');
  const [orderFields, setOrderFields] = useState<ProjectOrderFieldValues>(toProjectOrderFieldValues(savedProject));
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = projectName.trim() !== savedProject.name
    || projectDescription.trim() !== (savedProject.description ?? '').trim()
    || orderFields.clientName.trim() !== (savedProject.clientName ?? '')
    || orderFields.siteAddress.trim() !== (savedProject.siteAddress ?? '')
    || orderFields.orderDate !== (savedProject.orderDate ?? '')
    || orderFields.contractNumber.trim() !== (savedProject.contractNumber ?? '')
    || orderFields.documentPurpose.trim() !== (savedProject.documentPurpose ?? '')
    || orderFields.legalBasis.trim() !== (savedProject.legalBasis ?? '');

  const submitProjectChanges = (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    void saveProjectChanges();
  };

  /** true = projekt zapisany; false = blad walidacji / zapisu albo zapis w toku. */
  const saveProjectChanges = async (): Promise<boolean> => {
    if (isSubmissionInProgress.current) return false;
    setFormErrorMessage(null);

    if (!projectName.trim()) {
      setErrorMessageByFieldName({ name: 'Nazwa projektu jest wymagana.' });
      return false;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      // Oba pola zawsze wysylane - pusty opis oznacza jego wyczyszczenie.
      const savedProjectFromBackend = await projectsApi.updateProjectOfCurrentUser(savedProject.id, {
        name: projectName.trim(),
        description: projectDescription.trim(),
        clientName: orderFields.clientName.trim(),
        siteAddress: orderFields.siteAddress.trim(),
        orderDate: orderFields.orderDate,
        contractNumber: orderFields.contractNumber.trim(),
        documentPurpose: orderFields.documentPurpose.trim(),
        legalBasis: orderFields.legalBasis.trim(),
      });
      onProjectSaved(savedProjectFromBackend);
      return true;
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setErrorMessageByFieldName(savingError.errorMessageByFieldName);
        setFormErrorMessage(savingError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsSaving(false);
      return false;
    }
  };

  // Etap 49: okno edycji projektu z niezapisanymi zmianami - "Wstecz" przegladarki pyta "Zapisz / Odrzuc / Zostan".
  useUnsavedChangesSource(`edit-project-${savedProject.id}`, hasUnsavedChanges, 'dane projektu', saveProjectChanges, closeDialog);

  return (
    <ModalDialog title="Edytuj projekt" closeDialog={closeDialog} isClosingBlocked={isSaving}>
      <form onSubmit={submitProjectChanges} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <LabeledInputField label="Nazwa projektu *" autoFocus value={projectName} maxLength={255}
                           onChange={(changeEvent) => setProjectName(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.name} />
        <LabeledTextAreaField label="Opis" value={projectDescription}
                              onChange={(changeEvent) => setProjectDescription(changeEvent.target.value)}
                              errorMessage={errorMessageByFieldName.description} />
        <ProjectOrderFields values={orderFields} onChange={setOrderFields} errorMessageByFieldName={errorMessageByFieldName}
                            disabled={isSaving} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isSaving}>Anuluj</button>
          <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
            {isSaving ? 'Zapisywanie…' : 'Zapisz'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
