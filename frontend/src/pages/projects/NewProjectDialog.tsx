import { useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import { ModalDialog } from '../../components/ModalDialog';
import { ProjectOrderFields, type ProjectOrderFieldValues } from './ProjectOrderFields';
import { projectsApi } from './projectsApi';

/**
 * Okno tworzenia projektu. Po sukcesie przechodzi do /projects/{id} z odpowiedzi backendu.
 * Walidacja w przegladarce tylko poprawia UX - zrodlem prawdy sa bledy walidacji z backendu.
 */
export function NewProjectDialog({ closeDialog }: { closeDialog: () => void }) {
  const navigate = useNavigate();
  const [projectName, setProjectName] = useState('');
  const [projectDescription, setProjectDescription] = useState('');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isCreatingProject, setIsCreatingProject] = useState(false);
  // Synchroniczna blokada podwojnego wyslania: stan Reacta aktualizuje sie dopiero po renderze,
  // wiec dwa klikniecia w tym samym momencie widzialyby jeszcze isCreatingProject === false.
  const isSubmissionInProgress = useRef(false);

  const [orderFields, setOrderFields] = useState<ProjectOrderFieldValues>({ clientName: '', siteAddress: '', orderDate: '' });

  const submitNewProject = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    if (!projectName.trim()) {
      setErrorMessageByFieldName({ name: 'Nazwa projektu jest wymagana.' });
      return;
    }
    setErrorMessageByFieldName({});

    isSubmissionInProgress.current = true;
    setIsCreatingProject(true);
    try {
      const createdProject = await projectsApi.createProject({
        name: projectName.trim(),
        description: projectDescription.trim() ? projectDescription.trim() : undefined,
        clientName: orderFields.clientName.trim() || undefined,
        siteAddress: orderFields.siteAddress.trim() || undefined,
        orderDate: orderFields.orderDate || undefined,
      });
      navigate(`/projects/${createdProject.id}`);
    } catch (creationError) {
      // Formularz zostaje otwarty z wpisanymi danymi - uzytkownik moze poprawic i sprobowac ponownie.
      if (creationError instanceof ApiRequestError) {
        setErrorMessageByFieldName(creationError.errorMessageByFieldName);
        setFormErrorMessage(creationError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsCreatingProject(false);
    }
  };

  return (
    <ModalDialog title="Nowy projekt" closeDialog={closeDialog} isClosingBlocked={isCreatingProject}>
      <form onSubmit={submitNewProject} className="form" noValidate>
        {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
        <LabeledInputField label="Nazwa projektu *" autoFocus value={projectName} maxLength={255}
                           onChange={(changeEvent) => setProjectName(changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.name} />
        <LabeledTextAreaField label="Opis" value={projectDescription}
                              onChange={(changeEvent) => setProjectDescription(changeEvent.target.value)}
                              errorMessage={errorMessageByFieldName.description} />
        <ProjectOrderFields values={orderFields} onChange={setOrderFields} errorMessageByFieldName={errorMessageByFieldName}
                            disabled={isCreatingProject} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isCreatingProject}>
            Anuluj
          </button>
          <button type="submit" className="btn btn-primary" disabled={isCreatingProject}>
            {isCreatingProject ? 'Tworzenie…' : 'Utwórz projekt'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}
