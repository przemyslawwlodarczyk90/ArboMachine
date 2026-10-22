import { useState } from 'react';
import { ModalDialog } from '../components/ModalDialog';
import { unsavedChangesStore } from './unsavedChangesStore';

/**
 * Pytanie przy opuszczaniu niezapisanych zmian (etap 32): "Zapisz" (gdy zrodlo umie zapisac), "Odrzuc zmiany" albo
 * "Zostan". Po zapisie lub odrzuceniu wywoluje proceed - np. przelaczenie trybu albo otwarcie innego drzewa.
 * Nieudany zapis zostawia okno otwarte (blad pokazuje formularz pod spodem).
 */
export function UnsavedChangesDialog({ proceed, closeDialog }: { proceed: () => void; closeDialog: () => void }) {
  const [isSaving, setIsSaving] = useState(false);
  const [hasSavingFailed, setHasSavingFailed] = useState(false);

  const saveAndProceed = async () => {
    setIsSaving(true);
    setHasSavingFailed(false);
    const isSaved = await unsavedChangesStore.saveAll();
    setIsSaving(false);
    if (isSaved) proceed(); else setHasSavingFailed(true);
  };

  const discardAndProceed = () => {
    unsavedChangesStore.discardAll();
    proceed();
  };

  return (
    <ModalDialog title="Niezapisane zmiany" closeDialog={closeDialog} isClosingBlocked={isSaving}>
      <div className="stack">
        <p>Masz niezapisane zmiany w danych drzewa. Co zrobić?</p>
        {hasSavingFailed && <p className="field-error">Nie udało się zapisać zmian - popraw dane w formularzu i spróbuj ponownie.</p>}
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isSaving}>Zostań</button>
          <button type="button" className="btn btn-danger-outline" onClick={discardAndProceed} disabled={isSaving}>Odrzuć zmiany</button>
          {unsavedChangesStore.canSaveAll() && (
            <button type="button" className="btn btn-primary" onClick={saveAndProceed} disabled={isSaving}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz'}
            </button>
          )}
        </div>
      </div>
    </ModalDialog>
  );
}

/**
 * Wykonanie akcji z ochrona niezapisanych zmian: bez zmian - od razu, ze zmianami - po decyzji w UnsavedChangesDialog.
 * Zwraca funkcje do wywolania akcji i okno do wyrenderowania.
 */
export function useUnsavedChangesGuard() {
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const runGuarded = (action: () => void) => {
    if (unsavedChangesStore.hasUnsavedChanges()) setPendingAction(() => action);
    else action();
  };
  const dialog = pendingAction && (
    <UnsavedChangesDialog proceed={() => { setPendingAction(null); pendingAction(); }} closeDialog={() => setPendingAction(null)} />
  );
  return { runGuarded, dialog };
}
