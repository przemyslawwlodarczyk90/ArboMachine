// Logika dyktowania niezalezna od Reacta i od konkretnej przegladarki: dostaje fabryke obiektu rozpoznawania mowy
// (w przegladarce - Web Speech API, w testach - falszywy obiekt), zarzadza stanem i zglasza transkrypcje.
// Tylko tutaj (i w browserSpeechRecognition.ts) wiadomo cokolwiek o API rozpoznawania mowy.

/** Minimalny podzbior Web Speech API (SpeechRecognition), z ktorego korzystamy. */
export interface SpeechRecognitionLike {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((resultEvent: SpeechRecognitionResultEventLike) => void) | null;
  onerror: ((errorEvent: { error: string }) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

export interface SpeechRecognitionResultEventLike {
  resultIndex: number;
  results: ArrayLike<ArrayLike<{ transcript: string }> & { isFinal: boolean }>;
}

export type CreateSpeechRecognition = () => SpeechRecognitionLike;

export type DictationState =
  | { status: 'idle' }
  | { status: 'listening' }
  | { status: 'failed'; errorMessage: string };

/** Jezyk rozpoznawania V1 - UI i opisy arborysty sa po polsku (bez wyboru jezyka). */
export const DICTATION_LANGUAGE = 'pl-PL';

/**
 * Kody bledow Web Speech API -> komunikat dla uzytkownika po polsku. null = normalne zakonczenie bez komunikatu
 * (np. uzytkownik sam zatrzymal dyktowanie). Surowy wyjatek przegladarki nigdy nie trafia do UI.
 */
export function toDictationErrorMessage(speechErrorCode: string): string | null {
  switch (speechErrorCode) {
    case 'aborted':
      return null;
    case 'not-allowed':
    case 'service-not-allowed':
      return 'Nie udało się uzyskać dostępu do mikrofonu.';
    case 'no-speech':
      return 'Nie wykryto mowy. Spróbuj ponownie.';
    case 'audio-capture':
      return 'Nie znaleziono mikrofonu.';
    case 'network':
      return 'Rozpoznawanie mowy wymaga połączenia z internetem.';
    default:
      return 'Nie udało się rozpoznać mowy. Spróbuj ponownie.';
  }
}

/** Aktualnie sluchajacy kontroler - jedna sesja mikrofonu naraz w calej aplikacji. */
let activeDictationController: DictationController | null = null;

/**
 * Jedna sesja dyktowania do jednego pola. start() uruchamia mikrofon wylacznie na zadanie uzytkownika
 * (dopiero wtedy przegladarka pyta o zgode); kazdy koncowy fragment trafia do onTranscript. Nic nie jest zapisywane
 * ani wysylane dalej - tekst trafia tylko do pola formularza. Audio nie jest przechowywane.
 */
export class DictationController {
  private speechRecognition: SpeechRecognitionLike | null = null;
  private state: DictationState = { status: 'idle' };
  private readonly createSpeechRecognition: CreateSpeechRecognition | null;
  private readonly onStateChange: (state: DictationState) => void;
  private readonly onTranscript: (transcript: string) => void;

  constructor(createSpeechRecognition: CreateSpeechRecognition | null,
              onStateChange: (state: DictationState) => void,
              onTranscript: (transcript: string) => void) {
    this.createSpeechRecognition = createSpeechRecognition;
    this.onStateChange = onStateChange;
    this.onTranscript = onTranscript;
  }

  get isSupported(): boolean {
    return this.createSpeechRecognition !== null;
  }

  get currentState(): DictationState {
    return this.state;
  }

  start(): void {
    if (!this.createSpeechRecognition || this.state.status === 'listening') return;
    // Inne pole wlasnie slucha - zatrzymujemy je, zeby nie bylo dwoch mikrofonow naraz.
    if (activeDictationController && activeDictationController !== this) activeDictationController.stop();

    const speechRecognition = this.createSpeechRecognition();
    speechRecognition.lang = DICTATION_LANGUAGE;
    speechRecognition.continuous = true;
    // Tylko wyniki koncowe - prosto i stabilnie, bez migajacego tekstu w polu.
    speechRecognition.interimResults = false;
    speechRecognition.onresult = (resultEvent) => this.handleResult(resultEvent);
    speechRecognition.onerror = (errorEvent) => this.handleError(errorEvent.error);
    speechRecognition.onend = () => this.handleEnd(speechRecognition);

    this.speechRecognition = speechRecognition;
    activeDictationController = this;
    try {
      speechRecognition.start();
      this.changeState({ status: 'listening' });
    } catch {
      this.releaseSpeechRecognition();
      this.changeState({ status: 'failed', errorMessage: 'Nie udało się uruchomić dyktowania.' });
    }
  }

  /** Zatrzymanie na zadanie uzytkownika - powrot do "Dyktuj" bez komunikatu. */
  stop(): void {
    const speechRecognition = this.speechRecognition;
    this.releaseSpeechRecognition();
    speechRecognition?.stop();
    if (this.state.status === 'listening') this.changeState({ status: 'idle' });
  }

  /** Sprzatanie przy odmontowaniu pola / opuszczeniu strony: mikrofon zostaje wylaczony, bez dalszych zdarzen. */
  dispose(): void {
    const speechRecognition = this.speechRecognition;
    this.releaseSpeechRecognition();
    speechRecognition?.abort();
  }

  private handleResult(resultEvent: SpeechRecognitionResultEventLike): void {
    let finalTranscript = '';
    for (let resultIndex = resultEvent.resultIndex; resultIndex < resultEvent.results.length; resultIndex++) {
      const result = resultEvent.results[resultIndex];
      if (result.isFinal && result.length > 0) finalTranscript += ` ${result[0].transcript}`;
    }
    if (finalTranscript.trim()) this.onTranscript(finalTranscript.trim());
  }

  private handleError(speechErrorCode: string): void {
    const errorMessage = toDictationErrorMessage(speechErrorCode);
    if (errorMessage) this.changeState({ status: 'failed', errorMessage });
  }

  /** Przegladarka konczy sesje sama (cisza, blad, limit czasu) - wracamy do "Dyktuj", komunikat bledu zostaje. */
  private handleEnd(endedSpeechRecognition: SpeechRecognitionLike): void {
    if (this.speechRecognition !== endedSpeechRecognition) return;
    this.releaseSpeechRecognition();
    if (this.state.status === 'listening') this.changeState({ status: 'idle' });
  }

  private releaseSpeechRecognition(): void {
    if (this.speechRecognition) {
      this.speechRecognition.onresult = null;
      this.speechRecognition.onerror = null;
      this.speechRecognition.onend = null;
    }
    this.speechRecognition = null;
    if (activeDictationController === this) activeDictationController = null;
  }

  private changeState(newState: DictationState): void {
    this.state = newState;
    this.onStateChange(newState);
  }
}
