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

/**
 * Kopie odczytow do pracy offline (etap 26, rejestruje je warstwa offline): odpowiedzi GET z tokenem sa zapisywane,
 * a bez polaczenia zwracana jest ostatnia kopia. httpClient nie zna szczegolow magazynu ani endpointow.
 */
export interface ApiReadCache {
  load(path: string): Promise<unknown | undefined>;
  save(path: string, value: unknown): void;
  /** sciezki obiektow istniejacych tylko na urzadzeniu - nigdy nie wysylane do serwera */
  isLocalOnly(path: string): boolean;
  /** czy ostatnie zadanie dotarlo do serwera (dowolna odpowiedz HTTP) - stan "online/offline" w UI */
  reportReachability(reachable: boolean): void;
}

let apiReadCache: ApiReadCache | null = null;

export function registerApiReadCache(cache: ApiReadCache | null) {
  apiReadCache = cache;
}

/** AuthenticationContext rejestruje tu wylogowanie - wywolywane, gdy chroniony endpoint odpowie 401. */
export function registerUnauthorizedResponseHandler(handler: (() => void) | null) {
  handleUnauthorizedResponse = handler;
}

async function convertErrorResponseToApiRequestError(errorResponse: Response): Promise<ApiRequestError> {
  // Tresc bledow 5xx to szczegoly techniczne serwera (np. "Internal Server Error", stack trace) - nie pokazujemy jej.
  if (errorResponse.status >= 500) {
    return new ApiRequestError(errorResponse.status, defaultMessageForHttpStatus(errorResponse.status));
  }
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
    return new ApiRequestError(errorResponse.status, 'Popraw zaznaczone pola.', errorMessageByFieldName);
  }

  const message = typeof responseBody === 'string' && responseBody.trim()
    ? toUserFacingErrorMessage(responseBody)
    : defaultMessageForHttpStatus(errorResponse.status);
  return new ApiRequestError(errorResponse.status, message);
}

function defaultMessageForHttpStatus(httpStatus: number): string {
  if (httpStatus === 401) return toUserFacingErrorMessage('Unauthorized');
  if (httpStatus === 403) return toUserFacingErrorMessage('Forbidden');
  if (httpStatus >= 500) return 'Błąd serwera. Spróbuj ponownie później.';
  return `Żądanie nie powiodło się (${httpStatus}).`;
}

export interface ApiRequestOptions {
  httpMethod?: string;
  requestBody?: unknown;
  /** dolacz naglowek Authorization z zapisanym tokenem */
  sendAccessToken?: boolean;
}

/**
 * Wspolna czesc wszystkich zadan: naglowki, token, blad polaczenia, blad HTTP (w tym 401 -> wylogowanie).
 * Body FormData (upload pliku) idzie bez Content-Type - przegladarka sama ustawia multipart z boundary.
 */
async function fetchApiResponse(
  path: string,
  { httpMethod = 'GET', requestBody, sendAccessToken = false }: ApiRequestOptions,
): Promise<Response> {
  const isMultipartBody = requestBody instanceof FormData;
  const requestHeaders: Record<string, string> = {};
  if (requestBody !== undefined && !isMultipartBody) requestHeaders['Content-Type'] = 'application/json';
  if (sendAccessToken) {
    const storedAccessToken = accessTokenStorage.read();
    if (storedAccessToken) requestHeaders.Authorization = `Bearer ${storedAccessToken}`;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method: httpMethod,
      headers: requestHeaders,
      body: isMultipartBody ? requestBody : requestBody !== undefined ? JSON.stringify(requestBody) : undefined,
    });
  } catch {
    apiReadCache?.reportReachability(false);
    throw new ApiRequestError(0, 'Brak połączenia z serwerem. Sprawdź połączenie z internetem i spróbuj ponownie.');
  }
  apiReadCache?.reportReachability(true);

  if (!response.ok) {
    if (response.status === 401 && sendAccessToken) handleUnauthorizedResponse?.();
    throw await convertErrorResponseToApiRequestError(response);
  }
  return response;
}

/**
 * Pobiera plik binarny (np. zdjecie) jako Blob. Potrzebne, bo chronione pliki wymagaja naglowka Authorization,
 * a zwykly <img src> go nie wysyla.
 */
export async function sendApiRequestForBlob(path: string, options: ApiRequestOptions = {}): Promise<Blob> {
  return (await fetchApiResponse(path, options)).blob();
}

/**
 * Wysyla zadanie JSON (albo FormData) i zwraca odpowiedz JSON; rzuca ApiRequestError przy odpowiedzi innej niz 2xx
 * albo braku polaczenia.
 */
export async function sendApiRequest<ResponseType>(path: string, options: ApiRequestOptions = {}): Promise<ResponseType> {
  const isCacheableRead = apiReadCache !== null && (options.httpMethod ?? 'GET') === 'GET' && options.sendAccessToken === true;
  if (!isCacheableRead) return parseJsonResponse<ResponseType>(await fetchApiResponse(path, options));
  const cache = apiReadCache!;
  // Obiekt utworzony bez polaczenia (tymczasowe id) istnieje tylko na urzadzeniu - nie pytamy serwera.
  if (cache.isLocalOnly(path)) {
    const localValue = await cache.load(path);
    if (localValue !== undefined) return localValue as ResponseType;
    throw new ApiRequestError(404, 'Ten zasób nie jest dostępny na tym urządzeniu.');
  }
  try {
    const value = await parseJsonResponse<ResponseType>(await fetchApiResponse(path, options));
    cache.save(path, value);
    return value;
  } catch (requestError) {
    // Bez polaczenia: ostatnia kopia z tego urzadzenia (praca offline, etap 26).
    if (requestError instanceof ApiRequestError && requestError.httpStatus === 0) {
      const cachedValue = await cache.load(path);
      if (cachedValue !== undefined) return cachedValue as ResponseType;
    }
    throw requestError;
  }
}

async function parseJsonResponse<ResponseType>(response: Response): Promise<ResponseType> {
  if (response.status === 204) return undefined as ResponseType;
  const responseText = await response.text();
  if (!responseText) return undefined as ResponseType;
  try {
    return JSON.parse(responseText) as ResponseType;
  } catch {
    return responseText as ResponseType;
  }
}
