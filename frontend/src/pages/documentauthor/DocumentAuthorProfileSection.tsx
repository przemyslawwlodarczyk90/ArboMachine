import { useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ToBeClarifiedNote } from '../../components/ToBeClarifiedNote';
import { formatTimestampForDisplay } from '../../formatting/formatTimestampForDisplay';
import { documentAuthorProfileApi } from './documentAuthorProfileApi';
import {
  DOCUMENT_AUTHOR_PROFILE_FIELDS, hasUnsavedDocumentAuthorProfileChanges, toDocumentAuthorProfileFormValues,
  toDocumentAuthorProfileRequest, validateDocumentAuthorProfileFormValues, type DocumentAuthorProfile,
  type DocumentAuthorProfileField, type DocumentAuthorProfileFormValues,
} from './documentAuthorProfileFormValues';

type ProfileLoadingState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; profile: DocumentAuthorProfile };

/**
 * "Dane do dokumentów" w Moim profilu (etap 42): autor opracowania, kwalifikacje, wykonawca i staly opis metod -
 * wpisywane raz, trafiaja na strone tytulowa i do sekcji 1 kazdego dokumentu PDF projektu. Puste pola = w dokumencie
 * "Dane do uzupełnienia". Formularz zawsze otwarty (jak dane osobowe), zapis tylko przyciskiem.
 */
export function DocumentAuthorProfileSection() {
  const [loadingState, setLoadingState] = useState<ProfileLoadingState>({ status: 'loading' });
  const [formValues, setFormValues] = useState<DocumentAuthorProfileFormValues>(toDocumentAuthorProfileFormValues(null));
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<DocumentAuthorProfileField, string>>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResponseStillWanted = true;
    documentAuthorProfileApi.getDocumentAuthorProfile()
      .then((profile) => {
        if (!isResponseStillWanted) return;
        setLoadingState({ status: 'loaded', profile });
        setFormValues(toDocumentAuthorProfileFormValues(profile));
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setLoadingState({ status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  const savedProfile = loadingState.status === 'loaded' ? loadingState.profile : null;

  const submit = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    const errors = validateDocumentAuthorProfileFormValues(formValues);
    setFieldErrors(errors);
    setSuccessMessage(null);
    if (Object.keys(errors).length) {
      setFormErrorMessage('Popraw zaznaczone pola.');
      return;
    }
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setFormErrorMessage(null);
    try {
      const profile = await documentAuthorProfileApi.saveDocumentAuthorProfile(toDocumentAuthorProfileRequest(formValues));
      setLoadingState({ status: 'loaded', profile });
      setFormValues(toDocumentAuthorProfileFormValues(profile));
      setSuccessMessage(profile.hasProfile ? 'Dane do dokumentów zostały zapisane.' : 'Dane do dokumentów zostały usunięte.');
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setFieldErrors(savingError.errorMessageByFieldName as Partial<Record<DocumentAuthorProfileField, string>>);
        setFormErrorMessage(`Nie udało się zapisać danych. ${savingError.message}`);
      } else {
        setFormErrorMessage('Nie udało się zapisać danych. Nieoczekiwany błąd.');
      }
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <section className="card stack-tight document-author-profile-section" aria-labelledby="document-author-profile-heading">
      <h2 id="document-author-profile-heading">Dane do dokumentów</h2>
      <p className="muted small">
        Wpisz raz - pojawią się na stronie tytułowej i w opisie metod każdego dokumentu PDF projektu.
        Puste pole oznacza w dokumencie „Dane do uzupełnienia”.
      </p>
      <ToBeClarifiedNote>
        Czy dane wykonawcy są zawsze te same, czy zależą od projektu (kilku autorów, podwykonawcy), oraz jakie jeszcze
        stałe teksty ma dokument (np. podstawa prawna, zastrzeżenia).
      </ToBeClarifiedNote>

      {loadingState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
      {loadingState.status === 'failed' && <Alert variant="error">Nie udało się pobrać danych do dokumentów. {loadingState.errorMessage}</Alert>}

      {loadingState.status === 'loaded' && (
        <form className="form" onSubmit={submit} noValidate>
          {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
          {successMessage && <Alert variant="success">{successMessage}</Alert>}
          {DOCUMENT_AUTHOR_PROFILE_FIELDS.map(({ field, label, hint, maxLength, multiline }) => multiline ? (
            <LabeledTextAreaField key={field} label={label} hintText={hint} rows={field === 'methodsDescription' ? 6 : 3}
                                  maxLength={maxLength} value={formValues[field]} disabled={isSaving}
                                  errorMessage={fieldErrors[field]}
                                  onChange={(changeEvent) => setFormValues({ ...formValues, [field]: changeEvent.target.value })} />
          ) : (
            <LabeledInputField key={field} label={label} hintText={hint} maxLength={maxLength} value={formValues[field]}
                               disabled={isSaving} errorMessage={fieldErrors[field]}
                               onChange={(changeEvent) => setFormValues({ ...formValues, [field]: changeEvent.target.value })} />
          ))}
          <div className="form-actions">
            {savedProfile?.updatedAt && (
              <span className="muted small">Ostatnia zmiana {formatTimestampForDisplay(savedProfile.updatedAt)}</span>
            )}
            <button type="submit" className="btn btn-primary"
                    disabled={isSaving || !hasUnsavedDocumentAuthorProfileChanges(formValues, savedProfile)}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz dane do dokumentów'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
