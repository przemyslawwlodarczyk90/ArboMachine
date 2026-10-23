import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';

export interface ProjectOrderFieldValues {
  clientName: string;
  siteAddress: string;
  /** 'YYYY-MM-DD' albo '' */
  orderDate: string;
  /** etap 45 */
  contractNumber: string;
  documentPurpose: string;
}

export const toProjectOrderFieldValues = (project?: {
  clientName?: string | null; siteAddress?: string | null; orderDate?: string | null;
  contractNumber?: string | null; documentPurpose?: string | null;
}): ProjectOrderFieldValues => ({
  clientName: project?.clientName ?? '', siteAddress: project?.siteAddress ?? '', orderDate: project?.orderDate ?? '',
  contractNumber: project?.contractNumber ?? '', documentPurpose: project?.documentPurpose ?? '',
});

/** Dane zlecenia projektu (etap 29, numer umowy i cel - etap 45) - wspolne pola okien "Nowy projekt" i "Edytuj projekt"; wszystkie opcjonalne. */
export function ProjectOrderFields({ values, onChange, errorMessageByFieldName, disabled }: {
  values: ProjectOrderFieldValues;
  onChange: (values: ProjectOrderFieldValues) => void;
  errorMessageByFieldName: Record<string, string>;
  disabled?: boolean;
}) {
  return (
    <>
      <LabeledInputField label="Zleceniodawca" value={values.clientName} maxLength={255} disabled={disabled}
                         hintText="Np. gmina, zarządca terenu, inwestor (opcjonalnie)."
                         onChange={(changeEvent) => onChange({ ...values, clientName: changeEvent.target.value })}
                         errorMessage={errorMessageByFieldName.clientName} />
      <LabeledInputField label="Adres obiektu" value={values.siteAddress} maxLength={500} disabled={disabled}
                         hintText="Miejsce prac - adres albo opis lokalizacji (opcjonalnie)."
                         onChange={(changeEvent) => onChange({ ...values, siteAddress: changeEvent.target.value })}
                         errorMessage={errorMessageByFieldName.siteAddress} />
      <LabeledInputField label="Data zlecenia" type="date" value={values.orderDate} disabled={disabled}
                         onChange={(changeEvent) => onChange({ ...values, orderDate: changeEvent.target.value })}
                         errorMessage={errorMessageByFieldName.orderDate} />
      <LabeledInputField label="Numer umowy" value={values.contractNumber} maxLength={100} disabled={disabled}
                         hintText="Numer umowy albo zlecenia - pojawi się na stronie tytułowej dokumentu (opcjonalnie)."
                         onChange={(changeEvent) => onChange({ ...values, contractNumber: changeEvent.target.value })}
                         errorMessage={errorMessageByFieldName.contractNumber} />
      <LabeledTextAreaField label="Cel opracowania" value={values.documentPurpose} maxLength={2000} rows={3} disabled={disabled}
                            hintText="Np. inwentaryzacja przed przebudową, wniosek o wycinkę - do sekcji 1 dokumentu (opcjonalnie)."
                            onChange={(changeEvent) => onChange({ ...values, documentPurpose: changeEvent.target.value })}
                            errorMessage={errorMessageByFieldName.documentPurpose} />
    </>
  );
}
