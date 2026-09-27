// Typy danych wymienianych z backendem - odpowiadaja DTO w pakiecie domain.user.dto.

export type UserRole = 'ARBORIST' | 'OFFICIAL' | 'ADMIN';

/** Role, ktore mozna wybrac przy rejestracji albo nadac z panelu admina (nigdy ADMIN). */
export type AssignableUserRole = Exclude<UserRole, 'ADMIN'>;

/** Edytowalne dane profilu (UserProfileDto). */
export interface UserProfile {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  street?: string;
  postalCode?: string;
  city?: string;
}

/** Konto zwracane przez API (UserAccountDto): profil + rola + flagi statusu. */
export interface UserAccount extends UserProfile {
  id: number;
  role: UserRole;
  /** uzytkownik kliknal link z maila aktywacyjnego (albo admin potwierdzil e-mail za niego) */
  emailConfirmed: boolean;
  /** konto zablokowane przez admina */
  lockedByAdmin: boolean;
}

export interface UserRegistrationRequest {
  profile: UserProfile;
  password: string;
  accountType: AssignableUserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface RolePanelWelcome {
  role: UserRole;
  firstName: string;
  lastName: string;
  welcomeMessage: string;
}
