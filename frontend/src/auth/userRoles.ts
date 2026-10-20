import type { AssignableUserRole, UserRole } from '../api/apiDataTypes';

export const USER_ROLE_DISPLAY_NAMES: Record<UserRole, string> = {
  ARBORIST: 'Arborysta',
  OFFICIAL: 'Urzędnik',
  ADMIN: 'Administrator',
};

/** Panel, do ktorego trafia dana rola po zalogowaniu. Arborysta - od razu lista projektow (bez pulpitu). */
export const PANEL_HOME_PATH_BY_ROLE: Record<UserRole, string> = {
  ARBORIST: '/projects',
  OFFICIAL: '/official',
  ADMIN: '/admin',
};

/** Typy kont do wyboru w formularzu rejestracji (bez admina). */
export const REGISTRATION_ACCOUNT_TYPE_OPTIONS: { role: AssignableUserRole; displayName: string; description: string }[] = [
  {
    role: 'ARBORIST',
    displayName: 'Arborysta',
    description: 'Specjalista od drzew - inwentaryzacje, oceny i dokumentacja prac.',
  },
  {
    role: 'OFFICIAL',
    displayName: 'Urzędnik',
    description: 'Pracownik urzędu - rozpatrywanie wniosków i wydawanie decyzji.',
  },
];

/** Sciezki nalezace do panelu danej roli (poza strona glowna panelu - np. projekty arborysty). */
const PANEL_PATH_PREFIXES_BY_ROLE: Record<UserRole, string[]> = {
  ARBORIST: ['/arborist', '/projects'],
  OFFICIAL: ['/official'],
  ADMIN: ['/admin'],
};

/**
 * Dokad przekierowac po zalogowaniu: na strone, z ktorej uzytkownika wyrzucilo do logowania,
 * ale tylko jesli nalezy do JEGO panelu - inaczej na glowna jego panelu (zeby nie trafil na 403).
 */
export function resolvePanelPathAfterSignIn(role: UserRole, pathBeforeSignIn?: string): string {
  const pathBelongsToRolePanel = !!pathBeforeSignIn
    && PANEL_PATH_PREFIXES_BY_ROLE[role].some((panelPathPrefix) => pathBeforeSignIn.startsWith(panelPathPrefix));
  return pathBelongsToRolePanel ? pathBeforeSignIn! : PANEL_HOME_PATH_BY_ROLE[role];
}
