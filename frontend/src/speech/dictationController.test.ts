import assert from 'node:assert/strict';
import { test } from 'node:test';
import { findBrowserSpeechRecognition } from './browserSpeechRecognition.ts';
import {
  DictationController, toDictationErrorMessage, type DictationState, type SpeechRecognitionLike,
} from './dictationController.ts';

// Uruchamianie: npm test (node --test). Zamiast mikrofonu - falszywy obiekt rozpoznawania mowy (adapter).

class FakeSpeechRecognition implements SpeechRecognitionLike {
  lang = '';
  continuous = false;
  interimResults = true;
  onresult: SpeechRecognitionLike['onresult'] = null;
  onerror: SpeechRecognitionLike['onerror'] = null;
  onend: SpeechRecognitionLike['onend'] = null;
  isRunning = false;
  wasAborted = false;
  start() { this.isRunning = true; }
  stop() { this.isRunning = false; this.onend?.(); }
  abort() { this.isRunning = false; this.wasAborted = true; this.onend?.(); }
  say(transcript: string, isFinal = true) {
    this.onresult?.({ resultIndex: 0, results: [Object.assign([{ transcript }], { isFinal })] });
  }
  fail(errorCode: string) { this.onerror?.({ error: errorCode }); this.stop(); }
}

function createControllerWithFakeRecognition() {
  const createdRecognitions: FakeSpeechRecognition[] = [];
  const states: DictationState[] = [];
  const transcripts: string[] = [];
  const controller = new DictationController(() => {
    const recognition = new FakeSpeechRecognition();
    createdRecognitions.push(recognition);
    return recognition;
  }, (state) => states.push(state), (transcript) => transcripts.push(transcript));
  return { controller, createdRecognitions, states, transcripts };
}

test('przegladarka bez wsparcia: brak fabryki, kontroler niczego nie uruchamia', () => {
  assert.equal(findBrowserSpeechRecognition({}), null);
  const states: DictationState[] = [];
  const controller = new DictationController(null, (state) => states.push(state), () => {});
  assert.equal(controller.isSupported, false);
  controller.start();
  assert.deepEqual(states, []);
});

test('przegladarka ze wsparciem: wykrywa SpeechRecognition i webkitSpeechRecognition', () => {
  assert.ok(findBrowserSpeechRecognition({ webkitSpeechRecognition: FakeSpeechRecognition }));
  assert.ok(findBrowserSpeechRecognition({ SpeechRecognition: FakeSpeechRecognition })?.() instanceof FakeSpeechRecognition);
});

test('mikrofon startuje dopiero na start(), w jezyku pl-PL, tylko wyniki koncowe', () => {
  const { controller, createdRecognitions, states } = createControllerWithFakeRecognition();
  assert.equal(createdRecognitions.length, 0);
  controller.start();
  const recognition = createdRecognitions[0];
  assert.ok(recognition.isRunning);
  assert.equal(recognition.lang, 'pl-PL');
  assert.equal(recognition.interimResults, false);
  assert.deepEqual(states.at(-1), { status: 'listening' });
});

test('koncowa transkrypcja trafia do callbacku, wyniki posrednie nie', () => {
  const { controller, createdRecognitions, transcripts } = createControllerWithFakeRecognition();
  controller.start();
  createdRecognitions[0].say('korona asym', false);
  createdRecognitions[0].say(' korona asymetryczna ');
  assert.deepEqual(transcripts, ['korona asymetryczna']);
});

test('zatrzymanie przez uzytkownika wraca do idle bez komunikatu', () => {
  const { controller, createdRecognitions, states } = createControllerWithFakeRecognition();
  controller.start();
  controller.stop();
  assert.equal(createdRecognitions[0].isRunning, false);
  assert.deepEqual(states.at(-1), { status: 'idle' });
});

test('odmowa mikrofonu: polski komunikat, bez surowego wyjatku', () => {
  const { controller, createdRecognitions, states } = createControllerWithFakeRecognition();
  controller.start();
  createdRecognitions[0].fail('not-allowed');
  assert.deepEqual(states.at(-1), { status: 'failed', errorMessage: 'Nie udało się uzyskać dostępu do mikrofonu.' });
});

test('mapowanie bledow: cisza, brak mikrofonu, siec, przerwanie, nieznany', () => {
  assert.equal(toDictationErrorMessage('no-speech'), 'Nie wykryto mowy. Spróbuj ponownie.');
  assert.equal(toDictationErrorMessage('audio-capture'), 'Nie znaleziono mikrofonu.');
  assert.match(toDictationErrorMessage('network') ?? '', /internetem/);
  assert.equal(toDictationErrorMessage('aborted'), null);
  assert.equal(toDictationErrorMessage('cos-nowego'), 'Nie udało się rozpoznać mowy. Spróbuj ponownie.');
});

test('przegladarka sama konczy sesje -> powrot do idle', () => {
  const { controller, createdRecognitions, states } = createControllerWithFakeRecognition();
  controller.start();
  createdRecognitions[0].onend?.();
  assert.deepEqual(states.at(-1), { status: 'idle' });
});

test('jedna sesja naraz: start w drugim polu zatrzymuje pierwsze', () => {
  const first = createControllerWithFakeRecognition();
  const second = createControllerWithFakeRecognition();
  first.controller.start();
  second.controller.start();
  assert.equal(first.createdRecognitions[0].isRunning, false);
  assert.deepEqual(first.states.at(-1), { status: 'idle' });
  assert.ok(second.createdRecognitions[0].isRunning);
  second.controller.stop();
});

test('cleanup (odmontowanie): mikrofon przerwany, zadnych dalszych zdarzen', () => {
  const { controller, createdRecognitions, states, transcripts } = createControllerWithFakeRecognition();
  controller.start();
  const recognition = createdRecognitions[0];
  const statesBeforeDispose = states.length;
  controller.dispose();
  assert.ok(recognition.wasAborted);
  recognition.say('po odmontowaniu');
  assert.deepEqual(transcripts, []);
  assert.equal(states.length, statesBeforeDispose);
});
