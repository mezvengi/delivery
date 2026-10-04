const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware } = require('../auth');
const { broadcastLocationUpdate } = require('../websocket');

// Calculate distance between two points in km (Haversine formula)
function getDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

// Get active drivers in Sour El Ghozlane for customer modal
router.get('/active', async (req, res) => {
  const { userLat, userLon } = req.query;
  const refLat = parseFloat(userLat) || 36.1480;
  const refLon = parseFloat(userLon) || 3.6900;

  try {
    const result = await db.query(
      `SELECT dl.*, u.name, u.phone
       FROM driver_locations dl
       JOIN users u ON u.id = dl.driver_id
       WHERE dl.is_online = TRUE`
    );

    const driversWithDistance = result.rows.map((driver) => {
      const distance = getDistanceKm(refLat, refLon, parseFloat(driver.lat), parseFloat(driver.lon));
      return {
        ...driver,
        distance_km: distance,
        estimated_pickup_mins: Math.max(3, Math.round(distance * 3)),
      };
    });

    res.json(driversWithDistance);
  } catch (err) {
    console.error('Fetch active drivers error:', err);
    res.status(500).json({ error: 'تعذر جلب السائقين المتصلين' });
  }
});

// Update driver online status & vehicle
router.patch('/:id/status', authMiddleware(['driver', 'admin']), async (req, res) => {
  const { id } = req.params;
  const { is_online, vehicle_type, lat, lon } = req.body;

  try {
    const result = await db.query(
      `UPDATE driver_locations
       SET is_online = COALESCE($1, is_online),
           vehicle_type = COALESCE($2, vehicle_type),
           license_plate = COALESCE($3, license_plate),
           lat = COALESCE($4, lat),
           lon = COALESCE($5, lon),
           updated_at = NOW()
       WHERE driver_id = $6
       RETURNING *`,
      [is_online, vehicle_type, req.body.license_plate, lat, lon, id]
    );

    // Also update users table
    if (vehicle_type || req.body.license_plate || req.body.drivers_license) {
      await db.query(
        `UPDATE users
         SET vehicle_type = COALESCE($1, vehicle_type),
             license_plate = COALESCE($2, license_plate),
             drivers_license = COALESCE($3, drivers_license)
         WHERE id = $4`,
        [vehicle_type, req.body.license_plate, req.body.drivers_license, id]
      );
    }

    broadcastLocationUpdate({
      driverId: parseInt(id),
      lat: result.rows[0]?.lat,
      lon: result.rows[0]?.lon,
      isOnline: result.rows[0]?.is_online,
    });

    res.json(result.rows[0]);
  } catch (err) {
    console.error('Update driver status error:', err);
    res.status(500).json({ error: 'تعذر تحديث حالة السائق' });
  }
});

// Update GPS location (HTTP endpoint fallback for WebSocket)
router.post('/:id/location', authMiddleware(['driver', 'admin']), async (req, res) => {
  const { id } = req.params;
  const { lat, lon, speed, heading, orderId } = req.body;

  try {
    await db.query(
      `UPDATE driver_locations
       SET lat = $1, lon = $2, speed = $3, heading = $4, updated_at = NOW()
       WHERE driver_id = $5`,
      [lat, lon, speed || 0, heading || 0, id]
    );

    broadcastLocationUpdate({
      driverId: parseInt(id),
      lat,
      lon,
      speed: speed || 0,
      heading: heading || 0,
      orderId,
    });

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'تعذر تحديث الإحداثيات' });
  }
});

// Get driver statistics and vehicle profile (Deliverio feature)
router.get('/:id/stats', async (req, res) => {
  const { id } = req.params;
  try {
    const userRes = await db.query(
      `SELECT id, name, phone, vehicle_type, license_plate, drivers_license, completed_orders_count FROM users WHERE id = $1`,
      [id]
    );
    const activeOrdersRes = await db.query(
      `SELECT COUNT(*) FROM orders WHERE driver_id = $1 AND status IN ('READY_FOR_PICKUP', 'ON_THE_WAY')`,
      [id]
    );

    const user = userRes.rows[0] || {};
    const activeCount = parseInt(activeOrdersRes.rows[0]?.count || 0, 10);

    res.json({
      success: true,
      driver_id: parseInt(id),
      name: user.name,
      phone: user.phone,
      vehicle_type: user.vehicle_type || 'دراجة نارية SYM',
      license_plate: user.license_plate || '00123-116-10',
      drivers_license: user.drivers_license || 'DL-2024-DZ',
      completed_orders: user.completed_orders_count || 0,
      active_orders: activeCount,
      max_active_orders: 2,
    });
  } catch (err) {
    res.status(500).json({ error: 'تعذر جلب إحصائيات السائق' });
  }
});

module.exports = router;
