import { useSyncExternalStore } from 'react';

/** Domyslny komunikat po probie zapisu w trybie demo (takze odpowiedz 403 DEMO_READ_ONLY z backendu). */
export const DEMO_READ_ONLY_MESSAGE =
  'Tryb demo jest tylko do odczytu – zmiany nie są zapisywane. Po zalogowaniu na własne konto ta funkcja działa normalnie.';

interface DemoNoticeState {
  message: string | null;
  /** rosnie przy kazdym pokazaniu - ten sam tekst pokazany drugi raz tez restartuje licznik ukrycia */
  sequence: number;
}

let state: DemoNoticeState = { message: null, sequence: 0 };
const listeners = new Set<() => void>();

function setState(nextState: DemoNoticeState) {
  state = nextState;
  listeners.forEach((listener) => listener());
}

export function showDemoNotice(message: string = DEMO_READ_ONLY_MESSAGE) {
  setState({ message, sequence: state.sequence + 1 });
}

export function hideDemoNotice() {
  setState({ ...state, message: null });
}

export function useDemoNotice(): DemoNoticeState {
  return useSyncExternalStore(
    (listener) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    () => state,
  );
}
