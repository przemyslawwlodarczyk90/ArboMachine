// Laczenie istniejacego tekstu pola z podyktowanym fragmentem. Bezpieczne: nigdy nie kasuje tego, co juz jest
// w polu - tylko dopisuje na koncu. Modul bez importow - testowany przez node --test.

/** Konczy zdanie, wiec kolejny fragment zaczynamy wielka litera. */
const SENTENCE_END_PATTERN = /[.!?]$/;

/**
 * Dopisuje transkrypcje do tekstu pola:
 * - puste pole -> sama transkrypcja,
 * - pole z tekstem -> tekst + spacja + transkrypcja (bez podwojnych spacji; nowa linia na koncu zostaje),
 * - po zakonczonym zdaniu transkrypcja zaczyna sie wielka litera.
 */
export function appendTranscription(existingText: string, transcript: string): string {
  const trimmedTranscript = transcript.trim();
  if (!trimmedTranscript) return existingText;
  if (!existingText.trim()) return trimmedTranscript;

  const existingTextWithoutTrailingSpaces = existingText.replace(/[ \t]+$/, '');
  const startsNewSentence = SENTENCE_END_PATTERN.test(existingTextWithoutTrailingSpaces);
  const appendedFragment = startsNewSentence
    ? trimmedTranscript.charAt(0).toLocaleUpperCase('pl-PL') + trimmedTranscript.slice(1)
    : trimmedTranscript;
  const separator = existingTextWithoutTrailingSpaces.endsWith('\n') ? '' : ' ';
  return existingTextWithoutTrailingSpaces + separator + appendedFragment;
}
