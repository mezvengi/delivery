require('dotenv').config();
const http = require('http');
const express = require('express');
const cors = require('cors');
const { initDB } = require('./db');
const { initWebSocket } = require('./websocket');

const authRoutes = require('./routes/authRoutes');
const zoneRoutes = require('./routes/zoneRoutes');
const shopRoutes = require('./routes/shopRoutes');
const orderRoutes = require('./routes/orderRoutes');
const driverRoutes = require('./routes/driverRoutes');

const app = express();
const server = http.createServer(app);

// Middlewares
app.use(cors());
app.use(express.json());

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

// Root landing page
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
      <meta charset="UTF-8">
      <title>تطبيق توصيل سور الغزلان</title>
      <style>
        body { font-family: system-ui, sans-serif; background: #0f172a; color: #f8fafc; padding: 40px; text-align: center; }
        .card { max-width: 600px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
        h1 { color: #38bdf8; }
        .badge { display: inline-block; background: #10b981; color: white; padding: 6px 12px; border-radius: 20px; font-weight: bold; margin: 10px 0; }
        .info { text-align: right; margin-top: 20px; line-height: 1.8; color: #94a3b8; }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>منصة توصيل سور الغزلان</h1>
        <div class="badge">الخادم يعمل بنجاح (API Online)</div>
        <div class="info">
          <p>📍 <strong>النطاق الجغرافي:</strong> بلدية سور الغزلان (36.148° N, 3.690° E)</p>
          <p>💵 <strong>العملة:</strong> الدينار الجزائري (DA) - الدفع عند الاستلام (COD)</p>
          <p>🚚 <strong>سعر التوصيل الثابت:</strong> ${process.env.FIXED_DELIVERY_FEE || 200} دج</p>
          <p>🔗 <strong>مسار الـ API:</strong> <code>/api</code></p>
          <p>⚡ <strong>الاتصال اللحظي (WebSocket):</strong> <code>/ws</code></p>
        </div>
      </div>
    </body>
    </html>
  `);
});

// Mount API routes
app.use('/api/auth', authRoutes);
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
