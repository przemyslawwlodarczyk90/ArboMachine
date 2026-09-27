import { toUserFacingErrorMessage } from './userFacingErrorMessages';

// Wspolna warstwa HTTP dla wszystkich modulow. Nie zna zadnych konkretnych endpointow -
// te zyja w plikach *Api.ts poszczegolnych modulow.

const ACCESS_TOKEN_STORAGE_KEY = 'arbomachine.accessToken';

/** Token JWT trzymany w localStorage, zeby sesja przetrwala odswiezenie strony. */
export const accessTokenStorage = {
  read: (): string | null => localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY),
  save: (accessToken: string) => localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken),
  remove: () => localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY),
};

/** Blad zadania do API z juz przetlumaczonym komunikatem. */
export class ApiRequestError extends Error {
  readonly httpStatus: number;
  /** Bledy walidacji pol (klucz = nazwa pola, bez prefiksu "profile."). */
  readonly errorMessageByFieldName: Record<string, string>;

  constructor(httpStatus: number, message: string, errorMessageByFieldName: Record<string, string> = {}) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorMessageByFieldName = errorMessageByFieldName;
  }
}

let handleUnauthorizedResponse: (() => void) | null = null;

/** AuthenticationContext rejestruje tu wylogowanie - wywolywane, gdy chroniony endpoint odpowie 401. */
export function registerUnauthorizedResponseHandler(handler: (() => void) | null) {
  handleUnauthorizedResponse = handler;
}

async function convertErrorResponseToApiRequestError(errorResponse: Response): Promise<ApiRequestError> {
  const responseText = await errorResponse.text();
  let responseBody: unknown = responseText;
  try {
    responseBody = JSON.parse(responseText);
  } catch {
    // zwykly tekst, nie JSON
  }

  if (responseBody && typeof responseBody === 'object' && !Array.isArray(responseBody)) {
    const responseFields = responseBody as Record<string, unknown>;
    // Wlasny format bledow Springa (np. 401/403 z warstwy security): { status, error, message, path }.
    if ('status' in responseFields && 'error' in responseFields) {
      const springErrorMessage = String(responseFields.message || responseFields.error);
      return new ApiRequestError(errorResponse.status, toUserFacingErrorMessage(springErrorMessage));
    }
    // Bledy walidacji: { "nazwaPola": "komunikat" }.
    const errorMessageByFieldName: Record<string, string> = {};
    for (const [fieldPath, fieldErrorMessage] of Object.entries(responseFields)) {
      errorMessageByFieldName[fieldPath.replace(/^profile\./, '')] = toUserFacingErrorMessage(String(fieldErrorMessage));
    }
    return new ApiRequestError(errorResponse.status, 'Please correct the highlighted fields.', errorMessageByFieldName);
  }

  const message = typeof responseBody === 'string' && responseBody.trim()
    ? toUserFacingErrorMessage(responseBody)
    : defaultMessageForHttpStatus(errorResponse.status);
  return new ApiRequestError(errorResponse.status, message);
}

function defaultMessageForHttpStatus(httpStatus: number): string {
  if (httpStatus === 401) return toUserFacingErrorMessage('Unauthorized');
  if (httpStatus === 403) return toUserFacingErrorMessage('Forbidden');
  if (httpStatus >= 500) return 'Server error. Please try again later.';
  return `Request failed (${httpStatus}).`;
}

export interface ApiRequestOptions {
  httpMethod?: string;
  requestBody?: unknown;
  /** dolacz naglowek Authorization z zapisanym tokenem */
  sendAccessToken?: boolean;
}

/** Wysyla zadanie JSON; rzuca ApiRequestError przy odpowiedzi innej niz 2xx albo braku polaczenia. */
export async function sendApiRequest<ResponseType>(
  path: string,
  { httpMethod = 'GET', requestBody, sendAccessToken = false }: ApiRequestOptions = {},
): Promise<ResponseType> {
  const requestHeaders: Record<string, string> = {};
  if (requestBody !== undefined) requestHeaders['Content-Type'] = 'application/json';
  if (sendAccessToken) {
    const storedAccessToken = accessTokenStorage.read();
    if (storedAccessToken) requestHeaders.Authorization = `Bearer ${storedAccessToken}`;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method: httpMethod,
      headers: requestHeaders,
      body: requestBody !== undefined ? JSON.stringify(requestBody) : undefined,
    });
  } catch {
    throw new ApiRequestError(0, 'Cannot reach the server. Is the backend running?');
  }

  if (!response.ok) {
    if (response.status === 401 && sendAccessToken) handleUnauthorizedResponse?.();
    throw await convertErrorResponseToApiRequestError(response);
  }

  if (response.status === 204) return undefined as ResponseType;
  const responseText = await response.text();
  if (!responseText) return undefined as ResponseType;
  try {
    return JSON.parse(responseText) as ResponseType;
  } catch {
    return responseText as ResponseType;
  }
}
