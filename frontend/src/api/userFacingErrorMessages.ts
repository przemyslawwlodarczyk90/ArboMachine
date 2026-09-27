// Surowe komunikaty z backendu -> przyjazniejsze teksty pokazywane uzytkownikowi (UI jest po angielsku).
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
  'Unauthorized': 'Your session has expired. Please sign in again.',
  'Forbidden': 'You do not have permission to do this.',
  'Malformed request': 'The request was malformed.',
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
  'Project name is mandatory': 'Project name is required.',
  'Project name must not be blank': 'Project name is required.',
  'Project name must have at most 255 characters': 'Project name can have at most 255 characters.',
  'Project not found': 'This project does not exist or you do not have access to it.',
  'Tree not found': 'This tree does not exist or you do not have access to it.',
  'Inventory number is mandatory': 'Inventory number is required.',
  'Inventory number must have at most 100 characters': 'Inventory number can have at most 100 characters.',
  'Latin species name must have at most 255 characters': 'Latin species name can have at most 255 characters.',
  'User account owns projects and cannot be deleted':
    'This account owns projects and cannot be deleted yet.',
};

/** Zwraca przyjazny tekst albo oryginalny komunikat, jesli nie ma go w slowniku. */
export function toUserFacingErrorMessage(backendMessage: string): string {
  return USER_FACING_MESSAGE_BY_BACKEND_MESSAGE[backendMessage.trim()] ?? backendMessage;
}
