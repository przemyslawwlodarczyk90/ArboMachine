// Znacznik "ta sesja to tryb demo" - obok tokenu w localStorage, zeby przetrwal odswiezenie strony.
// Nie jest zabezpieczeniem (zapis blokuje backend: 403 DEMO_READ_ONLY) - steruje tylko wygladem (pasek, wskazowki).

const DEMO_SESSION_STORAGE_KEY = 'arbomachine.demoSession';

export const demoSessionStorage = {
  isActive(): boolean {
    try {
      return localStorage.getItem(DEMO_SESSION_STORAGE_KEY) === '1';
    } catch {
      return false;
    }
  },
  markActive() {
    try {
      localStorage.setItem(DEMO_SESSION_STORAGE_KEY, '1');
    } catch {
      // prywatne okno / zablokowany magazyn - demo dziala dalej, tylko bez pamietania po odswiezeniu
    }
  },
  clear() {
    try {
      localStorage.removeItem(DEMO_SESSION_STORAGE_KEY);
    } catch {
      // jw.
    }
  },
};
