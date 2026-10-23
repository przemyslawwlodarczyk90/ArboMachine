import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  hasUnsavedDocumentAuthorProfileChanges, toDocumentAuthorProfileFormValues, toDocumentAuthorProfileRequest,
  validateDocumentAuthorProfileFormValues,
} from './documentAuthorProfileFormValues.ts';

test('brak danych daje puste pola formularza', () => {
  const formValues = toDocumentAuthorProfileFormValues({ hasProfile: false });
  assert.equal(formValues.authorFullName, '');
  assert.equal(formValues.methodsDescription, '');
});

test('request pomija puste pola i przycina tekst', () => {
  const request = toDocumentAuthorProfileRequest({
    ...toDocumentAuthorProfileFormValues(null), authorFullName: '  Anna Dębowa ', qualifications: '   ',
  });
  assert.deepEqual(request, { authorFullName: 'Anna Dębowa' });
});

test('niezapisane zmiany ignoruja same spacje', () => {
  const saved = { hasProfile: true, authorFullName: 'Anna', companyName: null };
  const formValues = toDocumentAuthorProfileFormValues(saved);
  assert.equal(hasUnsavedDocumentAuthorProfileChanges({ ...formValues, authorFullName: 'Anna  ' }, saved), false);
  assert.equal(hasUnsavedDocumentAuthorProfileChanges({ ...formValues, companyName: 'Firma' }, saved), true);
});

test('za dlugi tekst ma blad z limitem pola', () => {
  const errors = validateDocumentAuthorProfileFormValues({
    ...toDocumentAuthorProfileFormValues(null), authorFullName: 'a'.repeat(256), methodsDescription: 'b'.repeat(10000),
  });
  assert.equal(errors.authorFullName, 'Tekst może mieć maksymalnie 255 znaków.');
  assert.equal(errors.methodsDescription, undefined);
});
