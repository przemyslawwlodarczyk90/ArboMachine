import { useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../../components/LabeledTextAreaField';
import type { TreeWork, TreeWorkFieldsRequest } from './treeWorkDataTypes';
import {
  areTreeWorkFormValuesEquivalent, MAXIMUM_TREE_WORK_NAME_LENGTH, toTreeWorkFieldsRequest, validateTreeWorkFormValues,
  type TreeWorkFormValues,
} from './treeWorkFormValues';

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
      <LabeledTextAreaField label="Opis" value={treeWorkFormValues.description}
                            onChange={(changeEvent) => changeFormField('description', changeEvent.target.value)}
                            errorMessage={errorMessageByFieldName.description} />
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
