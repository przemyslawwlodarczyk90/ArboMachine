import { sendApiRequest } from './httpClient';
import type { LoginRequest, LoginResponse, UserRegistrationRequest } from './apiDataTypes';

/** Publiczne endpointy kont: rejestracja, logowanie, potwierdzenie e-maila i reset hasla. */
export const authenticationApi = {
  registerNewUserAccount: (registrationRequest: UserRegistrationRequest) =>
    sendApiRequest<string>('/api/users/register', { httpMethod: 'POST', requestBody: registrationRequest }),

  signIn: (loginRequest: LoginRequest) =>
    sendApiRequest<LoginResponse>('/api/users/login', { httpMethod: 'POST', requestBody: loginRequest }),

  confirmEmailUsingToken: (confirmationToken: string) =>
    sendApiRequest<string>(`/api/users/confirm?token=${encodeURIComponent(confirmationToken)}`),

  requestPasswordResetLink: (email: string) =>
    sendApiRequest<string>('/api/users/password-reset/request', { httpMethod: 'POST', requestBody: { email } }),

  validatePasswordResetToken: (resetToken: string) =>
    sendApiRequest<string>(`/api/users/password-reset/validate?token=${encodeURIComponent(resetToken)}`),

  resetPasswordUsingToken: (resetToken: string, newPassword: string) =>
    sendApiRequest<string>('/api/users/password-reset/confirm', {
      httpMethod: 'POST',
      requestBody: { resetToken, newPassword },
    }),
};
