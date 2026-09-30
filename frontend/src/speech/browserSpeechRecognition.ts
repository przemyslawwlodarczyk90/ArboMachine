import type { CreateSpeechRecognition, SpeechRecognitionLike } from './dictationController.ts';

/**
 * JEDYNE miejsce, ktore zna nazwy Web Speech API przegladarki (SpeechRecognition / webkitSpeechRecognition).
 * Zwraca fabryke obiektu rozpoznawania mowy albo null, gdy przegladarka go nie ma (np. Firefox) - wtedy funkcja
 * dyktowania jest po prostu ukryta, a formularze dzialaja normalnie. Obiekt globalny mozna podmienic w testach.
 */
export function findBrowserSpeechRecognition(globalObject: object = globalThis): CreateSpeechRecognition | null {
  const globalScope = globalObject as Record<string, unknown>;
  const speechRecognitionConstructor = globalScope.SpeechRecognition ?? globalScope.webkitSpeechRecognition;
  if (typeof speechRecognitionConstructor !== 'function') return null;
  const SpeechRecognitionConstructor = speechRecognitionConstructor as new () => SpeechRecognitionLike;
  return () => new SpeechRecognitionConstructor();
}
