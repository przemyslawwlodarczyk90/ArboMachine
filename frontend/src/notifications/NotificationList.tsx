import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { formatTimestampForDisplay } from '../formatting/formatTimestampForDisplay';
import { notificationsApi, type UserNotification } from './notificationsApi';
import { NOTIFICATION_TYPE_LABELS } from './pushSupport';

type ListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; notifications: UserNotification[] };

/** Ostatnie powiadomienia zalogowanego konta (V1: bez stanu "przeczytane"). */
export function NotificationList() {
  const [listState, setListState] = useState<ListState>({ status: 'loading' });

  useEffect(() => {
    let isResponseStillWanted = true;
    notificationsApi.listNotifications()
      .then((notifications) => { if (isResponseStillWanted) setListState({ status: 'loaded', notifications }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setListState({ status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  if (listState.status === 'loading') return <div className="center"><LoadingSpinner /></div>;
  if (listState.status === 'failed') return <Alert variant="error">Nie udało się wczytać powiadomień. {listState.errorMessage}</Alert>;
  if (listState.notifications.length === 0) return <p className="muted">Brak powiadomień.</p>;

  return (
    <ul className="notification-list">
      {listState.notifications.map((notification) => (
        <li key={notification.id} className="notification-item">
          <div className="notification-item-header">
            <span className="badge">{NOTIFICATION_TYPE_LABELS[notification.type] ?? notification.type}</span>
            <span className="muted small">{formatTimestampForDisplay(notification.createdAt)}</span>
          </div>
          <strong>{notification.linkPath ? <Link to={notification.linkPath}>{notification.title}</Link> : notification.title}</strong>
          <span className="small">{notification.body}</span>
        </li>
      ))}
    </ul>
  );
}
