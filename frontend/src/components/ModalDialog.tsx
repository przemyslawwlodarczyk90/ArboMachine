import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react';

/** Elementy, na ktore moze trafic fokus klawiatury wewnatrz okna. */
const FOCUSABLE_ELEMENTS_SELECTOR = [
  'a[href]', 'button:not([disabled])', 'input:not([disabled])', 'select:not([disabled])', 'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/**
 * Wspolna obudowa okna dialogowego (tlo + karta + tytul). Zamykanie: klikniecie w tlo albo Escape, chyba ze
 * isClosingBlocked (np. trwa zapis). Podstawy dostepnosci bez zewnetrznej biblioteki: fokus trafia do okna
 * przy otwarciu (albo zostaje na polu z autoFocus), Tab krazy wewnatrz okna, a po zamknieciu fokus wraca
 * na element, ktory okno otworzyl. Tresc i przyciski dostarcza wywolujacy.
 */
export function ModalDialog({ title, closeDialog, isClosingBlocked = false, isFullScreen = false, children }: {
  title: string;
  closeDialog: () => void;
  isClosingBlocked?: boolean;
  /** okno na caly ekran (np. edytor adnotacji zdjecia na tablecie) */
  isFullScreen?: boolean;
  children: ReactNode;
}) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const elementFocusedBeforeOpening = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const dialogElement = dialogRef.current;
    // Pole z autoFocus moglo juz przejac fokus - wtedy go nie zabieramy.
    if (dialogElement && !dialogElement.contains(document.activeElement)) dialogElement.focus();
    return () => {
      if (elementFocusedBeforeOpening?.isConnected) elementFocusedBeforeOpening.focus();
    };
  }, []);

  const closeUnlessBlocked = () => {
    if (!isClosingBlocked) closeDialog();
  };

  const handleKeyDown = (keyboardEvent: KeyboardEvent<HTMLDivElement>) => {
    if (keyboardEvent.key === 'Escape') {
      keyboardEvent.stopPropagation();
      closeUnlessBlocked();
      return;
    }
    if (keyboardEvent.key !== 'Tab' || !dialogRef.current) return;
    const focusableElements = [...dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_ELEMENTS_SELECTOR)];
    if (focusableElements.length === 0) {
      keyboardEvent.preventDefault();
      return;
    }
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];
    const isFocusOutsideElements = !focusableElements.includes(document.activeElement as HTMLElement);
    if (keyboardEvent.shiftKey && (document.activeElement === firstElement || isFocusOutsideElements)) {
      keyboardEvent.preventDefault();
      lastElement.focus();
    } else if (!keyboardEvent.shiftKey && (document.activeElement === lastElement || isFocusOutsideElements)) {
      keyboardEvent.preventDefault();
      firstElement.focus();
    }
  };

  return (
    <div className="modal-backdrop" onClick={closeUnlessBlocked}>
      <div ref={dialogRef} className={`card modal${isFullScreen ? ' modal-fullscreen' : ''}`} role="dialog" aria-modal="true" aria-labelledby={titleId}
           tabIndex={-1} onKeyDown={handleKeyDown} onClick={(clickEvent) => clickEvent.stopPropagation()}>
        <h2 id={titleId}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
