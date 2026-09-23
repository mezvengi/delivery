const { WebSocketServer } = require('ws');
const jwt = require('jsonwebtoken');

let wss = null;
const clients = new Map(); // ws -> { userId, role, orderId }

function initWebSocket(server) {
  wss = new WebSocketServer({ server, path: '/ws' });

  wss.on('connection', (ws, req) => {
    console.log('[WS] New client connected');

    ws.on('message', (message) => {
      try {
        const data = JSON.parse(message.toString());

        // Authenticate client
        if (data.type === 'AUTH') {
          const token = data.token;
          try {
            const decoded = jwt.verify(token, process.env.JWT_SECRET || 'sour-default-jwt-secret-key-change-me');
            clients.set(ws, {
              userId: decoded.id,
              role: decoded.role,
              subscribedOrder: null
            });
            ws.send(JSON.stringify({ type: 'AUTH_OK', userId: decoded.id, role: decoded.role }));
          } catch (e) {
            ws.send(JSON.stringify({ type: 'AUTH_ERROR', message: 'رمز غير صالح' }));
          }
        }

        // Subscribe to a specific order tracking
        if (data.type === 'SUBSCRIBE_ORDER') {
          const clientData = clients.get(ws) || {};
          clientData.subscribedOrder = data.orderId;
          clients.set(ws, clientData);
          ws.send(JSON.stringify({ type: 'SUBSCRIBED', orderId: data.orderId }));
        }
      } catch (err) {
        console.error('[WS] Error processing message:', err.message);
      }
    });

    ws.on('close', () => {
      clients.delete(ws);
      console.log('[WS] Client disconnected');
    });
  });

  return wss;
}

function broadcastOrderStatus(order) {
  if (!wss) return;
  const payload = JSON.stringify({
    type: 'ORDER_UPDATE',
    order
  });

  for (const [ws, info] of clients.entries()) {
    if (ws.readyState === ws.OPEN) {
      // Notify admin, or the customer who owns the order, or driver assigned, or anyone watching orderId
      if (
        info.role === 'ADMIN' ||
        info.userId === order.customer_id ||
        info.userId === order.driver_id ||
        info.subscribedOrder == order.id
      ) {
        ws.send(payload);
      }
    }
  }
}

function broadcastDriverLocation(driverId, location) {
  if (!wss) return;
  const payload = JSON.stringify({
    type: 'DRIVER_LOCATION',
    driverId,
    location
  });

  for (const [ws, info] of clients.entries()) {
    if (ws.readyState === ws.OPEN) {
      // Admin sees all drivers, or customer whose active order is assigned to driver
      if (info.role === 'ADMIN' || info.subscribedDriver === driverId) {
        ws.send(payload);
      }
    }
  }
}

module.exports = { initWebSocket, broadcastOrderStatus, broadcastDriverLocation };
