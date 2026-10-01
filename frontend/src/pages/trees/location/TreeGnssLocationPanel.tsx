import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import {
  formatAccuracy, gnssAccuracyQuality, GnssReadingError, readDeviceGnssPosition, type GnssReading,
} from '../../../gnss/deviceGnss';
import type { Tree, TreeLocation } from '../treeDataTypes';
import { treesApi } from '../treesApi';

type GnssState =
  | { status: 'measuring' }
  | { status: 'measured'; reading: GnssReading }
  | { status: 'failed'; errorMessage: string };

const QUALITY_LABELS = {
  GOOD: 'dobra',
  ACCEPTABLE: 'akceptowalna',
  POOR: 'słaba',
} as const;

/**
 * Lokalizacja drzewa z GPS urzadzenia (etap 25): jeden pomiar przez Geolocation API (przegladarka pyta o zgode),
 * podglad punktu na mapie z dokladnoscia, zapis tym samym endpointem co mapa i formularz (PUT .../location, zrodlo
 * DEVICE_GNSS). Nic nie zapisuje sie samo; "Zmierz ponownie" robi nowy pomiar. Zoom mapy nie jest zmieniany.
 */
export function TreeGnssLocationPanel({ projectId, tree, onPendingLocationChanged, onTreeLocationSaved, cancel }: {
  projectId: number;
  tree: Tree;
  onPendingLocationChanged: (location: TreeLocation | null) => void;
  onTreeLocationSaved: (savedTree: Tree) => void;
  cancel: () => void;
}) {
  const [gnssState, setGnssState] = useState<GnssState>({ status: 'measuring' });
  const [measurementNumber, setMeasurementNumber] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [savingErrorMessage, setSavingErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  useEffect(() => {
    let isResultStillWanted = true;
    setGnssState({ status: 'measuring' });
    onPendingLocationChanged(null);
    readDeviceGnssPosition(typeof navigator !== 'undefined' ? navigator.geolocation : null, window.isSecureContext)
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
    return () => { isResultStillWanted = false; };
    // onPendingLocationChanged celowo poza zaleznosciami - nowy pomiar tylko po "Zmierz ponownie".
  }, [measurementNumber]);

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
      {gnssState.status === 'measuring' && (
        <p className="gnss-measuring"><LoadingSpinner /> <strong>Pobieranie lokalizacji…</strong>
          <span className="muted small"> Przeglądarka może zapytać o zgodę na dostęp do lokalizacji.</span></p>
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
