import { useSpeechDictation } from './useSpeechDictation';

/**
 * Przycisk dyktowania do pola tekstowego (V1). Nie zna zadnego API aplikacji - tylko oddaje podyktowany fragment
 * przez onTranscript; co z nim zrobic (dopisac do pola) decyduje wywolujacy. Niczego nie zapisuje.
 * Gdy przegladarka nie obsluguje rozpoznawania mowy, przycisk w ogole sie nie pokazuje (progressive enhancement).
 * Stan sluchania jest wyraznie widoczny ("Słucham…"), mikrofon startuje wylacznie po kliknieciu.
 */
export function VoiceInputButton({ onTranscript, disabled = false }: {
  onTranscript: (transcript: string) => void;
  disabled?: boolean;
}) {
  const { isSupported, dictationState, startDictation, stopDictation } = useSpeechDictation(onTranscript);
  if (!isSupported) return null;

  const isListening = dictationState.status === 'listening';
  return (
    <div className={`voice-input${isListening ? ' voice-input-listening' : ''}`}>
      <button type="button" className={`btn btn-small ${isListening ? 'btn-danger-outline' : 'btn-secondary'}`}
              onClick={isListening ? stopDictation : startDictation} disabled={disabled && !isListening}
              aria-pressed={isListening} aria-label={isListening ? 'Zatrzymaj dyktowanie' : 'Dyktuj tekst'}>
        <span aria-hidden="true">{isListening ? '■' : '🎤'}</span> {isListening ? 'Zatrzymaj dyktowanie' : 'Dyktuj'}
      </button>
      {isListening && (
        <span className="voice-input-status small" role="status">
          <span className="voice-input-recording-dot" aria-hidden="true" /> Słucham…
        </span>
      )}
      {dictationState.status === 'failed' && (
        <span className="voice-input-error small" role="alert">{dictationState.errorMessage}</span>
      )}
    </div>
  );
}
