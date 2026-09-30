/** Musi sie zgadzac z @Size(min = 8) na backendzie. */
export const MINIMUM_PASSWORD_LENGTH = 8;

/**
 * Sprawdza nowe haslo i jego powtorzenie. Zwraca bledy pod kluczami `passwordFieldName`
 * i `repeatedPassword`; pusty obiekt oznacza, ze wszystko jest w porzadku.
 */
export function validateNewPasswordAndRepetition(
  newPassword: string,
  repeatedPassword: string,
  passwordFieldName: string,
): Record<string, string> {
  const errorMessageByFieldName: Record<string, string> = {};
  if (newPassword.length < MINIMUM_PASSWORD_LENGTH) {
    errorMessageByFieldName[passwordFieldName] = `Hasło musi mieć co najmniej ${MINIMUM_PASSWORD_LENGTH} znaków.`;
  }
  if (newPassword !== repeatedPassword) {
    errorMessageByFieldName.repeatedPassword = 'Hasła nie są takie same.';
  }
  return errorMessageByFieldName;
}
