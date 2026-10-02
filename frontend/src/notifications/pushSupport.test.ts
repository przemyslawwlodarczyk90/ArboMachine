import assert from 'node:assert/strict';
import { test } from 'node:test';
import { base64UrlToBytes, pushAvailabilityMessage } from './pushSupport.ts';

test('klucz VAPID base64url (bez paddingu, z - i _) -> bajty', () => {
  const bytes = base64UrlToBytes('BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8');
  assert.equal(bytes.length, 65);
  assert.equal(bytes[0], 0x04);
  assert.deepEqual([...base64UrlToBytes('-_8')], [0xfb, 0xff]);
});

test('komunikaty o niedostepnosci push po polsku; dostepne = brak komunikatu', () => {
  assert.equal(pushAvailabilityMessage('AVAILABLE'), null);
  assert.match(pushAvailabilityMessage('BLOCKED') ?? '', /zablokowane/);
  assert.match(pushAvailabilityMessage('INSECURE_CONTEXT') ?? '', /HTTPS/);
});
