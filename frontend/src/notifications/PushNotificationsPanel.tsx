import { useEffect, useState } from 'react';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { isThisDeviceSubscribed, pushAvailability, subscribeThisDevice, unsubscribeThisDevice } from './browserPush';
import { notificationsApi } from './notificationsApi';
import { pushAvailabilityMessage } from './pushSupport';

type PanelState = 'checking' | 'subscribed' | 'unsubscribed';

/**
 * Przelacznik "Otrzymuj powiadomienia push" NA TYM URZADZENIU (etapy 29-30) + powiadomienie testowe. Powiadomienia
 * i tak trafiaja na liste w aplikacji. E-mail/SMS - jeszcze nie (do uzgodnienia), pokazane jako niedostepne.
 */
export function PushNotificationsPanel() {
  const availability = pushAvailability();
  const [panelState, setPanelState] = useState<PanelState>('checking');
  const [isWorking, setIsWorking] = useState(false);
  const [message, setMessage] = useState<{ variant: 'success' | 'error' | 'info'; text: string } | null>(null);

  useEffect(() => {
    let isResultStillWanted = true;
    isThisDeviceSubscribed()
      .then((subscribed) => { if (isResultStillWanted) setPanelState(subscribed ? 'subscribed' : 'unsubscribed'); })
      .catch(() => { if (isResultStillWanted) setPanelState('unsubscribed'); });
    return () => { isResultStillWanted = false; };
  }, []);

  const run = async (action: () => Promise<void>) => {
    setIsWorking(true);
    setMessage(null);
    try {
      await action();
    } catch (actionError) {
      setMessage({ variant: 'error', text: `Nie udało się zmienić ustawień powiadomień. ${
        actionError instanceof ApiRequestError ? actionError.message : 'Sprawdź połączenie i spróbuj ponownie.'}` });
    } finally {
      setIsWorking(false);
    }
  };

  const toggle = () => run(async () => {
    if (panelState === 'subscribed') {
      await unsubscribeThisDevice();
      setPanelState('unsubscribed');
      setMessage({ variant: 'info', text: 'Powiadomienia push na tym urządzeniu zostały wyłączone.' });
      return;
    }
    if (await subscribeThisDevice() === 'PERMISSION_DENIED') {
      setMessage({ variant: 'error', text: 'Nie udzielono zgody na powiadomienia. Zgodę można zmienić w ustawieniach przeglądarki.' });
      return;
    }
    setPanelState('subscribed');
    setMessage({ variant: 'success', text: 'Powiadomienia push na tym urządzeniu są włączone.' });
  });

  const sendTest = () => run(async () => {
    const { deliveredDeviceCount } = await notificationsApi.sendTestNotification();
    setMessage(deliveredDeviceCount > 0
      ? { variant: 'success', text: `Wysłano powiadomienie testowe (urządzenia: ${deliveredDeviceCount}).` }
      : { variant: 'info', text: 'Powiadomienie testowe zapisano na liście, ale nie dotarło do żadnego urządzenia.' });
  });

  const unavailableMessage = pushAvailabilityMessage(availability);

  return (
    <div className="stack-tight push-panel">
      {unavailableMessage ? <Alert variant="info">{unavailableMessage}</Alert> : (
        <label className="checkbox-row">
          <input type="checkbox" checked={panelState === 'subscribed'} disabled={isWorking || panelState === 'checking'}
                 onChange={() => void toggle()} />
          <span>Otrzymuj powiadomienia push na tym urządzeniu</span>
        </label>
      )}
      <p className="muted small">
        Powiadomienia: ocena stanu drzewa „Zły” przy kontroli oraz codzienne przypomnienie (rano) o pracach zaplanowanych na jutro.
        Wszystkie trafiają też na listę powiadomień w aplikacji.
      </p>
      <label className="checkbox-row muted">
        <input type="checkbox" disabled />
        <span>Powiadomienia e-mail (wkrótce - do uzgodnienia)</span>
      </label>
      {panelState === 'subscribed' && (
        <div className="form-actions">
          <button type="button" className="btn btn-secondary btn-small" onClick={() => void sendTest()} disabled={isWorking}>
            {isWorking ? 'Wysyłanie…' : 'Wyślij powiadomienie testowe'}
          </button>
        </div>
      )}
      {message && <Alert variant={message.variant}>{message.text}</Alert>}
    </div>
  );
}
