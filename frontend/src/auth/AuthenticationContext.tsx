import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { accessTokenStorage, registerDemoReadOnlyResponseHandler, registerUnauthorizedResponseHandler } from '../api/httpClient';
import { authenticationApi } from '../api/authenticationApi';
import { currentUserAccountApi } from '../api/currentUserAccountApi';
import { offlineService } from '../offline/offlineService';
import { demoApi } from '../demo/demoApi';
import { showDemoNotice } from '../demo/demoNotice';
import { demoPreferences } from '../demo/demoPreferences';
import { demoSessionStorage } from '../demo/demoSession';
import type { UserAccount } from '../api/apiDataTypes';

interface AuthenticationState {
  /** zalogowane konto albo null */
  currentUserAccount: UserAccount | null;
  /** true, dopoki przy starcie aplikacji sprawdzamy token zapisany w localStorage */
  isRestoringSession: boolean;
  signIn: (email: string, password: string) => Promise<UserAccount>;
  /** wejscie do trybu demo bez logowania (konto demo, tylko do odczytu) */
  startDemoSession: () => Promise<UserAccount>;
  /** true = zalogowane jest konto trybu demo (pasek demo, wskazowki, blokady przyciskow) */
  isDemoSession: boolean;
  signOut: () => void;
  /** podmienia dane zalogowanego konta, np. po edycji profilu */
  replaceCurrentUserAccount: (updatedAccount: UserAccount) => void;
}

const AuthenticationContext = createContext<AuthenticationState | null>(null);

export function AuthenticationProvider({ children }: { children: ReactNode }) {
  const [currentUserAccount, setCurrentUserAccount] = useState<UserAccount | null>(null);
  const [isRestoringSession, setIsRestoringSession] = useState(true);
  const [isDemoSession, setIsDemoSession] = useState(() => demoSessionStorage.isActive());

  const signOut = useCallback(() => {
    accessTokenStorage.remove();
    demoSessionStorage.clear();
    setIsDemoSession(false);
    setCurrentUserAccount(null);
  }, []);

  // Warstwa offline zna zalogowane konto: kolejka zmian jest wysylana tylko przez to samo konto, a wylogowanie
  // kasuje kopie odczytow z urzadzenia (niewyslane zmiany zostaja do ponownego zalogowania tego konta).
  // Tryb demo: odczyty jak zwykle, ale bez kolejki zmian - kazdy zapis idzie od razu do serwera (i dostaje 403).
  useEffect(() => {
    offlineService.setReadOnlyDemoMode(isDemoSession);
    void offlineService.setCurrentUser(currentUserAccount?.email ?? null);
  }, [currentUserAccount?.email, isDemoSession]);

  // Kazda odpowiedz 403 DEMO_READ_ONLY (proba zapisu z konta demo) pokazuje wyjasnienie trybu demo.
  useEffect(() => {
    registerDemoReadOnlyResponseHandler(() => showDemoNotice());
    return () => registerDemoReadOnlyResponseHandler(null);
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
    currentUserAccountApi.getCurrentUserAccount()
      .then(setCurrentUserAccount)
      .catch(() => signOut())
      .finally(() => setIsRestoringSession(false));
  }, [signOut]);

  const signIn = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authenticationApi.signIn({ email, password });
    accessTokenStorage.save(accessToken);
    demoSessionStorage.clear();
    setIsDemoSession(false);
    try {
      const signedInAccount = await currentUserAccountApi.getCurrentUserAccount();
      setCurrentUserAccount(signedInAccount);
      return signedInAccount;
    } catch (loadingAccountError) {
      accessTokenStorage.remove();
      throw loadingAccountError;
    }
  }, []);

  const startDemoSession = useCallback(async () => {
    const { accessToken } = await demoApi.startSession();
    accessTokenStorage.save(accessToken);
    demoSessionStorage.markActive();
    try {
      const demoAccount = await currentUserAccountApi.getCurrentUserAccount();
      demoPreferences.resetWelcome();
      setIsDemoSession(true);
      setCurrentUserAccount(demoAccount);
      return demoAccount;
    } catch (loadingAccountError) {
      accessTokenStorage.remove();
      demoSessionStorage.clear();
      throw loadingAccountError;
    }
  }, []);

  const authenticationState = useMemo(
    () => ({
      currentUserAccount,
      isRestoringSession,
      signIn,
      startDemoSession,
      isDemoSession: isDemoSession && currentUserAccount !== null,
      signOut,
      replaceCurrentUserAccount: setCurrentUserAccount,
    }),
    [currentUserAccount, isRestoringSession, isDemoSession, signIn, startDemoSession, signOut],
  );
  return <AuthenticationContext.Provider value={authenticationState}>{children}</AuthenticationContext.Provider>;
}

export function useAuthentication(): AuthenticationState {
  const authenticationState = useContext(AuthenticationContext);
  if (!authenticationState) throw new Error('useAuthentication must be used inside <AuthenticationProvider>');
  return authenticationState;
}
