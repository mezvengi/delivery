const { WebSocketServer } = require('ws');
const db = require('./db');

let wss = null;
const clients = new Set();

function initWebSocket(server) {
  wss = new WebSocketServer({ server, path: '/ws' });

  wss.on('connection', (ws) => {
    clients.add(ws);
    ws.subscriptions = new Set();

    ws.on('message', async (message) => {
      try {
        const data = JSON.parse(message.toString());
        await handleClientMessage(ws, data);
      } catch (err) {
        ws.send(JSON.stringify({ type: 'ERROR', message: 'تنسيق الرسالة غير صحيح' }));
      }
    });

    ws.on('close', () => {
      clients.delete(ws);
    });

    ws.send(JSON.stringify({
      type: 'CONNECTED',
      message: 'متصل بخادم التتبع المباشر لبلدية سور الغزلان 🛵',
      timestamp: Date.now(),
    }));
  });

  return wss;
}

async function handleClientMessage(ws, data) {
  switch (data.action) {
    case 'SUBSCRIBE_ORDER':
      if (data.orderId) {
        ws.subscriptions.add(`order_${data.orderId}`);
        ws.send(JSON.stringify({ type: 'SUBSCRIBED', channel: `order_${data.orderId}` }));
      }
      break;

    case 'SUBSCRIBE_DRIVERS':
      ws.subscriptions.add('drivers_active');
      ws.send(JSON.stringify({ type: 'SUBSCRIBED', channel: 'drivers_active' }));
      break;

    case 'UPDATE_DRIVER_LOCATION':
      // Driver publishes live GPS location
      if (data.driverId && data.lat && data.lon) {
        await db.query(
          `UPDATE driver_locations 
           SET lat = $1, lon = $2, speed = $3, heading = $4, is_online = $5, updated_at = NOW() 
           WHERE driver_id = $6`,
          [data.lat, data.lon, data.speed || 0, data.heading || 0, data.isOnline ?? true, data.driverId]
        );

        broadcastLocationUpdate({
          driverId: data.driverId,
          lat: data.lat,
          lon: data.lon,
          speed: data.speed || 0,
          heading: data.heading || 0,
          isOnline: data.isOnline ?? true,
          orderId: data.orderId || null,
        });
      }
      break;

    case 'PING':
      ws.send(JSON.stringify({ type: 'PONG', time: Date.now() }));
      break;

    default:
      break;
  }
}

function broadcastLocationUpdate(locationData) {
  const payload = JSON.stringify({
    type: 'DRIVER_LOCATION_UPDATE',
    data: locationData,
    timestamp: Date.now(),
  });

  for (const client of clients) {
    if (client.readyState === 1) { // OPEN
      if (
        client.subscriptions.has('drivers_active') ||
        (locationData.orderId && client.subscriptions.has(`order_${locationData.orderId}`))
      ) {
        client.send(payload);
      }
    }
  }
}

function broadcastOrderStatus(orderId, status, details = {}) {
  const payload = JSON.stringify({
    type: 'ORDER_STATUS_UPDATE',
    orderId,
    status,
    details,
    timestamp: Date.now(),
  });

  for (const client of clients) {
    if (client.readyState === 1) {
      if (client.subscriptions.has(`order_${orderId}`) || client.subscriptions.has('orders_all')) {
        client.send(payload);
      }
    }
  }
}

module.exports = {
  initWebSocket,
  broadcastLocationUpdate,
  broadcastOrderStatus,
};
