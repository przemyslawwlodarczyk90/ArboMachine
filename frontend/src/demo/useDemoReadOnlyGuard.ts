import { useCallback } from 'react';
import { useAuthentication } from '../auth/AuthenticationContext';
import { DEMO_BLOCKED_ACTION_MESSAGES, type DemoBlockedAction } from './demoHints';
import { showDemoNotice } from './demoNotice';

/**
 * Przyciski funkcji zmieniajacych dane zostaja w demo widoczne, ale zamiast otwierac formularz wyjasniaja, co robia
 * w pelnej wersji. Uzycie: onClick={guardDemo('ADD_TREE', openNewTreeDialog)}. Poza demo zwraca akcje bez zmian.
 * To tylko wyglad - zapis i tak blokuje backend (403 DEMO_READ_ONLY).
 */
export function useDemoReadOnlyGuard() {
  const { isDemoSession } = useAuthentication();
  return useCallback(
    <Arguments extends unknown[]>(blockedAction: DemoBlockedAction, action: (...actionArguments: Arguments) => void) =>
      (...actionArguments: Arguments) => {
        if (isDemoSession) {
          showDemoNotice(DEMO_BLOCKED_ACTION_MESSAGES[blockedAction]);
          return;
        }
        action(...actionArguments);
      },
    [isDemoSession],
  );
}
