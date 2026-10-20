/* Service worker Web Push de Tnc (TModeler) : affiche les push FCM reçus quand l'onglet n'est pas visible.
 * À servir à la racine du site (ex. public/tnc-sw.js). La configuration Firebase arrive par la query string
 * que Tnc ajoute à l'enregistrement (apiKey, projectId, senderId, appId, sdk). */
const params = new URL(self.location.href).searchParams;
const sdk = params.get('sdk') || '10.12.2';

importScripts('https://www.gstatic.com/firebasejs/' + sdk + '/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/' + sdk + '/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: params.get('apiKey'),
  projectId: params.get('projectId'),
  messagingSenderId: params.get('senderId'),
  appId: params.get('appId'),
});

const messaging = firebase.messaging();

// Message « data » seul : on construit la notification nous-mêmes.
messaging.onBackgroundMessage((payload) => {
  const data = (payload && payload.data) || {};
  const title = data.title || 'Nouveau message';
  return self.registration.showNotification(title, {
    body: data.body || '',
    tag: data.tnotif || data.messageId || undefined,
    data,
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const client of list) {
        if ('focus' in client) {
          client.postMessage({ tncOpen: data });
          return client.focus();
        }
      }
      const q = data.messagingId ? '?messagingId=' + encodeURIComponent(data.messagingId) : '';
      return self.clients.openWindow('/' + q);
    }),
  );
});
