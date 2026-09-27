import { useId, type ReactNode } from 'react';

/**
 * Wspolna obudowa okna dialogowego (tlo + karta + tytul). Klikniecie w tlo zamyka okno, chyba ze
 * isClosingBlocked (np. trwa zapis). Tresc i przyciski dostarcza wywolujacy.
 * Przyszle usprawnienia dostepnosci (Escape, pulapka fokusu, przywrocenie fokusu) dojda tutaj - w jednym miejscu.
 */
export function ModalDialog({ title, closeDialog, isClosingBlocked = false, children }: {
  title: string;
  closeDialog: () => void;
  isClosingBlocked?: boolean;
  children: ReactNode;
}) {
  const titleId = useId();
  const closeUnlessBlocked = () => {
    if (!isClosingBlocked) closeDialog();
  };

  return (
    <div className="modal-backdrop" onClick={closeUnlessBlocked}>
      <div className="card modal" role="dialog" aria-modal="true" aria-labelledby={titleId}
           onClick={(clickEvent) => clickEvent.stopPropagation()}>
        <h2 id={titleId}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
