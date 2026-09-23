const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { pool } = require('../db');
const { normalizePhone, sendOTP, verifyOTP } = require('../otp');
const { generateToken, authMiddleware } = require('../auth');

// Request OTP
router.post('/send-otp', async (req, res) => {
  const { phone } = req.body;
  if (!phone) {
    return res.status(400).json({ error: 'يرجى إدخال رقم الهاتف' });
  }

  try {
    const result = await sendOTP(phone);
    res.json({
      success: true,
      message: 'تم إرسال كود التحقق بنجاح',
      phone: result.phone,
      mock_code: result.mockCode // for development & tests
    });
  } catch (err) {
    console.error('Error sending OTP:', err);
    res.status(500).json({ error: 'حدث خطأ أثناء إرسال كود التحقق' });
  }
});

// Verify OTP & Login or Register
router.post('/verify-otp', async (req, res) => {
  const { phone, code, full_name, role } = req.body;
  if (!phone || !code) {
    return res.status(400).json({ error: 'رقم الهاتف وكود التحقق مطلوبان' });
  }

  try {
    const valid = await verifyOTP(phone, code);
    if (!valid) {
      return res.status(400).json({ error: 'كود التحقق غير صحيح أو انتهت صلاحيته' });
    }

    const normalized = normalizePhone(phone);
    let userRes = await pool.query('SELECT * FROM users WHERE phone = $1', [normalized]);

    let user;
    if (userRes.rowCount === 0) {
      // Register new user
      const assignedRole = ['CUSTOMER', 'DRIVER', 'SHOP'].includes(role) ? role : 'CUSTOMER';
      const insertRes = await pool.query(
        `INSERT INTO users (phone, full_name, role, zone_id)
         VALUES ($1, $2, $3, $4)
         RETURNING *`,
        [normalized, full_name || 'مستخدم جديد', assignedRole, process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane']
      );
      user = insertRes.rows[0];
    } else {
      user = userRes.rows[0];
    }

    const token = generateToken(user);
    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        zone_id: user.zone_id
      }
    });
  } catch (err) {
    console.error('Error verifying OTP:', err);
    res.status(500).json({ error: 'خطأ في معالجة كود التحقق' });
  }
});

// Login with password (Admin, Shop, or Drivers)
router.post('/login-password', async (req, res) => {
  const { phone, password } = req.body;
  if (!phone || !password) {
    return res.status(400).json({ error: 'يرجى إدخال الهاتف وكلمة المرور' });
  }

  try {
    const normalized = normalizePhone(phone);
    const userRes = await pool.query('SELECT * FROM users WHERE phone = $1', [normalized]);
    if (userRes.rowCount === 0) {
      return res.status(401).json({ error: 'بيانات الدخول غير صحيحة' });
    }

    const user = userRes.rows[0];
    if (!user.password_hash) {
      return res.status(400).json({ error: 'هذا الحساب يستخدم رمز التحقق فقط لتسجيل الدخول' });
    }

    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) {
      return res.status(401).json({ error: 'بيانات الدخول غير صحيحة' });
    }

    const token = generateToken(user);
    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        zone_id: user.zone_id
      }
    });
  } catch (err) {
    console.error('Error in login-password:', err);
    res.status(500).json({ error: 'خطأ في تسجيل الدخول' });
  }
});

// Current user profile
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const userRes = await pool.query('SELECT id, phone, full_name, role, zone_id, created_at FROM users WHERE id = $1', [req.user.id]);
    if (userRes.rowCount === 0) return res.status(404).json({ error: 'المستخدم غير موجود' });
    res.json({ user: userRes.rows[0] });
  } catch (err) {
    res.status(500).json({ error: 'خطأ في جلب بيانات المستخدم' });
  }
});

module.exports = router;
