import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import {
  collectAveragedGnssReading, formatAccuracy, GNSS_AVERAGING, gnssAccuracyQuality, GnssReadingError,
  readDeviceGnssPosition, type AveragedGnssReading, type GnssReading,
} from '../../../gnss/deviceGnss';
import type { Tree, TreeLocation } from '../treeDataTypes';
import { treesApi } from '../treesApi';

type GnssState =
  | { status: 'measuring'; sampleCount: number }
  | { status: 'measured'; reading: GnssReading | AveragedGnssReading }
  | { status: 'failed'; errorMessage: string };

/** Jeden odczyt (szybko) albo seria usredniona (etap 31 - pod odbiornik RTK, ok. 10 odczytow). */
type MeasurementMode = 'SINGLE' | 'AVERAGED';

const QUALITY_LABELS = {
  SURVEY: 'geodezyjna (RTK)',
  SUBMETER: 'submetrowa',
  GOOD: 'dobra',
  ACCEPTABLE: 'akceptowalna',
  POOR: 'słaba',
} as const;

const isAveraged = (reading: GnssReading | AveragedGnssReading): reading is AveragedGnssReading => 'sampleCount' in reading;

/**
 * Lokalizacja drzewa z GPS urzadzenia (etap 25) przez Geolocation API (przegladarka pyta o zgode): jeden odczyt albo
 * (etap 31) seria usredniona z rozrzutem. Podglad punktu na mapie z dokladnoscia, zapis tym samym endpointem co mapa
 * i formularz (PUT .../location, zrodlo DEVICE_GNSS). Nic nie zapisuje sie samo; "Zmierz ponownie" robi nowy pomiar.
 * Odbiornik RTK dziala, jesli jego aplikacja podstawia pozycje jako lokalizacje telefonu (mock location).
 */
export function TreeGnssLocationPanel({ projectId, tree, onPendingLocationChanged, onTreeLocationSaved, cancel }: {
  projectId: number;
  tree: Tree;
  onPendingLocationChanged: (location: TreeLocation | null) => void;
  onTreeLocationSaved: (savedTree: Tree) => void;
  cancel: () => void;
}) {
  const [gnssState, setGnssState] = useState<GnssState>({ status: 'measuring', sampleCount: 0 });
  const [measurementMode, setMeasurementMode] = useState<MeasurementMode>('SINGLE');
  const [measurementNumber, setMeasurementNumber] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [savingErrorMessage, setSavingErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResultStillWanted = true;
    setGnssState({ status: 'measuring', sampleCount: 0 });
    onPendingLocationChanged(null);
    const geolocation = typeof navigator !== 'undefined' ? navigator.geolocation : null;
    const series = measurementMode === 'AVERAGED'
      ? collectAveragedGnssReading(geolocation, window.isSecureContext,
        (sampleCount) => { if (isResultStillWanted) setGnssState({ status: 'measuring', sampleCount }); })
      : { result: readDeviceGnssPosition(geolocation, window.isSecureContext), abort: () => {} };
    series.result
      .then((reading) => {
        if (!isResultStillWanted) return;
        setGnssState({ status: 'measured', reading });
        onPendingLocationChanged({ latitude: reading.latitude, longitude: reading.longitude, accuracyM: reading.accuracyM });
      })
      .catch((readingError) => {
        if (!isResultStillWanted) return;
        setGnssState({ status: 'failed', errorMessage: readingError instanceof GnssReadingError
          ? readingError.message : 'Nie uzyskano lokalizacji.' });
      });
    return () => {
      isResultStillWanted = false;
      series.abort();
    };
    // onPendingLocationChanged celowo poza zaleznosciami - nowy pomiar tylko po "Zmierz ponownie" / zmianie trybu.
  }, [measurementNumber, measurementMode]);

  const saveReading = async (reading: GnssReading) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setSavingErrorMessage(null);
    try {
      onTreeLocationSaved(await treesApi.setTreeLocation(projectId, tree.id, {
        latitude: reading.latitude, longitude: reading.longitude, accuracyM: reading.accuracyM,
        ...(reading.altitudeM !== null ? { altitudeM: reading.altitudeM } : {}), source: 'DEVICE_GNSS',
      }));
    } catch (savingError) {
      setSavingErrorMessage(savingError instanceof ApiRequestError
        ? Object.values(savingError.errorMessageByFieldName).join(' ') || savingError.message
        : 'Nieoczekiwany błąd.');
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const quality = gnssState.status === 'measured' ? gnssAccuracyQuality(gnssState.reading.accuracyM) : null;

  return (
    <div className="map-picking-panel stack gnss-panel" aria-live="polite">
      <div className="map-view-mode" role="radiogroup" aria-label="Rodzaj pomiaru">
        {([['SINGLE', 'Jeden odczyt'], ['AVERAGED', `Uśrednij ${GNSS_AVERAGING.sampleCount} odczytów`]] as const).map(([mode, label]) => (
          <button key={mode} type="button" role="radio" aria-checked={measurementMode === mode} disabled={isSaving}
                  className={`btn btn-small ${measurementMode === mode ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => setMeasurementMode(mode)}>
            {label}
          </button>
        ))}
      </div>
      {gnssState.status === 'measuring' && (
        <p className="gnss-measuring"><LoadingSpinner /> <strong>
          {measurementMode === 'AVERAGED'
            ? `Zbieranie odczytów… ${gnssState.sampleCount} z ${GNSS_AVERAGING.sampleCount}`
            : 'Pobieranie lokalizacji…'}
        </strong>
          <span className="muted small"> Przeglądarka może zapytać o zgodę na dostęp do lokalizacji. Nie ruszaj się z miejsca pomiaru.</span></p>
      )}
      {gnssState.status === 'failed' && <Alert variant="error">{gnssState.errorMessage}</Alert>}
      {gnssState.status === 'measured' && (
        <>
          <p>
            Pozycja z GPS: <strong>{gnssState.reading.latitude}, {gnssState.reading.longitude}</strong>
            <br />
            <span>Dokładność: <strong>{formatAccuracy(gnssState.reading.accuracyM)}</strong> ({QUALITY_LABELS[quality!]})</span>
            {gnssState.reading.altitudeM !== null && (
              <span className="muted small"> · wysokość {Math.round(gnssState.reading.altitudeM)} m n.p.m.</span>
            )}
            {isAveraged(gnssState.reading) && (
              <span className="muted small">
                <br />Średnia z {gnssState.reading.sampleCount} odczytów, rozrzut {formatAccuracy(gnssState.reading.spreadM)}
              </span>
            )}
          </p>
          {quality === 'POOR' && (
            <Alert variant="info">
              Dokładność jest słaba - urządzenie mogło ustalić pozycję z sieci zamiast z GPS. Odczekaj chwilę na otwartym
              terenie i zmierz ponownie albo wskaż punkt na mapie.
            </Alert>
          )}
        </>
      )}
      {savingErrorMessage && <Alert variant="error">{savingErrorMessage}</Alert>}
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={cancel} disabled={isSaving}>Anuluj</button>
        <button type="button" className="btn btn-secondary" disabled={gnssState.status === 'measuring' || isSaving}
                onClick={() => setMeasurementNumber((number) => number + 1)}>
          Zmierz ponownie
        </button>
        <button type="button" className="btn btn-primary" disabled={gnssState.status !== 'measured' || isSaving}
                onClick={() => gnssState.status === 'measured' && saveReading(gnssState.reading)}>
          {isSaving ? 'Zapisywanie…' : 'Zapisz lokalizację z GPS'}
        </button>
      </div>
    </div>
  );
}
