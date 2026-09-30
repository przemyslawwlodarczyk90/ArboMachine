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
      <LabeledInputField label="Numer inwentaryzacyjny *" autoFocus maxLength={100} value={treeFormValues.inventoryNumber}
                         hintText="Własny identyfikator, np. 1, A-12, PARK-15."
                         onChange={(changeEvent) => changeFormField('inventoryNumber', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.inventoryNumber} />
      <LabeledInputField label="Gatunek (nazwa łacińska)" maxLength={255} placeholder="np. Quercus robur"
                         value={treeFormValues.speciesLatin}
                         onChange={(changeEvent) => changeFormField('speciesLatin', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.speciesLatin} />
      <LabeledInputField label="Gatunek (nazwa polska)" maxLength={255} placeholder="np. dąb szypułkowy"
                         value={treeFormValues.speciesPolish}
                         onChange={(changeEvent) => changeFormField('speciesPolish', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.speciesPolish} />
      {/* Tekst z klawiatura numeryczna zamiast type="number" - ten zle obsluguje przecinek i pozwala na "e". */}
      <LabeledInputField label="Obwód pnia (cm)" inputMode="numeric" placeholder="np. 314"
                         value={treeFormValues.trunkCircumferenceCm}
                         onChange={(changeEvent) => changeFormField('trunkCircumferenceCm', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.trunkCircumferenceCm} />
      <LabeledInputField label="Wysokość (m)" inputMode="decimal" placeholder="np. 18,5"
                         value={treeFormValues.heightM}
                         onChange={(changeEvent) => changeFormField('heightM', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.heightM} />
      <LabeledInputField label="Średnica korony (m)" inputMode="decimal" placeholder="np. 12"
                         value={treeFormValues.crownDiameterM}
                         onChange={(changeEvent) => changeFormField('crownDiameterM', changeEvent.target.value)}
                         errorMessage={errorMessageByFieldName.crownDiameterM} />
      <LabeledTextAreaField label="Uwagi" value={treeFormValues.notes}
                            onChange={(changeEvent) => changeFormField('notes', changeEvent.target.value)}
                            errorMessage={errorMessageByFieldName.notes} />
    </>
  );
}
