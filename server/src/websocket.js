const { WebSocketServer } = require('ws');
const jwt = require('jsonwebtoken');
const { pool } = require('./db');

let wss = null;
const clients = new Map(); // ws -> { userId, role, subscriptions: Set }

function initWebSocket(server) {
  wss = new WebSocketServer({ server, path: '/ws' });

  wss.on('connection', (ws) => {
    const clientState = {
      userId: null,
      role: null,
      subscriptions: new Set()
    };
    clients.set(ws, clientState);

    ws.send(JSON.stringify({
      type: 'CONNECTED',
      message: 'متصل بخادم التتبع المباشر لبلدية سور الغزلان 🛵',
      timestamp: Date.now()
    }));

    ws.on('message', async (message) => {
      try {
        const data = JSON.parse(message.toString());
        const action = data.action || data.type;

        switch (action) {
          case 'AUTH':
            if (data.token) {
              try {
                const decoded = jwt.verify(data.token, process.env.JWT_SECRET || 'sour-default-jwt-secret-key-change-me');
                clientState.userId = decoded.id;
                clientState.role = decoded.role;
                ws.send(JSON.stringify({ type: 'AUTH_OK', userId: decoded.id, role: decoded.role }));
              } catch (e) {
                ws.send(JSON.stringify({ type: 'AUTH_ERROR', message: 'رمز غير صالح' }));
              }
            }
            break;

          case 'SUBSCRIBE_ORDER':
            if (data.orderId) {
              clientState.subscriptions.add(`order_${data.orderId}`);
              ws.send(JSON.stringify({ type: 'SUBSCRIBED', channel: `order_${data.orderId}`, orderId: data.orderId }));
            }
            break;

          case 'SUBSCRIBE_DRIVERS':
            clientState.subscriptions.add('drivers_active');
            ws.send(JSON.stringify({ type: 'SUBSCRIBED', channel: 'drivers_active' }));
            break;

          case 'SUBSCRIBE_ORDERS':
            clientState.subscriptions.add('orders_all');
            ws.send(JSON.stringify({ type: 'SUBSCRIBED', channel: 'orders_all' }));
            break;

          case 'UPDATE_DRIVER_LOCATION':
            if (data.driverId && data.lat && (data.lng || data.lon)) {
              const lngVal = data.lng || data.lon;
              const speedVal = data.speed || 0;
              const headingVal = data.heading || 0;
              const isOnline = data.isOnline !== undefined ? data.isOnline : true;

              try {
                await pool.query(
                  `INSERT INTO driver_locations (driver_id, lat, lng, heading, is_online, updated_at)
                   VALUES ($1, $2, $3, $4, $5, NOW())
                   ON CONFLICT (driver_id) 
                   DO UPDATE SET lat = EXCLUDED.lat, lng = EXCLUDED.lng, heading = EXCLUDED.heading, is_online = EXCLUDED.is_online, updated_at = NOW()`,
                  [data.driverId, data.lat, lngVal, headingVal, isOnline]
                );
              } catch (dbErr) {
                console.error('[WS] Error updating driver location in DB:', dbErr.message);
              }

              broadcastDriverLocation(data.driverId, {
                driverId: data.driverId,
                lat: data.lat,
                lng: lngVal,
                lon: lngVal,
                speed: speedVal,
                heading: headingVal,
                isOnline,
                orderId: data.orderId || null
              });
            }
            break;

          case 'PING':
            ws.send(JSON.stringify({ type: 'PONG', timestamp: Date.now() }));
            break;

          default:
            break;
        }
      } catch (err) {
        console.error('[WS] Error processing message:', err.message);
      }
    });

    ws.on('close', () => {
      clients.delete(ws);
    });
  });

  return wss;
}

function broadcastOrderStatus(order) {
  if (!wss) return;

  const orderId = order.id || order.orderId;
  const status = order.status || 'NEW';

  const payload = JSON.stringify({
    type: 'ORDER_STATUS_UPDATE',
    action: 'ORDER_UPDATE',
    orderId,
    status,
    details: order,
    order,
    timestamp: Date.now()
  });

  for (const [ws, state] of clients.entries()) {
    if (ws.readyState === 1) { // OPEN
      if (
        !state.subscriptions ||
        state.subscriptions.size === 0 ||
        state.subscriptions.has(`order_${orderId}`) ||
        state.subscriptions.has('orders_all')
      ) {
        ws.send(payload);
      }
    }
  }
}

function broadcastDriverLocation(driverId, location) {
  if (!wss) return;

  const payload = JSON.stringify({
    type: 'DRIVER_LOCATION_UPDATE',
    action: 'DRIVER_LOCATION',
    driverId,
    data: location,
    location,
    timestamp: Date.now()
  });

  for (const [ws, state] of clients.entries()) {
    if (ws.readyState === 1) { // OPEN
      if (
        !state.subscriptions ||
        state.subscriptions.size === 0 ||
        state.subscriptions.has('drivers_active') ||
        (location.orderId && state.subscriptions.has(`order_${location.orderId}`))
      ) {
        ws.send(payload);
      }
    }
  }
}

module.exports = { initWebSocket, broadcastOrderStatus, broadcastDriverLocation };
