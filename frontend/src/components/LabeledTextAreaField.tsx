import { useId, type TextareaHTMLAttributes } from 'react';

interface LabeledTextAreaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  /** komunikat bledu walidacji - podswietla pole na czerwono */
  errorMessage?: string;
  /** podpowiedz pod polem, pokazywana gdy nie ma bledu */
  hintText?: string;
}

/** Wieloliniowy odpowiednik LabeledInputField (te same klasy CSS i powiazania aria). */
export function LabeledTextAreaField({ label, errorMessage, hintText, ...textAreaAttributes }: LabeledTextAreaFieldProps) {
  const textAreaId = useId();
  const describedById = errorMessage ? `${textAreaId}-error` : hintText ? `${textAreaId}-hint` : undefined;
  return (
    <div className={`field${errorMessage ? ' field-invalid' : ''}`}>
      <label htmlFor={textAreaId}>{label}</label>
      <textarea id={textAreaId} aria-invalid={!!errorMessage} aria-describedby={describedById} rows={4}
                {...textAreaAttributes} />
      {errorMessage ? (
        <span id={`${textAreaId}-error`} className="field-error">{errorMessage}</span>
      ) : hintText ? (
        <span id={`${textAreaId}-hint`} className="field-hint">{hintText}</span>
      ) : null}
    </div>
  );
}
