const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, validateAlgerianPhone, generateTokenPair } = require('../auth');

/**
 * PATCH /me/phone (and /api/users/me/phone)
 * Sets or updates the authenticated user's Algerian phone number.
 */
router.patch('/me/phone', authMiddleware, async (req, res) => {
  const { phone } = req.body;

  if (!phone) {
    return res.status(400).json({
      success: false,
      error: 'رقم الهاتف مطلوب لتحديث الملف الشخصي.',
    });
  }

  const phoneCheck = validateAlgerianPhone(phone);
  if (!phoneCheck.valid) {
    return res.status(400).json({
      success: false,
      error: phoneCheck.error || 'يرجى إدخال رقم هاتف جزائري صالح (05 / 06 / 07).',
    });
  }

  const normalized = phoneCheck.phone;
  const userId = req.user.id;

  try {
    // 1. Check if another user already owns this phone number
    const duplicateCheck = await pool.query(
      'SELECT id FROM users WHERE phone = $1 AND id != $2 LIMIT 1',
      [normalized, userId]
    );

    if (duplicateCheck.rows.length > 0) {
      return res.status(409).json({
        success: false,
        error: 'رقم الهاتف هذا مسجل بالفعل ومربوط بحساب آخر في النظام.',
      });
    }

    // 2. Update user phone number
    const updateResult = await pool.query(
      `UPDATE users 
       SET phone = $1, updated_at = NOW()
       WHERE id = $2 
       RETURNING id, full_name, phone, email, firebase_uid, photo_url, role, status, phone_verified`,
      [normalized, userId]
    );

    if (updateResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'المستخدم غير موجود.',
      });
    }

    const updatedUser = updateResult.rows[0];
    const tokens = await generateTokenPair(updatedUser);

    return res.status(200).json({
      success: true,
      message: 'تم حفظ وتحديث رقم الهاتف بنجاح! سيتم استخدامه لتأكيد طلباتك عند التوصيل 🛵',
      tokens,
      token: tokens.accessToken,
      user: {
        id: updatedUser.id,
        name: updatedUser.full_name,
        full_name: updatedUser.full_name,
        phone: updatedUser.phone,
        email: updatedUser.email,
        photo_url: updatedUser.photo_url,
        role: updatedUser.role,
        phone_verified: updatedUser.phone_verified === true,
        status: updatedUser.status || 'active',
      },
    });

  } catch (error) {
    console.error('❌ [Update Phone Error]:', error);
    return res.status(500).json({
      success: false,
      error: 'حدث خطأ في الخادم أثناء تحديث رقم الهاتف',
    });
  }
});

module.exports = router;
