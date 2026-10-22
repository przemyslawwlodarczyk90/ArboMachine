// Etap 32: zapamietany tryb pracy na urzadzeniu (widok biurowy albo tryb terenowy). Wzor: basemapPreference.ts -
// modul bez React, magazyn do podstawienia w testach, odporny na brak lub wyjatek localStorage.
import type { KeyValueStorage } from '../map/basemapPreference.ts';

export type WorkMode = 'OFFICE' | 'FIELD';

export const WORK_MODE_STORAGE_KEY = 'arbomachine.workMode';
export const DEFAULT_WORK_MODE: WorkMode = 'OFFICE';

const isWorkMode = (value: unknown): value is WorkMode => value === 'OFFICE' || value === 'FIELD';

/** Zapisany tryb albo null (brak wpisu, uszkodzony wpis, niedostepny magazyn). */
export function readSavedWorkMode(storage: KeyValueStorage | null): WorkMode | null {
  try {
    const saved = storage?.getItem(WORK_MODE_STORAGE_KEY) ?? null;
    return isWorkMode(saved) ? saved : null;
  } catch {
    return null;
  }
}

/**
 * Tryb startowy: zapisany wybor, a przy pierwszym uruchomieniu (V1, do potwierdzenia) - tryb terenowy na urzadzeniu
 * dotykowym (pointer: coarse). Nie zgadujemy po szerokosci ekranu: tablet w poziomie ma szerokosc laptopa.
 */
export function resolveWorkMode(storage: KeyValueStorage | null, isCoarsePointer: boolean): WorkMode {
  return readSavedWorkMode(storage) ?? (isCoarsePointer ? 'FIELD' : DEFAULT_WORK_MODE);
}

export function saveWorkMode(storage: KeyValueStorage | null, workMode: WorkMode) {
  try {
    storage?.setItem(WORK_MODE_STORAGE_KEY, workMode);
  } catch {
    // Tryb prywatny / zablokowany magazyn - wybor dziala do zamkniecia strony (przelaczenie i tak nawiguje).
  }
}

export function browserStorageOrNull(): KeyValueStorage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

function isCoarsePointerDevice(): boolean {
  try {
    return typeof matchMedia === 'function' && matchMedia('(pointer: coarse)').matches;
  } catch {
    return false;
  }
}

/** Tryb pracy tego urzadzenia (przegladarka). */
export const currentWorkMode = (): WorkMode => resolveWorkMode(browserStorageOrNull(), isCoarsePointerDevice());
export const rememberWorkMode = (workMode: WorkMode) => saveWorkMode(browserStorageOrNull(), workMode);
