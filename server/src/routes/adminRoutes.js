const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, requireRole, revokeAllUserTokens } = require('../auth');

// All routes in this router require authentication and ADMIN role
router.use(authMiddleware);
router.use(requireRole('admin'));

/**
 * GET /admin/pending-users
 * Returns pending driver and store accounts awaiting approval
 */
router.get('/pending-users', async (req, res) => {
  try {
    const query = `
      SELECT 
        u.id,
        u.phone,
        u.full_name,
        u.role,
        u.status,
        u.zone_id,
        u.created_at,
        d.vehicle_type,
        d.license_plate,
        s.name AS store_name,
        s.category AS store_category,
        s.address AS store_address
      FROM users u
      LEFT JOIN drivers d ON u.id = d.user_id AND u.role = 'driver'
      LEFT JOIN stores s ON u.id = s.user_id AND u.role = 'store'
      WHERE u.status = 'pending'
      ORDER BY u.created_at DESC
    `;

    const result = await pool.query(query);

    const pendingUsers = result.rows.map(row => {
      const userObj = {
        id: row.id,
        phone: row.phone,
        full_name: row.full_name,
        role: row.role,
        status: row.status,
        zone_id: row.zone_id,
        created_at: row.created_at
      };

      if (row.role === 'driver') {
        userObj.profile = {
          vehicle_type: row.vehicle_type,
          license_plate: row.license_plate
        };
      } else if (row.role === 'store') {
        userObj.profile = {
          name: row.store_name,
          category: row.store_category,
          address: row.store_address
        };
      }

      return userObj;
    });

    res.json({
      success: true,
      count: pendingUsers.length,
      users: pendingUsers
    });
  } catch (err) {
    console.error('Error fetching pending users:', err);
    res.status(500).json({ error: 'خطأ أثناء جلب قائمة الحسابات المعلقة' });
  }
});

/**
 * GET /admin/users
 * Returns all user accounts with search, role, and status filters
 */
router.get('/users', async (req, res) => {
  try {
    const { role, status, search } = req.query;
    let query = `
      SELECT 
        u.id, 
        u.phone, 
        u.full_name, 
        u.role, 
        u.status, 
        u.zone_id, 
        u.created_at,
        d.vehicle_type,
        d.license_plate,
        s.name AS store_name,
        s.category AS store_category,
        s.address AS store_address
      FROM users u
      LEFT JOIN drivers d ON u.id = d.user_id AND u.role = 'driver'
      LEFT JOIN stores s ON u.id = s.user_id AND (u.role = 'store' OR u.role = 'shop')
      WHERE 1=1
    `;
    const params = [];

    if (role && role !== 'all') {
      const normRole = role.toLowerCase() === 'shop' ? 'store' : role.toLowerCase();
      params.push(normRole);
      query += ` AND (LOWER(u.role) = $${params.length}${normRole === 'store' ? ` OR LOWER(u.role) = 'shop'` : ''})`;
    }

    if (status) {
      if (status === 'active') {
        params.push('active');
        query += ` AND u.status = $${params.length}`;
      } else if (status === 'suspended' || status === 'inactive') {
        params.push('suspended');
        query += ` AND u.status = $${params.length}`;
      } else if (status === 'pending') {
        params.push('pending');
        query += ` AND u.status = $${params.length}`;
      }
    }

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      query += ` AND (u.full_name ILIKE $${params.length} OR u.phone ILIKE $${params.length})`;
    }

    query += ` ORDER BY u.created_at DESC`;

    const result = await pool.query(query, params);
    res.json({
      success: true,
      count: result.rowCount,
      users: result.rows.map(u => ({
        id: u.id,
        name: u.full_name,
        full_name: u.full_name,
        phone: u.phone,
        role: u.role,
        is_active: u.status === 'active',
        status: u.status || 'active',
        created_at: u.created_at,
        profile: u.role === 'driver' ? {
          vehicle_type: u.vehicle_type,
          license_plate: u.license_plate
        } : (u.role === 'store' || u.role === 'shop') ? {
          name: u.store_name,
          category: u.store_category,
          address: u.store_address
        } : null
      }))
    });
  } catch (err) {
    console.error('Error fetching all users:', err);
    res.status(500).json({ error: 'خطأ أثناء جلب قائمة المستخدمين' });
  }
});

/**
 * PUT /admin/users/:id/status
 * Activate or suspend a user account (status: 'active' | 'suspended' | 'pending' or is_active: boolean)
 */
router.put('/users/:id/status', async (req, res) => {
  const userId = parseInt(req.params.id, 10);
  const { status, is_active } = req.body;

  if (isNaN(userId)) {
    return res.status(400).json({ error: 'معرّف المستخدم غير صالح' });
  }

  let targetStatus = null;
  if (is_active !== undefined) {
    targetStatus = is_active ? 'active' : 'suspended';
  } else if (status) {
    targetStatus = status.toLowerCase();
  }

  const allowedStatuses = ['active', 'suspended', 'pending'];
  if (!targetStatus || !allowedStatuses.includes(targetStatus)) {
    return res.status(400).json({
      error: `الحالة غير مقبولة. الحالات المتاحة: ${allowedStatuses.join(', ')}`
    });
  }

  try {
    // Check if user exists
    const userCheck = await pool.query('SELECT id, phone, full_name, role, status FROM users WHERE id = $1', [userId]);
    if (userCheck.rowCount === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }

    const currentUser = userCheck.rows[0];

    // Prevent suspending the master admin
    if ((currentUser.role === 'admin' || currentUser.phone.includes('555000000')) && targetStatus === 'suspended') {
      return res.status(403).json({ error: 'لا يمكن تعليق حساب مدير النظام' });
    }

    // Update status
    const updateRes = await pool.query(
      `UPDATE users 
       SET status = $1, updated_at = NOW() 
       WHERE id = $2 
       RETURNING id, phone, full_name, role, status, updated_at`,
      [targetStatus, userId]
    );

    const updatedUser = updateRes.rows[0];

    // Sync status to store / driver profiles
    if (updatedUser.role === 'store' || updatedUser.role === 'shop') {
      const isActive = targetStatus === 'active';
      await pool.query('UPDATE stores SET is_active = $1 WHERE user_id = $2', [isActive, userId]);
      await pool.query('UPDATE shops SET is_active = $1 WHERE user_id = $2', [isActive, userId]);
    } else if (updatedUser.role === 'driver') {
      const isAvail = targetStatus === 'active';
      await pool.query('UPDATE drivers SET is_available = $1 WHERE user_id = $2', [isAvail, userId]);
    }

    // If account was suspended, immediately revoke all active sessions/tokens
    if (targetStatus === 'suspended') {
      await revokeAllUserTokens(userId);
    }

    const isAct = targetStatus === 'active';
    res.json({
      success: true,
      message: isAct ? 'تم تفعيل الحساب بنجاح ✅' : 'تم تعليق وتجميد الحساب بنجاح 🛑',
      user: {
        id: updatedUser.id,
        name: updatedUser.full_name,
        full_name: updatedUser.full_name,
        phone: updatedUser.phone,
        role: updatedUser.role,
        is_active: isAct,
        status: updatedUser.status
      }
    });
  } catch (err) {
    console.error('Error updating user status:', err);
    res.status(500).json({ error: 'حدث خطأ أثناء تحديث حالة الحساب' });
  }
});

/**
 * DELETE /admin/users/:id
 * Permanently delete a user account and associated profile records
 */
router.delete('/users/:id', async (req, res) => {
  const userId = parseInt(req.params.id, 10);
  if (isNaN(userId)) {
    return res.status(400).json({ error: 'معرّف المستخدم غير صالح' });
  }

  try {
    const checkUser = await pool.query('SELECT id, full_name, phone, role FROM users WHERE id = $1', [userId]);
    if (checkUser.rowCount === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }
    const target = checkUser.rows[0];
    if (target.phone.includes('555000000') || target.role === 'admin') {
      return res.status(400).json({ error: 'لا يمكن حذف حساب الإدارة الرئيسية' });
    }

    // Clean up dependent records safely
    await pool.query('DELETE FROM driver_locations WHERE driver_id = $1', [userId]);
    await pool.query('UPDATE orders SET driver_id = NULL WHERE driver_id = $1', [userId]);
    await pool.query('UPDATE orders SET customer_id = NULL WHERE customer_id = $1', [userId]);
    await pool.query('DELETE FROM stores WHERE user_id = $1', [userId]);
    await pool.query('DELETE FROM shops WHERE user_id = $1', [userId]);
    await pool.query('DELETE FROM drivers WHERE user_id = $1', [userId]);
    await pool.query('DELETE FROM customers WHERE user_id = $1', [userId]);
    await pool.query('DELETE FROM refresh_tokens WHERE user_id = $1', [userId]);

    await pool.query('DELETE FROM users WHERE id = $1', [userId]);

    res.json({
      success: true,
      message: `تم حذف حساب (${target.full_name || target.phone}) نهائياً من قاعدة البيانات بنجاح 🗑️`
    });
  } catch (err) {
    console.error('Delete user error:', err);
    res.status(500).json({ error: 'تعذر حذف الحساب من قاعدة البيانات' });
  }
});

module.exports = router;
