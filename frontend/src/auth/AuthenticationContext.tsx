import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { accessTokenStorage, arboMachineApi, registerUnauthorizedResponseHandler } from '../api/arboMachineApiClient';
import type { UserAccount } from '../api/apiDataTypes';

interface AuthenticationState {
  /** zalogowane konto albo null */
  currentUserAccount: UserAccount | null;
  /** true, dopoki przy starcie aplikacji sprawdzamy token zapisany w localStorage */
  isRestoringSession: boolean;
  signIn: (email: string, password: string) => Promise<UserAccount>;
  signOut: () => void;
  /** podmienia dane zalogowanego konta, np. po edycji profilu */
  replaceCurrentUserAccount: (updatedAccount: UserAccount) => void;
}

const AuthenticationContext = createContext<AuthenticationState | null>(null);

export function AuthenticationProvider({ children }: { children: ReactNode }) {
  const [currentUserAccount, setCurrentUserAccount] = useState<UserAccount | null>(null);
  const [isRestoringSession, setIsRestoringSession] = useState(true);

  const signOut = useCallback(() => {
    accessTokenStorage.remove();
    setCurrentUserAccount(null);
  }, []);

  // Kazda odpowiedz 401 z chronionego endpointu (wygasly token, zablokowane konto) wylogowuje.
  useEffect(() => {
    registerUnauthorizedResponseHandler(signOut);
    return () => registerUnauthorizedResponseHandler(null);
  }, [signOut]);

  // Przywrocenie sesji po odswiezeniu strony.
  useEffect(() => {
    if (!accessTokenStorage.read()) {
      setIsRestoringSession(false);
      return;
    }
    arboMachineApi.getCurrentUserAccount()
      .then(setCurrentUserAccount)
      .catch(() => signOut())
      .finally(() => setIsRestoringSession(false));
  }, [signOut]);

  const signIn = useCallback(async (email: string, password: string) => {
    const { accessToken } = await arboMachineApi.signIn({ email, password });
    accessTokenStorage.save(accessToken);
    try {
      const signedInAccount = await arboMachineApi.getCurrentUserAccount();
      setCurrentUserAccount(signedInAccount);
      return signedInAccount;
    } catch (loadingAccountError) {
      accessTokenStorage.remove();
      throw loadingAccountError;
    }
  }, []);

  const authenticationState = useMemo(
    () => ({
      currentUserAccount,
      isRestoringSession,
      signIn,
      signOut,
      replaceCurrentUserAccount: setCurrentUserAccount,
    }),
    [currentUserAccount, isRestoringSession, signIn, signOut],
  );
  return <AuthenticationContext.Provider value={authenticationState}>{children}</AuthenticationContext.Provider>;
}

export function useAuthentication(): AuthenticationState {
  const authenticationState = useContext(AuthenticationContext);
  if (!authenticationState) throw new Error('useAuthentication must be used inside <AuthenticationProvider>');
  return authenticationState;
}
