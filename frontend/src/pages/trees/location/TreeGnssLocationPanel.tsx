import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import {
  formatAccuracy, GNSS_AVERAGING, gnssAccuracyQuality, GnssReadingError, type AveragedGnssReading, type GnssReading,
} from '../../../gnss/deviceGnss';
import { createBrowserGeolocationAdapter, locationFieldsOfFix, type GnssAdapter, type GnssFix } from '../../../gnss/gnssAdapter';
import type { Tree, TreeLocation } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import { fieldLocationSourceLabel, formatMeasurementTime, formatReportedAccuracy } from '../../../fieldmode/fieldLocationDescription';

type GnssState =
  | { status: 'measuring'; sampleCount: number }
  | { status: 'measured'; reading: GnssFix }
  | { status: 'failed'; errorMessage: string };

/** Jeden odczyt (szybko) albo seria usredniona (etap 31 - pod odbiornik RTK, ok. 10 odczytow). */
export type MeasurementMode = 'SINGLE' | 'AVERAGED';

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
export function TreeGnssLocationPanel({ projectId, tree, onPendingLocationChanged, onTreeLocationSaved, cancel,
                                        onReadingConfirmed, describesForField = false, initialMeasurementMode = 'SINGLE',
                                        onMeasurementModeChange, adapter }: {
  projectId: number;
  /** null tylko razem z onReadingConfirmed (pomiar przed utworzeniem drzewa) */
  tree: Tree | null;
  onPendingLocationChanged: (location: TreeLocation | null) => void;
  onTreeLocationSaved: (savedTree: Tree) => void;
  cancel: () => void;
  /**
   * etap 32 ("+ Drzewo tutaj"): zamiast zapisu lokalizacji oddaje zaakceptowany odczyt (np. do okna numeru nowego
   * drzewa); przycisk glowny to wtedy "Dalej"
   */
  onReadingConfirmed?: (reading: GnssFix) => void;
  /** etap 35: zrodlo pozycji (domyslnie przegladarka); kolejne adaptery - po poznaniu odbiornika klienta */
  adapter?: GnssAdapter;
  /** etap 32 (tryb terenowy): zrodlo i czas pomiaru, dokladnosc "zgloszona przez urzadzenie" - bez slowa "RTK" */
  describesForField?: boolean;
  initialMeasurementMode?: MeasurementMode;
  /** etap 32: zapamietanie wyboru "Usrednij" na urzadzeniu */
  onMeasurementModeChange?: (mode: MeasurementMode) => void;
}) {
  const [gnssState, setGnssState] = useState<GnssState>({ status: 'measuring', sampleCount: 0 });
  const [measurementMode, setMeasurementModeState] = useState<MeasurementMode>(initialMeasurementMode);
  const setMeasurementMode = (mode: MeasurementMode) => {
    setMeasurementModeState(mode);
    onMeasurementModeChange?.(mode);
  };
  const [measurementNumber, setMeasurementNumber] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [savingErrorMessage, setSavingErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResultStillWanted = true;
    setGnssState({ status: 'measuring', sampleCount: 0 });
    onPendingLocationChanged(null);
    // Etap 35: pomiar przez adapter (domyslnie przegladarka) - wynik niesie czas i sposob pomiaru.
    const gnssAdapter = adapter ?? createBrowserGeolocationAdapter(
      typeof navigator !== 'undefined' ? navigator.geolocation : null, window.isSecureContext);
    const series = gnssAdapter.measure(measurementMode,
      (sampleCount) => { if (isResultStillWanted) setGnssState({ status: 'measuring', sampleCount }); });
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

  const saveReading = async (reading: GnssFix) => {
    if (isSubmissionInProgress.current) return;
    if (onReadingConfirmed || !tree) {
      onReadingConfirmed?.(reading);
      return;
    }
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setSavingErrorMessage(null);
    try {
      onTreeLocationSaved(await treesApi.setTreeLocation(projectId, tree.id, locationFieldsOfFix(reading)));
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
            {describesForField ? (
              // Etap 32: przegladarka nie zna zrodla (GPS tabletu czy odbiornik) ani typu rozwiazania - bez "RTK".
              <span>
                Źródło: <strong>{fieldLocationSourceLabel('DEVICE_GNSS')}</strong> · {formatReportedAccuracy(gnssState.reading.accuracyM)}
                {' · '}pomiar o {formatMeasurementTime(gnssState.reading.measuredAt)}
              </span>
            ) : (
              <span>Dokładność: <strong>{formatAccuracy(gnssState.reading.accuracyM)}</strong> ({QUALITY_LABELS[quality!]})</span>
            )}
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
          {onReadingConfirmed ? 'Dalej: numer drzewa' : isSaving ? 'Zapisywanie…' : 'Zapisz lokalizację z GPS'}
        </button>
      </div>
    </div>
  );
}
