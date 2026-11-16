import { useSyncExternalStore } from 'react';

// Ustawienia trybu demo na tym urzadzeniu: wskazowki wl./wyl. i czy powitanie / samouczek byly juz pokazane.

const HINTS_ENABLED_KEY = 'arbomachine.demo.hintsEnabled';
const WELCOME_SEEN_KEY = 'arbomachine.demo.welcomeSeen';

export interface DemoPreferences {
  hintsEnabled: boolean;
  welcomeSeen: boolean;
}

function readFlag(key: string, defaultValue: boolean): boolean {
  try {
    const stored = localStorage.getItem(key);
    return stored === null ? defaultValue : stored === '1';
  } catch {
    return defaultValue;
  }
}

function writeFlag(key: string, value: boolean) {
  try {
    localStorage.setItem(key, value ? '1' : '0');
  } catch {
    // brak dostepu do magazynu - ustawienie dziala do odswiezenia strony
  }
}

let preferences: DemoPreferences = {
  hintsEnabled: readFlag(HINTS_ENABLED_KEY, true),
  welcomeSeen: readFlag(WELCOME_SEEN_KEY, false),
};
const listeners = new Set<() => void>();

function update(changes: Partial<DemoPreferences>) {
  preferences = { ...preferences, ...changes };
  listeners.forEach((listener) => listener());
}

export const demoPreferences = {
  setHintsEnabled(hintsEnabled: boolean) {
    writeFlag(HINTS_ENABLED_KEY, hintsEnabled);
    update({ hintsEnabled });
  },
  markWelcomeSeen() {
    writeFlag(WELCOME_SEEN_KEY, true);
    update({ welcomeSeen: true });
  },
  /** Nowa sesja demo zaczyna od powitania (ustawienie wskazowek zostaje). */
  resetWelcome() {
    writeFlag(WELCOME_SEEN_KEY, false);
    update({ welcomeSeen: false });
  },
};

export function useDemoPreferences(): DemoPreferences {
  return useSyncExternalStore(
    (listener) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    () => preferences,
  );
}
