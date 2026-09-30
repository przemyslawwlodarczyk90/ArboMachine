import assert from 'node:assert/strict';
import { test } from 'node:test';
import { appendTranscription } from './appendTranscription.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('puste pole dostaje sama transkrypcje', () => {
  assert.equal(appendTranscription('', ' posusz w koronie '), 'posusz w koronie');
  assert.equal(appendTranscription('   ', 'posusz'), 'posusz');
});

test('istniejacy tekst NIE jest kasowany - transkrypcja jest dopisywana ze spacja', () => {
  assert.equal(appendTranscription('Pomiar dalmierzem', 'korona asymetryczna'), 'Pomiar dalmierzem korona asymetryczna');
  assert.equal(appendTranscription('Pomiar dalmierzem   ', 'korona'), 'Pomiar dalmierzem korona');
});

test('po zakonczonym zdaniu nowy fragment zaczyna sie wielka litera (takze polska)', () => {
  assert.equal(appendTranscription('Pomiar dalmierzem.', 'korona asymetryczna'), 'Pomiar dalmierzem. Korona asymetryczna');
  assert.equal(appendTranscription('Uwaga!', 'żywotność obniżona'), 'Uwaga! Żywotność obniżona');
});

test('nowa linia na koncu zostaje, bez dodatkowej spacji', () => {
  assert.equal(appendTranscription('Pierwsza linia\n', 'druga linia'), 'Pierwsza linia\ndruga linia');
});

test('pusta transkrypcja nie zmienia pola', () => {
  assert.equal(appendTranscription('Bez zmian', '   '), 'Bez zmian');
});
