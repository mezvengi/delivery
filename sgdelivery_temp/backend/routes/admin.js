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

// Get All Accounts with Filters and Search
router.get('/users', async (req, res) => {
  try {
    const { role, status, search } = req.query;
    let query = `SELECT id, name, phone, role, is_active, created_at FROM users WHERE 1=1`;
    const params = [];

    if (role && role !== 'all') {
      params.push(role);
      query += ` AND role = $${params.length}`;
    }

    if (status === 'active') {
      query += ` AND is_active = TRUE`;
    } else if (status === 'suspended' || status === 'inactive') {
      query += ` AND is_active = FALSE`;
    }

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      query += ` AND (name ILIKE $${params.length} OR phone ILIKE $${params.length})`;
    }

    query += ` ORDER BY created_at DESC`;

    const result = await db.query(query, params);
    res.json({
      success: true,
      users: result.rows.map(u => ({
        id: u.id,
        name: u.name,
        phone: u.phone,
        role: u.role,
        is_active: u.is_active,
        status: u.is_active ? 'active' : 'suspended',
        created_at: u.created_at,
      }))
    });
  } catch (err) {
    console.error('Get all users error:', err);
    res.status(500).json({ error: 'تعذر جلب قائمة المستخدمين' });
  }
});

// Get Pending Accounts
router.get('/pending-users', async (req, res) => {
  try {
    const result = await db.query(
      `SELECT id, name, phone, role, is_active, created_at 
       FROM users 
       WHERE is_active = FALSE 
       ORDER BY created_at DESC`
    );
    res.json({
      success: true,
      users: result.rows.map(u => ({
        id: u.id,
        name: u.name,
        phone: u.phone,
        role: u.role,
        status: u.is_active ? 'active' : 'pending',
        created_at: u.created_at,
      })),
    });
  } catch (err) {
    console.error('Pending users error:', err);
    res.status(500).json({ error: 'تعذر جلب الحسابات المعلقة' });
  }
});

// Update Account Status (Approve / Suspend / Reactivate)
router.put('/users/:userId/status', async (req, res) => {
  const { userId } = req.params;
  const { status, is_active } = req.body;
  const activeFlag = is_active !== undefined ? Boolean(is_active) : (status === 'active');

  try {
    const checkUser = await db.query('SELECT role, phone FROM users WHERE id = $1', [userId]);
    if (checkUser.rows.length === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }
    if (checkUser.rows[0].phone === '0555000000' && !activeFlag) {
      return res.status(400).json({ error: 'لا يمكن تعليق حساب مدير النظام الرئيسي' });
    }

    const result = await db.query(
      `UPDATE users SET is_active = $1 WHERE id = $2 RETURNING id, name, phone, role, is_active, created_at`,
      [activeFlag, userId]
    );

    const u = result.rows[0];
    res.json({
      success: true,
      message: activeFlag ? 'تم تفعيل الحساب بنجاح ✅' : 'تم تعليق وتجميد الحساب بنجاح 🛑',
      user: {
        id: u.id,
        name: u.name,
        phone: u.phone,
        role: u.role,
        is_active: u.is_active,
        status: u.is_active ? 'active' : 'suspended',
      },
    });
  } catch (err) {
    console.error('Update user status error:', err);
    res.status(500).json({ error: 'تعذر تحديث حالة الحساب' });
  }
});

// Permanently Delete User Account
router.delete('/users/:userId', async (req, res) => {
  const { userId } = req.params;
  try {
    const checkUser = await db.query('SELECT name, phone, role FROM users WHERE id = $1', [userId]);
    if (checkUser.rows.length === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }
    const target = checkUser.rows[0];
    if (target.phone === '0555000000' || target.role === 'admin') {
      return res.status(400).json({ error: 'لا يمكن حذف حساب الإدارة الرئيسية' });
    }

    // Clean up dependent records safely
    await db.query(`DELETE FROM driver_locations WHERE driver_id = $1`, [userId]);
    await db.query(`UPDATE orders SET driver_id = NULL WHERE driver_id = $1`, [userId]);
    await db.query(`UPDATE orders SET customer_id = NULL WHERE customer_id = $1`, [userId]);
    await db.query(`UPDATE shops SET user_id = NULL WHERE user_id = $1`, [userId]);

    await db.query(`DELETE FROM users WHERE id = $1`, [userId]);

    res.json({
      success: true,
      message: `تم حذف حساب (${target.name}) نهائياً من قاعدة البيانات بنجاح 🗑️`
    });
  } catch (err) {
    console.error('Delete user error:', err);
    res.status(500).json({ error: 'تعذر حذف الحساب من قاعدة البيانات' });
  }
});

module.exports = router;
