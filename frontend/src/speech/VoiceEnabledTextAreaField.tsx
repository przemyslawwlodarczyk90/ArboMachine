import { useRef, type TextareaHTMLAttributes } from 'react';
import { LabeledTextAreaField } from '../components/LabeledTextAreaField';
import { appendTranscription } from './appendTranscription';
import { VoiceInputButton } from './VoiceInputButton';

/**
 * Pole wieloliniowe z opcjonalnym dyktowaniem: podyktowany tekst jest DOPISYWANY na koncu (appendTranscription),
 * nigdy nie zastepuje tego, co juz wpisano. Uzytkownik moze go poprawic przed zapisem formularza.
 * Uzywac tylko przy polach opisowych (notatki, opisy) - nie przy numerach, nazwach lacinskich ani pomiarach.
 */
export function VoiceEnabledTextAreaField({ label, value, onValueChange, errorMessage, hintText, rows, disabled }: {
  label: string;
  value: string;
  onValueChange: (newValue: string) => void;
  errorMessage?: string;
  hintText?: string;
  rows?: TextareaHTMLAttributes<HTMLTextAreaElement>['rows'];
  disabled?: boolean;
}) {
  // Transkrypcja przychodzi asynchronicznie - dopisujemy do NAJNOWSZEJ wartosci pola, a nie z chwili startu.
  const currentValueRef = useRef(value);
  currentValueRef.current = value;

  return (
    <LabeledTextAreaField label={label} value={value} rows={rows} disabled={disabled} errorMessage={errorMessage}
                          hintText={hintText}
                          onChange={(changeEvent) => onValueChange(changeEvent.target.value)}
                          fieldAccessory={
                            <VoiceInputButton disabled={disabled}
                                              onTranscript={(transcript) => {
                                                const valueWithTranscript = appendTranscription(currentValueRef.current, transcript);
                                                // Od razu, a nie po renderze - kolejny fragment dopisze sie do tej wartosci.
                                                currentValueRef.current = valueWithTranscript;
                                                onValueChange(valueWithTranscript);
                                              }} />
                          } />
  );
}
