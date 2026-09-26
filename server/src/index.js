require('dotenv').config();
const http = require('http');
const express = require('express');
const cors = require('cors');
const { initDB } = require('./db');
const { initWebSocket } = require('./websocket');

const authRoutes = require('./routes/authRoutes');
const adminRoutes = require('./routes/adminRoutes');
const zoneRoutes = require('./routes/zoneRoutes');
const shopRoutes = require('./routes/shopRoutes');
const orderRoutes = require('./routes/orderRoutes');
const driverRoutes = require('./routes/driverRoutes');

const app = express();
const server = http.createServer(app);

// Middlewares
app.use(cors());
app.use(express.json());
const path = require('path');
app.use('/app', express.static(path.join(__dirname, '../public')));

// Request logger
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'sour-delivery-api',
    zone: process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane',
    timestamp: new Date().toISOString()
  });
});

// Root landing page redirects to /app
app.get('/', (req, res) => {
  res.redirect('/app');
});

// Admin browser URL direct access redirects to frontend admin panel
app.get('/admin', (req, res, next) => {
  if (req.headers.accept && req.headers.accept.includes('text/html')) {
    return res.redirect('/app/?role=admin');
  }
  next();
});

// Mount API routes
app.use('/api/auth', authRoutes);
app.use('/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/admin', adminRoutes);
app.use('/api/zones', zoneRoutes);
app.use('/api/shops', shopRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/drivers', driverRoutes);

// 404 Handler
app.use((req, res) => {
  res.status(404).json({ error: 'المسار غير موجود' });
});

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('[Unhandled Error]', err);
  res.status(500).json({ error: 'حدث خطأ داخلي في الخادم' });
});

// Initialize WebSocket
initWebSocket(server);

// Start Server after DB Init
const PORT = process.env.PORT || 3000;

async function bootstrap() {
  try {
    await initDB();
    server.listen(PORT, () => {
      console.log(`🚀 [SOUR-DELIVERY] Server running on port ${PORT}`);
      console.log(`📡 WebSocket ready on ws://localhost:${PORT}/ws`);
    });
  } catch (err) {
    console.error('Failed to initialize database and start server:', err);
    process.exit(1);
  }
}

bootstrap();
