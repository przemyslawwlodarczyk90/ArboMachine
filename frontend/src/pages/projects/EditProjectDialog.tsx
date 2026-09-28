import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import { ModalDialog } from '../../components/ModalDialog';
import type { Project } from './projectDataTypes';
import { projectsApi } from './projectsApi';

/**
 * Okno edycji nazwy i opisu projektu (PATCH). Po sukcesie oddaje zapisany projekt (onProjectSaved) - bez GET.
 * Przy bledzie okno zostaje otwarte z wpisanymi danymi.
 */
export function EditProjectDialog({ savedProject, onProjectSaved, closeDialog }: {
  savedProject: Project;
  onProjectSaved: (savedProject: Project) => void;
  closeDialog: () => void;
}) {
  const [projectName, setProjectName] = useState(savedProject.name);
  const [projectDescription, setProjectDescription] = useState(savedProject.description ?? '');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = projectName.trim() !== savedProject.name
    || projectDescription.trim() !== (savedProject.description ?? '').trim();

  const submitProjectChanges = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    if (!projectName.trim()) {
      setErrorMessageByFieldName({ name: 'Project name is required.' });
      return;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      // Oba pola zawsze wysylane - pusty opis oznacza jego wyczyszczenie.
      const savedProjectFromBackend = await projectsApi.updateProjectOfCurrentUser(savedProject.id, {
        name: projectName.trim(),
        description: projectDescription.trim(),
      });
      onProjectSaved(savedProjectFromBackend);
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
    <ModalDialog title="Edit project" closeDialog={closeDialog} isClosingBlocked={isSaving}>
      <form onSubmit={submitProjectChanges} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <LabeledInputField label="Project name *" autoFocus value={projectName} maxLength={255}
                           onChange={(changeEvent) => setProjectName(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.name} />
        <LabeledTextAreaField label="Description" value={projectDescription}
                              onChange={(changeEvent) => setProjectDescription(changeEvent.target.value)}
                              errorMessage={errorMessageByFieldName.description} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isSaving}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
            {isSaving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
