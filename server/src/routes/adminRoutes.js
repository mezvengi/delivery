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
 * PUT /admin/users/:id/status
 * Activate or suspend a user account (status: 'active' | 'suspended')
 */
router.put('/users/:id/status', async (req, res) => {
  const userId = parseInt(req.params.id, 10);
  const { status } = req.body;

  if (isNaN(userId)) {
    return res.status(400).json({ error: 'معرّف المستخدم غير صالح' });
  }

  const allowedStatuses = ['active', 'suspended', 'pending'];
  if (!status || !allowedStatuses.includes(status.toLowerCase())) {
    return res.status(400).json({
      error: `الحالة غير مقبولة. الحالات المتاحة: ${allowedStatuses.join(', ')}`
    });
  }

  const targetStatus = status.toLowerCase();

  try {
    // Check if user exists
    const userCheck = await pool.query('SELECT id, phone, full_name, role, status FROM users WHERE id = $1', [userId]);
    if (userCheck.rowCount === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }

    const currentUser = userCheck.rows[0];

    // Prevent suspending the master admin from themselves accidentally
    if (currentUser.role === 'admin' && targetStatus === 'suspended') {
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
    if (updatedUser.role === 'store') {
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

    res.json({
      success: true,
      message: `تم تحديث حالة الحساب بنجاح إلى (${targetStatus})`,
      user: updateRes.rows[0]
    });
  } catch (err) {
    console.error('Error updating user status:', err);
    res.status(500).json({ error: 'حدث خطأ أثناء تحديث حالة الحساب' });
  }
});

module.exports = router;
