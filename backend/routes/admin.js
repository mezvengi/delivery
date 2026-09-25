const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware } = require('../auth');

// Admin Dashboard stats
router.get('/stats', authMiddleware(['admin']), async (req, res) => {
  try {
    const totalOrdersRes = await db.query(`SELECT COUNT(*) as count FROM orders`);
    const todayOrdersRes = await db.query(
      `SELECT COUNT(*) as count, COALESCE(SUM(total), 0) as revenue
       FROM orders 
       WHERE created_at >= CURRENT_DATE`
    );
    const activeDriversRes = await db.query(`SELECT COUNT(*) as count FROM driver_locations WHERE is_online = TRUE`);
    const totalShopsRes = await db.query(`SELECT COUNT(*) as count FROM shops WHERE is_open = TRUE`);

    const recentOrdersRes = await db.query(
      `SELECT o.*, s.name as shop_name, u.name as driver_name
       FROM orders o
       JOIN shops s ON s.id = o.shop_id
       LEFT JOIN users u ON u.id = o.driver_id
       ORDER BY o.created_at DESC
       LIMIT 10`
    );

    res.json({
      totalOrders: parseInt(totalOrdersRes.rows[0].count),
      todayOrders: parseInt(todayOrdersRes.rows[0].count),
      todayRevenue: parseInt(todayOrdersRes.rows[0].revenue),
      activeDrivers: parseInt(activeDriversRes.rows[0].count),
      activeShops: parseInt(totalShopsRes.rows[0].count),
      geofence: {
        centerLat: 36.1480,
        centerLon: 3.6900,
        radiusKm: 8.0,
        zoneId: 'sour_el_ghozlane',
      },
      recentOrders: recentOrdersRes.rows,
    });
  } catch (err) {
    console.error('Admin stats error:', err);
    res.status(500).json({ error: 'تعذر جلب إحصائيات النظام' });
  }
});

// Manual driver assignment
router.post('/assign-driver', authMiddleware(['admin']), async (req, res) => {
  const { order_id, driver_id } = req.body;
  try {
    const result = await db.query(
      `UPDATE orders SET driver_id = $1, status = 'ON_THE_WAY', updated_at = NOW() WHERE id = $2 RETURNING *`,
      [driver_id, order_id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'تعذر تعيين السائق للطلب' });
  }
});

module.exports = router;
