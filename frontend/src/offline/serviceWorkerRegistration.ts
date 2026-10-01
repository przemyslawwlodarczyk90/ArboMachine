/**
 * Rejestracja service workera powloki aplikacji (public/sw.js) - tylko w zbudowanej aplikacji (nie w "npm run dev",
 * gdzie cache przeszkadzalby w pracy) i tylko w bezpiecznym kontekscie (HTTPS albo localhost). Brak wsparcia
 * przegladarki = aplikacja dziala jak dotad, tylko bez otwierania strony offline.
 */
export function registerAppShellServiceWorker() {
  if (!import.meta.env.PROD || typeof navigator === 'undefined' || !('serviceWorker' in navigator) || !window.isSecureContext) return;
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Bez service workera aplikacja dziala normalnie - tylko nie otworzy sie bez polaczenia.
    });
  });
}
