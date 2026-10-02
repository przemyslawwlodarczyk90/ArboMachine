import { LabeledInputField } from '../../components/LabeledInputField';

export interface ProjectOrderFieldValues {
  clientName: string;
  siteAddress: string;
  /** 'YYYY-MM-DD' albo '' */
  orderDate: string;
}

/** Dane zlecenia projektu (etap 29) - wspolne pola okien "Nowy projekt" i "Edytuj projekt"; wszystkie opcjonalne. */
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
    </>
  );
}
