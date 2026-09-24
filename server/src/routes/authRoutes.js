const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { pool } = require('../db');
const {
  validateAlgerianPhone,
  generateTokenPair,
  rotateRefreshToken,
  revokeRefreshToken,
  authMiddleware
} = require('../auth');
const { createRateLimiter, resetAttempts } = require('../rateLimiter');
const { normalizePhone, sendOTP, verifyOTP } = require('../otp');

// Rate limiter for login: max 5 failed attempts per 15 minutes
const loginLimiter = createRateLimiter({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: 'تم تجاوز الحد الأقصى لمحاولات تسجيل الدخول. يرجى الانتظار 15 دقيقة قبل المحاولة مرة أخرى.'
});

/**
 * Helper to fetch role-specific profile data
 */
async function getUserProfile(userId, role) {
  const normRole = (role || '').toLowerCase();
  if (normRole === 'customer') {
    const res = await pool.query('SELECT address, created_at FROM customers WHERE user_id = $1', [userId]);
    return res.rows[0] || null;
  } else if (normRole === 'driver') {
    const res = await pool.query('SELECT vehicle_type, license_plate, is_available, created_at FROM drivers WHERE user_id = $1', [userId]);
    return res.rows[0] || null;
  } else if (normRole === 'store' || normRole === 'shop') {
    const res = await pool.query('SELECT name, category, address, phone, is_active, created_at FROM stores WHERE user_id = $1', [userId]);
    return res.rows[0] || null;
  }
  return null;
}

// ==========================================
// 1. REGISTER ENDPOINTS
// ==========================================

/**
 * POST /register/customer
 * Registers a new customer (immediately active)
 */
router.post('/register/customer', async (req, res) => {
  const { phone, password, full_name, address } = req.body;

  const phoneCheck = validateAlgerianPhone(phone);
  if (!phoneCheck.valid) {
    return res.status(400).json({ error: phoneCheck.error });
  }

  if (!password || password.length < 6) {
    return res.status(400).json({ error: 'كلمة المرور يجب أن لا تقل عن 6 أحرف أو أرقام' });
  }

  try {
    const existing = await pool.query('SELECT id FROM users WHERE phone = $1', [phoneCheck.phone]);
    if (existing.rowCount > 0) {
      return res.status(409).json({ error: 'رقم الهاتف مسجل مسبقاً في النظام' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const zoneId = process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';

    // Insert user (active)
    const userRes = await pool.query(
      `INSERT INTO users (phone, password_hash, full_name, role, status, zone_id)
       VALUES ($1, $2, $3, 'customer', 'active', $4)
       RETURNING id, phone, full_name, role, status, zone_id, created_at`,
      [phoneCheck.phone, hashedPassword, full_name || 'زبون جديد', zoneId]
    );

    const user = userRes.rows[0];

    // Insert customer profile
    await pool.query(
      `INSERT INTO customers (user_id, address)
       VALUES ($1, $2)
       ON CONFLICT (user_id) DO NOTHING`,
      [user.id, address || '']
    );

    // Generate tokens
    const tokens = await generateTokenPair(user);

    res.status(201).json({
      success: true,
      message: 'تم إنشاء حساب الزبون بنجاح',
      user,
      tokens
    });
  } catch (err) {
    console.error('Error in customer register:', err);
    res.status(500).json({ error: 'خطأ أثناء إنشاء حساب الزبون' });
  }
});

/**
 * POST /register/driver
 * Registers a new driver (status: pending until admin approval)
 */
router.post('/register/driver', async (req, res) => {
  const { phone, password, full_name, vehicle_type, license_plate } = req.body;

  const phoneCheck = validateAlgerianPhone(phone);
  if (!phoneCheck.valid) {
    return res.status(400).json({ error: phoneCheck.error });
  }

  if (!password || password.length < 6) {
    return res.status(400).json({ error: 'كلمة المرور يجب أن لا تقل عن 6 أحرف أو أرقام' });
  }

  if (!vehicle_type || !license_plate) {
    return res.status(400).json({ error: 'نوع المركبة ورقم اللوحة مطلوبان لتسجيل السائق' });
  }

  try {
    const existing = await pool.query('SELECT id FROM users WHERE phone = $1', [phoneCheck.phone]);
    if (existing.rowCount > 0) {
      return res.status(409).json({ error: 'رقم الهاتف مسجل مسبقاً في النظام' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const zoneId = process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';

    // Insert user (pending)
    const userRes = await pool.query(
      `INSERT INTO users (phone, password_hash, full_name, role, status, zone_id)
       VALUES ($1, $2, $3, 'driver', 'pending', $4)
       RETURNING id, phone, full_name, role, status, zone_id, created_at`,
      [phoneCheck.phone, hashedPassword, full_name || 'سائق جديد', zoneId]
    );

    const user = userRes.rows[0];

    // Insert driver profile
    await pool.query(
      `INSERT INTO drivers (user_id, vehicle_type, license_plate, is_available)
       VALUES ($1, $2, $3, FALSE)
       ON CONFLICT (user_id) DO NOTHING`,
      [user.id, vehicle_type, license_plate]
    );

    res.status(201).json({
      success: true,
      message: 'تم تسجيل طلب انضمام السائق بنجاح. الحساب قيد المراجعة بانتظار موافقة الإدارة.',
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        status: user.status
      },
      driver_profile: {
        vehicle_type,
        license_plate
      }
    });
  } catch (err) {
    console.error('Error in driver register:', err);
    res.status(500).json({ error: 'خطأ أثناء تسجيل حساب السائق' });
  }
});

/**
 * POST /register/store
 * Registers a new store (status: pending until admin approval)
 */
router.post('/register/store', async (req, res) => {
  const { phone, password, full_name, store_name, category, address } = req.body;

  const phoneCheck = validateAlgerianPhone(phone);
  if (!phoneCheck.valid) {
    return res.status(400).json({ error: phoneCheck.error });
  }

  if (!password || password.length < 6) {
    return res.status(400).json({ error: 'كلمة المرور يجب أن لا تقل عن 6 أحرف أو أرقام' });
  }

  if (!store_name) {
    return res.status(400).json({ error: 'اسم المتجر أو المطعم مطلوب' });
  }

  try {
    const existing = await pool.query('SELECT id FROM users WHERE phone = $1', [phoneCheck.phone]);
    if (existing.rowCount > 0) {
      return res.status(409).json({ error: 'رقم الهاتف مسجل مسبقاً في النظام' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const zoneId = process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';

    // Insert user (pending)
    const userRes = await pool.query(
      `INSERT INTO users (phone, password_hash, full_name, role, status, zone_id)
       VALUES ($1, $2, $3, 'store', 'pending', $4)
       RETURNING id, phone, full_name, role, status, zone_id, created_at`,
      [phoneCheck.phone, hashedPassword, full_name || store_name, zoneId]
    );

    const user = userRes.rows[0];

    // Insert into stores profile
    await pool.query(
      `INSERT INTO stores (user_id, name, category, address, phone, is_active)
       VALUES ($1, $2, $3, $4, $5, FALSE)
       ON CONFLICT (user_id) DO NOTHING`,
      [user.id, store_name, category || 'general', address || '', phoneCheck.phone]
    );

    // Also insert into legacy shops table for backward compatibility
    await pool.query(
      `INSERT INTO shops (user_id, name, category, address_description, zone_id, is_active)
       VALUES ($1, $2, $3, $4, $5, FALSE)`,
      [user.id, store_name, category || 'general', address || '', zoneId]
    );

    res.status(201).json({
      success: true,
      message: 'تم تسجيل المتجر بنجاح. الحساب قيد المراجعة بانتظار موافقة الإدارة.',
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        status: user.status
      },
      store_profile: {
        store_name,
        category: category || 'general',
        address: address || ''
      }
    });
  } catch (err) {
    console.error('Error in store register:', err);
    res.status(500).json({ error: 'خطأ أثناء تسجيل حساب المتجر' });
  }
});

// ==========================================
// 2. LOGIN / REFRESH / LOGOUT
// ==========================================

/**
 * POST /login
 * Login with phone & password. Returns Access Token + Refresh Token + Profile.
 */
router.post('/login', loginLimiter, async (req, res) => {
  const { phone, password } = req.body;

  if (!phone || !password) {
    return res.status(400).json({ error: 'رقم الهاتف وكلمة المرور مطلوبان' });
  }

  const phoneCheck = validateAlgerianPhone(phone);
  const targetPhone = phoneCheck.valid ? phoneCheck.phone : phone;

  try {
    const userRes = await pool.query('SELECT * FROM users WHERE phone = $1', [targetPhone]);
    if (userRes.rowCount === 0) {
      return res.status(401).json({ error: 'بيانات الدخول غير صحيحة (الهاتف أو كلمة المرور)' });
    }

    const user = userRes.rows[0];

    if (!user.password_hash) {
      return res.status(400).json({ error: 'هذا الحساب مسجل عبر رمز التحقق (OTP)، يرجى تعيين كلمة مرور أو استخدام OTP' });
    }

    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) {
      return res.status(401).json({ error: 'بيانات الدخول غير صحيحة (الهاتف أو كلمة المرور)' });
    }

    // Check account status
    if (user.status === 'suspended') {
      return res.status(403).json({ error: 'تم تعليق هذا الحساب. يرجى التواصل مع إدارة التطبيق.' });
    }

    // Reset rate limiter on successful authentication
    resetAttempts(req);

    // Generate tokens
    const tokens = await generateTokenPair(user);
    const profile = await getUserProfile(user.id, user.role);

    res.json({
      success: true,
      message: 'تم تسجيل الدخول بنجاح',
      tokens,
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: (user.role || '').toLowerCase(),
        status: user.status || 'active',
        zone_id: user.zone_id
      },
      profile
    });
  } catch (err) {
    console.error('Error in /login:', err);
    res.status(500).json({ error: 'خطأ أثناء تسجيل الدخول' });
  }
});

/**
 * POST /refresh
 * Refreshes an expired access token using a valid refresh token (Refresh Token Rotation)
 */
router.post('/refresh', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) {
    return res.status(400).json({ error: 'رمز التحديث (refreshToken) مطلوب' });
  }

  try {
    const newTokens = await rotateRefreshToken(refreshToken);
    res.json({
      success: true,
      message: 'تم تجديد الرمز بنجاح',
      tokens: newTokens
    });
  } catch (err) {
    res.status(401).json({ error: err.message || 'رمز التحديث غير صالح أو منتهي الصلاحية' });
  }
});

/**
 * POST /logout
 * Revokes the provided refresh token
 */
router.post('/logout', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) {
    return res.status(400).json({ error: 'رمز التحديث (refreshToken) مطلوب لتسجيل الخروج' });
  }

  try {
    await revokeRefreshToken(refreshToken);
    res.json({
      success: true,
      message: 'تم تسجيل الخروج بنجاح وإبطال الجلسة'
    });
  } catch (err) {
    console.error('Error in /logout:', err);
    res.status(500).json({ error: 'خطأ أثناء تسجيل الخروج' });
  }
});

/**
 * GET /me
 * Returns current authenticated user and profile
 */
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const userRes = await pool.query(
      `SELECT id, phone, full_name, role, status, zone_id, created_at, updated_at 
       FROM users WHERE id = $1`,
      [req.user.id]
    );

    if (userRes.rowCount === 0) {
      return res.status(404).json({ error: 'المستخدم غير موجود' });
    }

    const user = userRes.rows[0];
    const profile = await getUserProfile(user.id, user.role);

    res.json({
      success: true,
      user,
      profile
    });
  } catch (err) {
    console.error('Error in /me:', err);
    res.status(500).json({ error: 'خطأ في جلب بيانات المستخدم' });
  }
});

// ==========================================
// 3. LEGACY / OTP COMPATIBILITY ENDPOINTS
// ==========================================

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
      mock_code: result.mockCode
    });
  } catch (err) {
    console.error('Error sending OTP:', err);
    res.status(500).json({ error: 'حدث خطأ أثناء إرسال كود التحقق' });
  }
});

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
      const assignedRole = ['customer', 'driver', 'store'].includes((role || '').toLowerCase())
        ? role.toLowerCase()
        : 'customer';
      const initialStatus = assignedRole === 'customer' ? 'active' : 'pending';

      const insertRes = await pool.query(
        `INSERT INTO users (phone, full_name, role, status, zone_id)
         VALUES ($1, $2, $3, $4, $5)
         RETURNING *`,
        [normalized, full_name || 'مستخدم جديد', assignedRole, initialStatus, process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane']
      );
      user = insertRes.rows[0];

      if (assignedRole === 'customer') {
        await pool.query('INSERT INTO customers (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [user.id]);
      } else if (assignedRole === 'driver') {
        await pool.query('INSERT INTO drivers (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [user.id]);
      }
    } else {
      user = userRes.rows[0];
    }

    const tokens = await generateTokenPair(user);
    res.json({
      success: true,
      token: tokens.accessToken,
      tokens,
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        status: user.status,
        zone_id: user.zone_id
      }
    });
  } catch (err) {
    console.error('Error verifying OTP:', err);
    res.status(500).json({ error: 'خطأ في معالجة كود التحقق' });
  }
});

// Legacy login-password route for backward compatibility
router.post('/login-password', loginLimiter, async (req, res) => {
  const { phone, password } = req.body;
  if (!phone || !password) {
    return res.status(400).json({ error: 'يرجى إدخال الهاتف وكلمة المرور' });
  }

  const phoneCheck = validateAlgerianPhone(phone);
  const targetPhone = phoneCheck.valid ? phoneCheck.phone : phone;

  try {
    const userRes = await pool.query('SELECT * FROM users WHERE phone = $1', [targetPhone]);
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

    if (user.status === 'suspended') {
      return res.status(403).json({ error: 'تم تعليق هذا الحساب. يرجى التواصل مع الإدارة' });
    }

    resetAttempts(req);
    const tokens = await generateTokenPair(user);

    res.json({
      success: true,
      token: tokens.accessToken,
      tokens,
      user: {
        id: user.id,
        phone: user.phone,
        full_name: user.full_name,
        role: user.role,
        status: user.status,
        zone_id: user.zone_id
      }
    });
  } catch (err) {
    console.error('Error in login-password:', err);
    res.status(500).json({ error: 'خطأ في تسجيل الدخول' });
  }
});

module.exports = router;
