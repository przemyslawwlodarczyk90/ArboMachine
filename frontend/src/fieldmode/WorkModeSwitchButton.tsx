import { useLocation, useNavigate } from 'react-router-dom';
import { toFieldModePath, toOfficeModePath } from './fieldModePaths';
import { rememberWorkMode, type WorkMode } from './fieldModePreference';
import { useUnsavedChangesGuard } from './UnsavedChangesDialog';

/**
 * Przycisk przelaczenia widok biurowy <-> tryb terenowy (etap 32): zapamietuje wybor na urzadzeniu i przechodzi
 * do odpowiadajacego adresu (ten sam projekt i drzewo). Niezapisane zmiany -> najpierw pytanie.
 */
export function WorkModeSwitchButton({ targetMode, className = 'btn btn-secondary' }: { targetMode: WorkMode; className?: string }) {
  const navigate = useNavigate();
  const { pathname, search } = useLocation();
  const { runGuarded, dialog } = useUnsavedChangesGuard();

  const switchMode = () => runGuarded(() => {
    rememberWorkMode(targetMode);
    navigate(targetMode === 'FIELD' ? toFieldModePath(pathname, search) : toOfficeModePath(pathname, search));
  });

  return (
    <>
      <button type="button" className={className} onClick={switchMode}>
        {targetMode === 'FIELD' ? 'Tryb terenowy' : 'Widok biurowy'}
      </button>
      {dialog}
    </>
  );
}
