const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, requireRole } = require('../auth');
const { broadcastDriverLocation } = require('../websocket');

// Driver updates location
router.post('/location', authMiddleware, requireRole('DRIVER'), async (req, res) => {
  const { lat, lng, heading } = req.body;
  if (lat === undefined || lng === undefined) {
    return res.status(400).json({ error: 'الإحداثيات مطلوبة' });
  }

  try {
    await pool.query(
      `INSERT INTO driver_locations (driver_id, lat, lng, heading, is_online, updated_at)
       VALUES ($1, $2, $3, $4, TRUE, NOW())
       ON CONFLICT (driver_id) DO UPDATE
       SET lat = EXCLUDED.lat, lng = EXCLUDED.lng, heading = EXCLUDED.heading, is_online = TRUE, updated_at = NOW()`,
      [req.user.id, lat, lng, heading || 0]
    );

    broadcastDriverLocation(req.user.id, { lat, lng, heading });

    res.json({ success: true });
  } catch (err) {
    console.error('Error updating driver location:', err);
    res.status(500).json({ error: 'خطأ في تحديث الموقع' });
  }
});

// List available online drivers on map (for Customers & Admin)
router.get('/available', authMiddleware, async (req, res) => {
  const zoneId = req.query.zone_id || process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';
  try {
    const result = await pool.query(`
      SELECT u.id, u.full_name, u.phone, u.zone_id,
             dl.lat, dl.lng, dl.heading, dl.updated_at as last_seen
      FROM users u
      JOIN driver_locations dl ON u.id = dl.driver_id
      WHERE u.role = 'DRIVER' AND dl.is_online = TRUE AND u.zone_id = $1
      ORDER BY dl.updated_at DESC
    `, [zoneId]);
    res.json({ drivers: result.rows });
  } catch (err) {
    console.error('Error fetching available drivers:', err);
    res.status(500).json({ error: 'خطأ في جلب السائقين المتوفرين' });
  }
});

// List all drivers (Admin only)
router.get('/', authMiddleware, requireRole('ADMIN'), async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT u.id, u.full_name, u.phone, u.zone_id,
             dl.lat, dl.lng, dl.heading, dl.is_online, dl.updated_at as last_location_update
      FROM users u
      LEFT JOIN driver_locations dl ON u.id = dl.driver_id
      WHERE u.role = 'DRIVER'
      ORDER BY u.id ASC
    `);
    res.json({ drivers: result.rows });
  } catch (err) {
    console.error('Error fetching drivers:', err);
    res.status(500).json({ error: 'خطأ في جلب بيانات السائقين' });
  }
});

module.exports = router;
