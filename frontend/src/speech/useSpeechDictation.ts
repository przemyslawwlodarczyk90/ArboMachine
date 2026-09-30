import { useEffect, useRef, useState } from 'react';
import { findBrowserSpeechRecognition } from './browserSpeechRecognition';
import { DictationController, type DictationState } from './dictationController';

/**
 * Hook dyktowania do jednego pola: cienka warstwa Reacta nad DictationController. Kontroler powstaje raz na pole,
 * a przy odmontowaniu (np. przejscie na inna strone) mikrofon jest wylaczany. Najnowszy callback w refie -
 * transkrypcja zawsze dopisuje sie do aktualnej wartosci pola.
 */
export function useSpeechDictation(onTranscript: (transcript: string) => void) {
  const onTranscriptRef = useRef(onTranscript);
  onTranscriptRef.current = onTranscript;
  const [dictationState, setDictationState] = useState<DictationState>({ status: 'idle' });
  const controllerRef = useRef<DictationController | null>(null);
  if (!controllerRef.current) {
    controllerRef.current = new DictationController(findBrowserSpeechRecognition(), setDictationState,
      (transcript) => onTranscriptRef.current(transcript));
  }

  useEffect(() => {
    const controller = controllerRef.current;
    return () => controller?.dispose();
  }, []);

  const controller = controllerRef.current;
  return {
    isSupported: controller.isSupported,
    dictationState,
    startDictation: () => controller.start(),
    stopDictation: () => controller.stop(),
  };
}
