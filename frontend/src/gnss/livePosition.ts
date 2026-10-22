// Biezaca pozycja na mapie trybu terenowego (etap 32) - logika bez React i bez DOM, testowana przez node --test.
// Pozycja na zywo sluzy tylko orientacji; punkt drzewa zapisuje nadal pomiar z deviceGnss (pojedynczy albo usredniony).
import type { GnssFailureReason, GnssReading } from './deviceGnss.ts';

export type LivePositionStatus =
  | { status: 'idle' }
  | { status: 'watching' }
  | { status: 'unavailable'; reason: GnssFailureReason };

/** Kody bledow GeolocationPositionError (stale z MDN). */
const PERMISSION_DENIED = 1;
const TIMEOUT = 3;

/**
 * Nowy odczyt z watchPosition: odczyt starszy niz poprzedni (przegladarka moze oddac pozycje z pamieci podrecznej)
 * jest odrzucany; odczyt z ta sama chwila pomiaru wygrywa tylko, gdy jest dokladniejszy.
 */
export function chooseLivePositionReading(previous: GnssReading | null, candidate: GnssReading): GnssReading {
  if (!previous) return candidate;
  const previousTime = Date.parse(previous.measuredAt);
  const candidateTime = Date.parse(candidate.measuredAt);
  if (candidateTime < previousTime) return previous;
  if (candidateTime === previousTime && candidate.accuracyM >= previous.accuracyM) return previous;
  return candidate;
}

/**
 * Stan po bledzie watchPosition: brak zgody - zawsze "niedostepne"; przekroczony czas pojedynczego odczytu nie przerywa
 * sledzenia; brak pozycji przerywa je tylko, gdy nie ma jeszcze zadnego odczytu (inaczej zostaje ostatni znany).
 */
export function livePositionStatusAfterError(errorCode: number, hasReading: boolean): LivePositionStatus {
  if (errorCode === PERMISSION_DENIED) return { status: 'unavailable', reason: 'PERMISSION_DENIED' };
  if (errorCode === TIMEOUT || hasReading) return { status: 'watching' };
  return { status: 'unavailable', reason: 'POSITION_UNAVAILABLE' };
}
