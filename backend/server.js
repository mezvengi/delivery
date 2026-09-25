require('dotenv').config();
const http = require('http');
const express = require('express');
const cors = require('cors');
const { initWebSocket } = require('./websocket');

const authRoutes = require('./routes/auth');
const shopRoutes = require('./routes/shops');
const productRoutes = require('./routes/products');
const orderRoutes = require('./routes/orders');
const driverRoutes = require('./routes/drivers');
const adminRoutes = require('./routes/admin');

const app = express();
const server = http.createServer(app);

// Initialize WebSocket server on the same HTTP server
const wss = initWebSocket(server);

// Middleware
app.use(cors());
app.use(express.json());

// Request logger for debugging
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Healthcheck & Local Geofence Info
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ONLINE',
    system: 'Sour El Ghozlane Delivery Engine',
    wilaya: 'Bouira (10)',
    zone_id: 'sour_el_ghozlane',
    coordinates: {
      lat: 36.1480,
      lon: 3.6900,
      radius_km: 8.0,
    },
    currency: 'DZD',
    fixed_delivery_fee: 200,
    timestamp: new Date().toISOString(),
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/shops', shopRoutes);
app.use('/api/products', productRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/drivers', driverRoutes);
app.use('/api/admin', adminRoutes);

// Error Handler
app.use((err, req, res, next) => {
  console.error('[Internal Server Error]:', err);
  res.status(500).json({ error: 'حدث خطأ داخلي في الخادم' });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`🚀 Sour El Ghozlane Delivery API & WebSocket started`);
  console.log(`📡 Listening on http://0.0.0.0:${PORT}`);
  console.log(`🛵 Geofence: Sour El Ghozlane (Lat: 36.1480, Lon: 3.6900)`);
  console.log(`💰 Fixed Delivery Fee: 200 DA (Cash on Delivery)`);
  console.log(`=======================================================`);
});
