import { useId, type InputHTMLAttributes } from 'react';

interface LabeledInputFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  /** komunikat bledu walidacji - podswietla pole na czerwono */
  errorMessage?: string;
  /** podpowiedz pod polem, pokazywana gdy nie ma bledu */
  hintText?: string;
}

/** Pole tekstowe z etykieta, powiazane z komunikatem bledu/podpowiedzia przez aria-describedby. */
export function LabeledInputField({ label, errorMessage, hintText, ...inputAttributes }: LabeledInputFieldProps) {
  const inputId = useId();
  const describedById = errorMessage ? `${inputId}-error` : hintText ? `${inputId}-hint` : undefined;
  return (
    <div className={`field${errorMessage ? ' field-invalid' : ''}`}>
      <label htmlFor={inputId}>{label}</label>
      <input id={inputId} aria-invalid={!!errorMessage} aria-describedby={describedById} {...inputAttributes} />
      {errorMessage ? (
        <span id={`${inputId}-error`} className="field-error">{errorMessage}</span>
      ) : hintText ? (
        <span id={`${inputId}-hint`} className="field-hint">{hintText}</span>
      ) : null}
    </div>
  );
}
