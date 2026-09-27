import { LabeledInputField } from '../../components/LabeledInputField';
import { LabeledTextAreaField } from '../../components/LabeledTextAreaField';
import type { TreeFormValues } from './treeFormValues';

/**
 * Pola formularza drzewa wspolne dla dodawania i edycji - w kolejnosci pracy przy drzewie, nie kolumn w bazie.
 * Komponent tylko wyswietla pola; stan, walidacje i wysylanie trzyma formularz, ktory go uzywa.
 */
export function TreeFormFields({ treeFormValues, errorMessageByFieldName, changeFormField }: {
  treeFormValues: TreeFormValues;
  errorMessageByFieldName: Record<string, string>;
  changeFormField: (fieldName: keyof TreeFormValues, newValue: string) => void;
}) {
  return (
    <>
      <LabeledInputField label="Inventory number *" autoFocus maxLength={100} value={treeFormValues.inventoryNumber}
                         hintText="Your own identifier, e.g. 1, A-12, PARK-15."
                         onChange={(changeEvent) => changeFormField('inventoryNumber', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.inventoryNumber} />
      <LabeledInputField label="Latin species" maxLength={255} placeholder="e.g. Quercus robur"
                         value={treeFormValues.speciesLatin}
                         onChange={(changeEvent) => changeFormField('speciesLatin', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.speciesLatin} />
      <LabeledInputField label="Polish species" maxLength={255} placeholder="e.g. dąb szypułkowy"
                         value={treeFormValues.speciesPolish}
                         onChange={(changeEvent) => changeFormField('speciesPolish', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.speciesPolish} />
      {/* Tekst z klawiatura numeryczna zamiast type="number" - ten zle obsluguje przecinek i pozwala na "e". */}
      <LabeledInputField label="Trunk circumference (cm)" inputMode="numeric" placeholder="e.g. 314"
                         value={treeFormValues.trunkCircumferenceCm}
                         onChange={(changeEvent) => changeFormField('trunkCircumferenceCm', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.trunkCircumferenceCm} />
      <LabeledInputField label="Height (m)" inputMode="decimal" placeholder="e.g. 18.5"
                         value={treeFormValues.heightM}
                         onChange={(changeEvent) => changeFormField('heightM', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.heightM} />
      <LabeledTextAreaField label="Notes" value={treeFormValues.notes}
                            onChange={(changeEvent) => changeFormField('notes', changeEvent.target.value)}
                            errorMessage={errorMessageByFieldName.notes} />
    </>
  );
}
