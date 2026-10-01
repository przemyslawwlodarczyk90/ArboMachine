import { useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { formatTimestampForDisplay } from '../../../formatting/formatTimestampForDisplay';
import { VoiceEnabledTextAreaField } from '../../../speech/VoiceEnabledTextAreaField';
import { treeValuationsApi } from './treeValuationsApi';
import { ValuationBadge } from './ValuationBadge';
import {
  NOT_ASSESSED_LABEL, VALUATION_CATEGORY_OPTIONS, VALUATION_TEXT_FIELDS, type TreeValuation,
  type TreeValuationTextField,
} from './valuationDataTypes';
import {
  hasUnsavedValuationChanges, isValuationEmpty, toValuationFormValues, toValuationRequest, validateValuationFormValues,
  type TreeValuationFormValues,
} from './valuationFormValues';

type ValuationLoadingState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; valuation: TreeValuation };

/**
 * Sekcja "Waloryzacja" karty drzewa (etap 27) - ZAPIS OCENY ARBORYSTY, system niczego nie wylicza. Ocena ogolna
 * (tymczasowa kategoria V1, wybierana recznie albo "Nie oceniono") i opisowe wartosci skladowe z dyktowaniem.
 * Dziala tez bez polaczenia (kolejka offline). Nic nie zapisuje sie samo; "Anuluj" porzuca zmiany.
 */
export function TreeValuationSection({ projectId, treeId, syncGeneration = 0 }: {
  projectId: number;
  treeId: number;
  syncGeneration?: number;
}) {
  const [loadingState, setLoadingState] = useState<ValuationLoadingState>({ status: 'loading' });
  const [formValues, setFormValues] = useState<TreeValuationFormValues | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<TreeValuationTextField, string>>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isRemovalConfirmationVisible, setIsRemovalConfirmationVisible] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResponseStillWanted = true;
    treeValuationsApi.getTreeValuation(projectId, treeId)
      .then((valuation) => { if (isResponseStillWanted) setLoadingState({ status: 'loaded', valuation }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setLoadingState({ status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId, syncGeneration]);

  const savedValuation = loadingState.status === 'loaded' ? loadingState.valuation : null;
  const isEditing = formValues !== null;

  const startEditing = () => {
    setFormValues(toValuationFormValues(savedValuation));
    setFieldErrors({});
    setFormErrorMessage(null);
    setSuccessMessage(null);
  };

  const stopEditing = () => {
    setFormValues(null);
    setFieldErrors({});
    setFormErrorMessage(null);
  };

  const runSubmission = async (submission: () => Promise<TreeValuation>, message: (valuation: TreeValuation) => string) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setFormErrorMessage(null);
    try {
      const valuation = await submission();
      setLoadingState({ status: 'loaded', valuation });
      setSuccessMessage(message(valuation));
      setIsRemovalConfirmationVisible(false);
      stopEditing();
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setFieldErrors(savingError.errorMessageByFieldName as Partial<Record<TreeValuationTextField, string>>);
        setFormErrorMessage(`Nie udało się zapisać waloryzacji. ${savingError.message}`);
      } else {
        setFormErrorMessage('Nie udało się zapisać waloryzacji. Nieoczekiwany błąd.');
      }
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const offlineMessage = 'zapisano na tym urządzeniu - zostanie wysłana po połączeniu z internetem.';

  const submit = (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (!formValues) return;
    const errors = validateValuationFormValues(formValues);
    setFieldErrors(errors);
    if (Object.keys(errors).length) {
      setFormErrorMessage('Popraw zaznaczone pola.');
      return;
    }
    if (isValuationEmpty(formValues)) {
      setFormErrorMessage('Wybierz ocenę ogólną albo wpisz przynajmniej jedną wartość. Aby usunąć waloryzację, użyj „Usuń waloryzację”.');
      return;
    }
    void runSubmission(() => treeValuationsApi.saveTreeValuation(projectId, treeId, toValuationRequest(formValues)),
      (valuation) => (valuation.offlinePending ? `Waloryzację ${offlineMessage}` : 'Waloryzacja została zapisana.'));
  };

  const removeValuation = () => void runSubmission(
    async () => (await treeValuationsApi.deleteTreeValuation(projectId, treeId)) ?? { treeId, hasValuation: false },
    (valuation) => (valuation.offlinePending ? `Usunięcie waloryzacji ${offlineMessage}` : 'Waloryzacja została usunięta.'));

  return (
    <section className="tree-card-section tree-valuation-section" aria-labelledby="tree-valuation-heading">
      <div className="section-header-row">
        <h3 id="tree-valuation-heading">Waloryzacja</h3>
        {loadingState.status === 'loaded' && !isEditing && (
          <div className="form-actions">
            <button type="button" className={`btn ${savedValuation?.hasValuation ? 'btn-secondary' : 'btn-primary'}`} onClick={startEditing}>
              {savedValuation?.hasValuation ? 'Edytuj waloryzację' : 'Dodaj waloryzację'}
            </button>
            {savedValuation?.hasValuation && (
              <button type="button" className="btn btn-danger-outline" onClick={() => setIsRemovalConfirmationVisible(true)}>
                Usuń waloryzację
              </button>
            )}
          </div>
        )}
      </div>
      <p className="muted small">
        Ocena arborysty - aplikacja niczego nie wylicza. Kategorie ogólne są tymczasowe (wersja V1), do uzgodnienia z arborystami.
      </p>

      {loadingState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
      {loadingState.status === 'failed' && <Alert variant="error">Nie udało się pobrać waloryzacji. {loadingState.errorMessage}</Alert>}
      {successMessage && !isEditing && <Alert variant="success">{successMessage}</Alert>}

      {isRemovalConfirmationVisible && (
        <div className="photo-annotation-confirmation" role="alert">
          <span>Czy na pewno usunąć waloryzację tego drzewa? Dane drzewa i prace zostaną bez zmian.</span>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setIsRemovalConfirmationVisible(false)} disabled={isSaving}>Anuluj</button>
            <button type="button" className="btn btn-danger" onClick={removeValuation} disabled={isSaving}>
              {isSaving ? 'Usuwanie…' : 'Usuń waloryzację'}
            </button>
          </div>
        </div>
      )}

      {loadingState.status === 'loaded' && !isEditing && (savedValuation?.hasValuation ? (
        <dl className="valuation-summary">
          <dt>Ocena ogólna</dt>
          <dd><ValuationBadge category={savedValuation.overallCategory} /></dd>
          {VALUATION_TEXT_FIELDS.filter(({ field }) => savedValuation[field]).map(({ field, label }) => (
            <div key={field} className="valuation-summary-row">
              <dt>{label}</dt>
              <dd className="multiline-text">{savedValuation[field]}</dd>
            </div>
          ))}
          {savedValuation.updatedAt && (
            <p className="muted small">Ostatnia zmiana {formatTimestampForDisplay(savedValuation.updatedAt)}
              {savedValuation.offlinePending ? ' · czeka na synchronizację' : ''}</p>
          )}
        </dl>
      ) : (
        <p className="muted">Nie przeprowadzono jeszcze waloryzacji tego drzewa.</p>
      ))}

      {isEditing && formValues && (
        <form className="form valuation-form" onSubmit={submit} noValidate>
          {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
          <fieldset className="valuation-category-field">
            <legend>Ocena ogólna</legend>
            <div className="valuation-category-options" role="radiogroup" aria-label="Ocena ogólna">
              {[{ category: '' as const, label: NOT_ASSESSED_LABEL }, ...VALUATION_CATEGORY_OPTIONS].map(({ category, label }) => (
                <button key={label} type="button" role="radio" aria-checked={formValues.overallCategory === category} disabled={isSaving}
                        className={`btn ${formValues.overallCategory === category ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setFormValues({ ...formValues, overallCategory: category })}>
                  {label}
                </button>
              ))}
            </div>
          </fieldset>
          {VALUATION_TEXT_FIELDS.map(({ field, label, placeholder }) => (
            <VoiceEnabledTextAreaField key={field} label={label} rows={field === 'notes' ? 2 : 3} value={formValues[field]}
                                       disabled={isSaving} errorMessage={fieldErrors[field]}
                                       hintText={placeholder || undefined}
                                       onValueChange={(newValue) => setFormValues({ ...formValues, [field]: newValue })} />
          ))}
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={stopEditing} disabled={isSaving}>Anuluj</button>
            <button type="submit" className="btn btn-primary"
                    disabled={isSaving || !hasUnsavedValuationChanges(formValues, savedValuation)}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz waloryzację'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
