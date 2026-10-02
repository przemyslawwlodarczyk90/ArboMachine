import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import { VoiceEnabledTextAreaField } from '../../../speech/VoiceEnabledTextAreaField';
import type { TreeWork, TreeWorkFieldsRequest, TreeWorkPriority, TreeWorkStatus } from './treeWorkDataTypes';
import {
  areTreeWorkFormValuesEquivalent, MAXIMUM_TREE_WORK_NAME_LENGTH, toTreeWorkFieldsRequest, validateTreeWorkFormValues,
  type TreeWorkFormValues,
} from './treeWorkFormValues';
import { TREE_WORK_PRIORITIES, TREE_WORK_STATUSES, todayIsoDate, workPriorityLabel, workStatusLabel } from './workManagement';

/**
 * Formularz pracy - wspolny dla dodawania i edycji. Nie zna endpointu: wysylanie dostaje z zewnatrz
 * (saveTreeWork), a po sukcesie oddaje prace z odpowiedzi backendu (onTreeWorkSaved) - to ona jest zrodlem prawdy.
 * Przy bledzie zostaje otwarty z wpisanymi danymi. Anuluj niczego nie wysyla.
 */
export function TreeWorkForm({ initialFormValues, saveTreeWork, onTreeWorkSaved, cancelEditing, submitButtonLabel }: {
  initialFormValues: TreeWorkFormValues;
  saveTreeWork: (treeWorkFieldsRequest: TreeWorkFieldsRequest) => Promise<TreeWork>;
  onTreeWorkSaved: (savedTreeWork: TreeWork) => void;
  cancelEditing: () => void;
  submitButtonLabel: string;
}) {
  const [treeWorkFormValues, setTreeWorkFormValues] = useState<TreeWorkFormValues>(initialFormValues);
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const hasUnsavedChanges = !areTreeWorkFormValuesEquivalent(treeWorkFormValues, initialFormValues);

  const changeFormField = (fieldName: keyof TreeWorkFormValues, newValue: string) =>
    setTreeWorkFormValues((previousValues) => ({ ...previousValues, [fieldName]: newValue }));

  // Wybor "Wykonana" podpowiada dzisiejsza date wykonania (mozna ja zmienic albo wyczyscic - bez wymuszania).
  const changeStatus = (status: TreeWorkStatus) =>
    setTreeWorkFormValues((previousValues) => ({
      ...previousValues, status,
      completedDate: status === 'COMPLETED' && !previousValues.completedDate ? todayIsoDate() : previousValues.completedDate,
    }));

  const submitTreeWork = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    const clientSideErrors = validateTreeWorkFormValues(treeWorkFormValues);
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) return;

    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      onTreeWorkSaved(await saveTreeWork(toTreeWorkFieldsRequest(treeWorkFormValues)));
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setErrorMessageByFieldName(savingError.errorMessageByFieldName);
        setFormErrorMessage(savingError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <form onSubmit={submitTreeWork} className="form tree-work-form" noValidate>
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      {/* Wolny tekst zamiast listy wyboru - slownik rodzajow prac nie jest jeszcze ustalony. */}
      <LabeledInputField label="Nazwa *" autoFocus maxLength={MAXIMUM_TREE_WORK_NAME_LENGTH}
                         placeholder="np. Cięcia redukcyjne"
                         hintText="Krótka nazwa zalecenia lub pracy - własnymi słowami."
                         value={treeWorkFormValues.name}
                         onChange={(changeEvent) => changeFormField('name', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.name} />
      <VoiceEnabledTextAreaField label="Opis" value={treeWorkFormValues.description}
                                 onValueChange={(newValue) => changeFormField('description', newValue)}
                                 errorMessage={errorMessageByFieldName.description} />
      {/* Etap 28: status i priorytet to robocze slowniki V1; dowolne przejscia statusow, daty bez ograniczen. */}
      <fieldset className="work-choice-field">
        <legend>Status</legend>
        <div className="work-choice-options" role="radiogroup" aria-label="Status">
          {TREE_WORK_STATUSES.map((status) => (
            <button key={status} type="button" role="radio" aria-checked={treeWorkFormValues.status === status} disabled={isSaving}
                    className={`btn btn-small ${treeWorkFormValues.status === status ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => changeStatus(status)}>
              {workStatusLabel(status)}
            </button>
          ))}
        </div>
        {errorMessageByFieldName.status && <span className="field-error">{errorMessageByFieldName.status}</span>}
      </fieldset>
      <fieldset className="work-choice-field">
        <legend>Priorytet</legend>
        <div className="work-choice-options" role="radiogroup" aria-label="Priorytet">
          {(['', ...TREE_WORK_PRIORITIES] as (TreeWorkPriority | '')[]).map((priority) => (
            <button key={priority || 'NONE'} type="button" role="radio" aria-checked={treeWorkFormValues.priority === priority}
                    disabled={isSaving}
                    className={`btn btn-small ${treeWorkFormValues.priority === priority ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => changeFormField('priority', priority)}>
              {workPriorityLabel(priority || null)}
            </button>
          ))}
        </div>
        {errorMessageByFieldName.priority && <span className="field-error">{errorMessageByFieldName.priority}</span>}
      </fieldset>
      <div className="work-date-fields">
        <LabeledInputField label="Termin" type="date" value={treeWorkFormValues.plannedDate} disabled={isSaving}
                           hintText="Planowany termin wykonania (opcjonalnie)."
                           onChange={(changeEvent) => changeFormField('plannedDate', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.plannedDate} />
        <LabeledInputField label="Data wykonania" type="date" value={treeWorkFormValues.completedDate} disabled={isSaving}
                           hintText="Kiedy pracę wykonano (opcjonalnie)."
                           onChange={(changeEvent) => changeFormField('completedDate', changeEvent.target.value)}
                           errorMessage={errorMessageByFieldName.completedDate} />
      </div>
      <div className="form-actions">
        {/* Anuluj zablokowane w trakcie zapisu - inaczej odpowiedz backendu nadpisalaby porzucona edycje. */}
        <button type="button" className="btn btn-ghost" onClick={cancelEditing} disabled={isSaving}>Anuluj</button>
        <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
          {isSaving ? 'Zapisywanie…' : submitButtonLabel}
        </button>
      </div>
    </form>
  );
}
