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
    errorMessageByFieldName[passwordFieldName] = `Password must have at least ${MINIMUM_PASSWORD_LENGTH} characters.`;
  }
  if (newPassword !== repeatedPassword) {
    errorMessageByFieldName.repeatedPassword = 'Passwords do not match.';
  }
  return errorMessageByFieldName;
}
