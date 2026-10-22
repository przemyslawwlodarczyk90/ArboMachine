import { describeGnssFailure, formatAccuracy, gnssAccuracyQuality, type GnssReading } from '../gnss/deviceGnss';
import type { LivePositionStatus } from '../gnss/livePosition';

/** Kolor wskaznika wg dokladnosci zgloszonej przez urzadzenie (progi z deviceGnss; bez nazywania tego "RTK"). */
const TONE_BY_QUALITY = { SURVEY: 'good', SUBMETER: 'good', GOOD: 'good', ACCEPTABLE: 'medium', POOR: 'poor' } as const;

/** Wskaznik GPS w gornym pasku trybu terenowego (etap 32): "GPS ±4 m", "GPS…" albo "GPS niedostepny". */
export function FieldGpsIndicator({ liveStatus, lastReading }: { liveStatus: LivePositionStatus; lastReading: GnssReading | null }) {
  if (liveStatus.status === 'unavailable') {
    return <span className="field-gps field-gps-poor" role="status" title={describeGnssFailure(liveStatus.reason)}>GPS niedostępny</span>;
  }
  if (!lastReading || liveStatus.status === 'idle') {
    return <span className="field-gps" role="status" title="Oczekiwanie na pozycję urządzenia">GPS…</span>;
  }
  const tone = TONE_BY_QUALITY[gnssAccuracyQuality(lastReading.accuracyM)];
  return (
    <span className={`field-gps field-gps-${tone}`} role="status"
          title={`Dokładność zgłoszona przez urządzenie: ${formatAccuracy(lastReading.accuracyM)}`}>
      GPS {formatAccuracy(lastReading.accuracyM)}
    </span>
  );
}
