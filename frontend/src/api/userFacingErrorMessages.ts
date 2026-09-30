// Surowe komunikaty z backendu -> przyjazniejsze teksty pokazywane uzytkownikowi. Panel arborysty (projekty, drzewa)
// ma polski UI; komunikaty kont/logowania i paneli urzednika/admina sa na razie po angielsku (docs/DO_DOPRECYZOWANIA.md).
const USER_FACING_MESSAGE_BY_BACKEND_MESSAGE: Record<string, string> = {
  'Invalid email or password': 'Invalid e-mail or password.',
  'User is not activated': 'Your account is not active yet. Click the link in the confirmation e-mail.',
  'Account is locked': 'Your account has been locked by an administrator.',
  'Email is already in use by an active user': 'An account with this e-mail already exists.',
  'Token not found': 'This link is invalid or has already been used.',
  'Token has expired': 'This link has expired.',
  'Failed to send email': 'We could not send the e-mail. Please try again in a moment.',
  'Email cannot be changed': 'The e-mail address cannot be changed.',
  'User not found': 'User not found.',
  'Unauthorized': 'Sesja wygasła. Zaloguj się ponownie.',
  'Forbidden': 'Nie masz uprawnień do wykonania tej operacji.',
  'Malformed request': 'Nieprawidłowe żądanie.',
  'Admin accounts can only be managed directly in the database':
    'Admin accounts can only be managed directly in the database.',
  'Email should be valid': 'Enter a valid e-mail address.',
  'Email is mandatory': 'E-mail is required.',
  'Password is mandatory': 'Password is required.',
  'Password must have at least 8 characters': 'Password must have at least 8 characters.',
  'First name is mandatory': 'First name is required.',
  'Last name is mandatory': 'Last name is required.',
  'Phone number is invalid': 'Invalid phone number.',
  'Postal code is invalid': 'Postal code must look like 00-000.',
  'Token is mandatory': 'The link is missing its token.',
  'User data is mandatory': 'Fill in your details.',
  'Account type is mandatory': 'Choose an account type.',
  // Komunikaty modulow projektow i drzew - po polsku (panel arborysty ma polski UI).
  'Project name is mandatory': 'Nazwa projektu jest wymagana.',
  'Project name must not be blank': 'Nazwa projektu jest wymagana.',
  'Project name must have at most 255 characters': 'Nazwa projektu może mieć maksymalnie 255 znaków.',
  'Project not found': 'Ten projekt nie istnieje albo nie masz do niego dostępu.',
  'Tree not found': 'To drzewo nie istnieje albo nie masz do niego dostępu.',
  'Inventory number is mandatory': 'Numer inwentaryzacyjny jest wymagany.',
  'Inventory number must have at most 100 characters': 'Numer inwentaryzacyjny może mieć maksymalnie 100 znaków.',
  'Latin species name must have at most 255 characters': 'Nazwa łacińska gatunku może mieć maksymalnie 255 znaków.',
  'Polish species name must have at most 255 characters': 'Nazwa polska gatunku może mieć maksymalnie 255 znaków.',
  'Trunk circumference must be a whole number of centimeters': 'Obwód pnia musi być liczbą całkowitą centymetrów.',
  'Trunk circumference must be at least 1 cm': 'Obwód pnia musi mieścić się w zakresie od 1 do 5000 cm.',
  'Trunk circumference must be at most 5000 cm': 'Obwód pnia musi mieścić się w zakresie od 1 do 5000 cm.',
  'Height can have at most 2 decimal places': 'Wysokość może mieć maksymalnie 2 miejsca po przecinku.',
  'Height must be greater than 0 m': 'Wysokość musi być większa od 0 i nie większa niż 150 m.',
  'Height must be at most 150 m': 'Wysokość musi być większa od 0 i nie większa niż 150 m.',
  'Crown diameter can have at most 2 decimal places': 'Średnica korony może mieć maksymalnie 2 miejsca po przecinku.',
  'Crown diameter must be greater than 0 m': 'Średnica korony musi być większa od 0 i nie większa niż 200 m.',
  'Crown diameter must be at most 200 m': 'Średnica korony musi być większa od 0 i nie większa niż 200 m.',
  'Latitude is mandatory': 'Szerokość geograficzna jest wymagana.',
  'Latitude must be between -90 and 90': 'Szerokość geograficzna musi być w zakresie od -90 do 90.',
  'Longitude is mandatory': 'Długość geograficzna jest wymagana.',
  'Longitude must be between -180 and 180': 'Długość geograficzna musi być w zakresie od -180 do 180.',
  'User account owns projects and cannot be deleted':
    'This account owns projects and cannot be deleted yet.',
};

/** Zwraca przyjazny tekst albo oryginalny komunikat, jesli nie ma go w slowniku. */
export function toUserFacingErrorMessage(backendMessage: string): string {
  return USER_FACING_MESSAGE_BY_BACKEND_MESSAGE[backendMessage.trim()] ?? backendMessage;
}
