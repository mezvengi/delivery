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
           lat = COALESCE($3, lat),
           lon = COALESCE($4, lon),
           updated_at = NOW()
       WHERE driver_id = $5
       RETURNING *`,
      [is_online, vehicle_type, lat, lon, id]
    );

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

module.exports = router;
