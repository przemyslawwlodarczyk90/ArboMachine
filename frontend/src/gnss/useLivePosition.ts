import { useEffect, useState } from 'react';
import { GNSS_READING_OPTIONS, toGnssReading, type GnssReading } from './deviceGnss';
import { chooseLivePositionReading, livePositionStatusAfterError, type LivePositionStatus } from './livePosition';

const isPageVisible = () => typeof document === 'undefined' || document.visibilityState !== 'hidden';

/**
 * Biezaca pozycja (etap 32) przez navigator.geolocation.watchPosition z tymi samymi opcjami co pomiar drzewa.
 * Dziala tylko, gdy ekran jest zamontowany, isEnabled i strona widoczna - aplikacja w tle nie zuzywa baterii na GPS.
 * Bledy opisuje describeGnssFailure (deviceGnss). Ostatni odczyt zostaje po zatrzymaniu (szary w pasku = nieaktualny).
 */
export function useLivePosition(isEnabled = true): { liveStatus: LivePositionStatus; lastReading: GnssReading | null } {
  const [liveStatus, setLiveStatus] = useState<LivePositionStatus>({ status: 'idle' });
  const [lastReading, setLastReading] = useState<GnssReading | null>(null);
  const [isVisible, setIsVisible] = useState(isPageVisible);

  useEffect(() => {
    const updateVisibility = () => setIsVisible(isPageVisible());
    document.addEventListener('visibilitychange', updateVisibility);
    return () => document.removeEventListener('visibilitychange', updateVisibility);
  }, []);

  useEffect(() => {
    if (!isEnabled || !isVisible) {
      setLiveStatus({ status: 'idle' });
      return;
    }
    if (!window.isSecureContext) {
      setLiveStatus({ status: 'unavailable', reason: 'INSECURE_CONTEXT' });
      return;
    }
    const geolocation = typeof navigator !== 'undefined' ? navigator.geolocation : undefined;
    if (!geolocation) {
      setLiveStatus({ status: 'unavailable', reason: 'UNSUPPORTED' });
      return;
    }
    let hasReading = false;
    setLiveStatus({ status: 'watching' });
    const watchId = geolocation.watchPosition(
      (position) => {
        hasReading = true;
        setLastReading((previous) => chooseLivePositionReading(previous, toGnssReading(position)));
        setLiveStatus({ status: 'watching' });
      },
      (error) => setLiveStatus(livePositionStatusAfterError(error.code, hasReading)),
      GNSS_READING_OPTIONS,
    );
    return () => geolocation.clearWatch(watchId);
  }, [isEnabled, isVisible]);

  return { liveStatus, lastReading };
}
