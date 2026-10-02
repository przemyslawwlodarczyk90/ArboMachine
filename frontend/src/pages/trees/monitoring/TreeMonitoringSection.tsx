import { useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LabeledInputField } from '../../../components/LabeledInputField';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { VoiceEnabledTextAreaField } from '../../../speech/VoiceEnabledTextAreaField';
import { formatWorkDate, todayIsoDate } from '../works/workManagement';
import { HealthStatusBadge } from './HealthStatusBadge';
import { HEALTH_STATUS_NOT_SPECIFIED_LABEL, HEALTH_STATUS_OPTIONS, type TreeMonitoringRecord } from './monitoringDataTypes';
import {
  areMonitoringFormValuesEquivalent, emptyMonitoringFormValues, toMonitoringFormValues, toMonitoringRequest,
  validateMonitoringFormValues, type MonitoringFormValues,
} from './monitoringFormValues';
import { treeMonitoringApi } from './treeMonitoringApi';

type MonitoringListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; records: TreeMonitoringRecord[] };

/** Edytowany wpis: null = nowa kontrola. */
interface EditedRecord {
  recordId: number | null;
  initialValues: MonitoringFormValues;
  values: MonitoringFormValues;
}

const byInspectionNewestFirst = (first: TreeMonitoringRecord, second: TreeMonitoringRecord) =>
  second.inspectionDate.localeCompare(first.inspectionDate) || Math.abs(second.id) - Math.abs(first.id);

/**
 * Sekcja "Monitoring stanu" karty drzewa (etap 29): historia kontroli (najnowsza pierwsza), dodanie, edycja,
 * usuniecie. Zapis OCENY ARBORYSTY - aplikacja niczego nie wylicza. Ocena "Zły" wysyla powiadomienie (gdy
 * wlaczone). Dziala tez bez polaczenia (kolejka offline). Drzewo bez wpisow = "Nie sprawdzone".
 */
export function TreeMonitoringSection({ projectId, treeId, syncGeneration = 0 }: {
  projectId: number;
  treeId: number;
  syncGeneration?: number;
}) {
  const [listState, setListState] = useState<MonitoringListState>({ status: 'loading' });
  const [editedRecord, setEditedRecord] = useState<EditedRecord | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof MonitoringFormValues, string>>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [recordAwaitingDeletion, setRecordAwaitingDeletion] = useState<TreeMonitoringRecord | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResponseStillWanted = true;
    treeMonitoringApi.listRecords(projectId, treeId)
      .then((records) => { if (isResponseStillWanted) setListState({ status: 'loaded', records }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setListState({ status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId, syncGeneration]);

  const records = listState.status === 'loaded' ? listState.records : [];

  const startEditing = (record: TreeMonitoringRecord | null) => {
    const initialValues = record ? toMonitoringFormValues(record) : emptyMonitoringFormValues(todayIsoDate());
    setEditedRecord({ recordId: record?.id ?? null, initialValues, values: initialValues });
    setFieldErrors({});
    setFormErrorMessage(null);
    setSuccessMessage(null);
    setRecordAwaitingDeletion(null);
  };

  const changeValues = (changes: Partial<MonitoringFormValues>) =>
    setEditedRecord((current) => (current ? { ...current, values: { ...current.values, ...changes } } : current));

  const replaceRecords = (change: (current: TreeMonitoringRecord[]) => TreeMonitoringRecord[]) =>
    setListState((current) => (current.status === 'loaded'
      ? { status: 'loaded', records: change(current.records).sort(byInspectionNewestFirst) } : current));

  const offlineSuffix = ' Zapisano na tym urządzeniu - zostanie wysłane po połączeniu z internetem.';

  const submit = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (!editedRecord || isSubmissionInProgress.current) return;
    const errors = validateMonitoringFormValues(editedRecord.values);
    setFieldErrors(errors);
    if (Object.keys(errors).length) {
      setFormErrorMessage('Popraw zaznaczone pola.');
      return;
    }
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setFormErrorMessage(null);
    try {
      const request = toMonitoringRequest(editedRecord.values);
      const saved = editedRecord.recordId === null
        ? await treeMonitoringApi.createRecord(projectId, treeId, request)
        : await treeMonitoringApi.updateRecord(projectId, treeId, editedRecord.recordId, request);
      replaceRecords((current) => [...current.filter((record) => record.id !== saved.id), saved]);
      setSuccessMessage((editedRecord.recordId === null ? 'Kontrola została zapisana.' : 'Zmiany kontroli zostały zapisane.')
        + (saved.offlinePending ? offlineSuffix : ''));
      setEditedRecord(null);
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setFieldErrors(savingError.errorMessageByFieldName as Partial<Record<keyof MonitoringFormValues, string>>);
        setFormErrorMessage(`Nie udało się zapisać kontroli. ${savingError.message}`);
      } else {
        setFormErrorMessage('Nie udało się zapisać kontroli. Nieoczekiwany błąd.');
      }
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const deleteRecord = async (record: TreeMonitoringRecord) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      await treeMonitoringApi.deleteRecord(projectId, treeId, record.id);
      replaceRecords((current) => current.filter((item) => item.id !== record.id));
      setSuccessMessage('Kontrola została usunięta.');
      setRecordAwaitingDeletion(null);
    } catch (deletionError) {
      setSuccessMessage(null);
      setFormErrorMessage(`Nie udało się usunąć kontroli. ${deletionError instanceof ApiRequestError ? deletionError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const form = editedRecord && (
    <form className="form monitoring-form" onSubmit={submit} noValidate>
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      <LabeledInputField label="Data kontroli" type="date" value={editedRecord.values.inspectionDate} disabled={isSaving}
                         onChange={(changeEvent) => changeValues({ inspectionDate: changeEvent.target.value })}
                         errorMessage={fieldErrors.inspectionDate} />
      <fieldset className="work-choice-field">
        <legend>Ocena stanu</legend>
        <div className="work-choice-options" role="radiogroup" aria-label="Ocena stanu">
          {[{ status: '' as const, label: HEALTH_STATUS_NOT_SPECIFIED_LABEL }, ...HEALTH_STATUS_OPTIONS].map(({ status, label }) => (
            <button key={label} type="button" role="radio" aria-checked={editedRecord.values.healthStatus === status} disabled={isSaving}
                    className={`btn ${editedRecord.values.healthStatus === status ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => changeValues({ healthStatus: status })}>
              {label}
            </button>
          ))}
        </div>
        {editedRecord.values.healthStatus === 'POOR' && (
          <span className="muted small">Zapis oceny „Zły” wyśle powiadomienie (jeśli są włączone).</span>
        )}
      </fieldset>
      <VoiceEnabledTextAreaField label="Uwagi" rows={3} value={editedRecord.values.notes} disabled={isSaving}
                                 errorMessage={fieldErrors.notes} hintText="Co zaobserwowano przy kontroli (opcjonalnie)."
                                 onValueChange={(newValue) => changeValues({ notes: newValue })} />
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={() => setEditedRecord(null)} disabled={isSaving}>Anuluj</button>
        <button type="submit" className="btn btn-primary"
                disabled={isSaving || (editedRecord.recordId !== null
                  && areMonitoringFormValuesEquivalent(editedRecord.values, editedRecord.initialValues))}>
          {isSaving ? 'Zapisywanie…' : 'Zapisz kontrolę'}
        </button>
      </div>
    </form>
  );

  return (
    <section className="tree-card-section" aria-labelledby="tree-monitoring-heading">
      <div className="section-header-row">
        <h3 id="tree-monitoring-heading">Monitoring stanu</h3>
        {listState.status === 'loaded' && !editedRecord && (
          <button type="button" className="btn btn-secondary" onClick={() => startEditing(null)}>Dodaj kontrolę</button>
        )}
      </div>
      <p className="muted small">
        Historia kontroli drzewa - ocena arborysty. Skala „Dobry / Umiarkowany / Zły” jest robocza (wersja V1), to nie metoda VTA.
      </p>

      {listState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
      {listState.status === 'failed' && <Alert variant="error">Nie udało się wczytać kontroli. {listState.errorMessage}</Alert>}
      {successMessage && !editedRecord && <Alert variant="success">{successMessage}</Alert>}
      {formErrorMessage && !editedRecord && <Alert variant="error">{formErrorMessage}</Alert>}

      {editedRecord?.recordId === null && form}

      {listState.status === 'loaded' && records.length === 0 && !editedRecord && (
        <p className="muted">Brak zapisanych kontroli - drzewo nie było jeszcze sprawdzane.</p>
      )}

      {records.length > 0 && (
        <ul className="tree-work-list monitoring-list">
          {records.map((record) => (
            <li key={record.id} className="tree-work-item">
              {editedRecord?.recordId === record.id ? form : (
                <>
                  <div className="tree-work-item-text">
                    <strong>{formatWorkDate(record.inspectionDate)}</strong>
                    <HealthStatusBadge status={record.healthStatus} />
                    {record.offlinePending && <span className="badge offline-pending-badge">Czeka na synchronizację</span>}
                    {record.notes && <p className="tree-work-item-description">{record.notes}</p>}
                  </div>
                  {recordAwaitingDeletion?.id === record.id ? (
                    <div className="photo-annotation-confirmation" role="alert">
                      <span>Usunąć kontrolę z {formatWorkDate(record.inspectionDate)}?</span>
                      <div className="form-actions">
                        <button type="button" className="btn btn-ghost" onClick={() => setRecordAwaitingDeletion(null)} disabled={isSaving}>Anuluj</button>
                        <button type="button" className="btn btn-danger" onClick={() => void deleteRecord(record)} disabled={isSaving}>
                          {isSaving ? 'Usuwanie…' : 'Usuń kontrolę'}
                        </button>
                      </div>
                    </div>
                  ) : !editedRecord && (
                    <div className="form-actions">
                      <button type="button" className="btn btn-secondary btn-small" onClick={() => startEditing(record)}>Edytuj</button>
                      {record.id > 0 && (
                        <button type="button" className="btn btn-danger-outline btn-small" onClick={() => setRecordAwaitingDeletion(record)}>
                          Usuń
                        </button>
                      )}
                    </div>
                  )}
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
