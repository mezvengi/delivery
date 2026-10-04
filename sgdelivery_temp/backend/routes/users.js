const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware, normalizeAlgerianPhone, generateToken } = require('../auth');

/**
 * PATCH /users/me/phone (and /api/users/me/phone)
 * Sets or updates the authenticated user's Algerian phone number.
 * Validates 05/06/07 Algerian format and ensures uniqueness across accounts.
 */
router.patch('/me/phone', authMiddleware(), async (req, res) => {
  const { phone } = req.body;

  if (!phone) {
    return res.status(400).json({
      success: false,
      error: 'رقم الهاتف مطلوب لتحديث الملف الشخصي.',
    });
  }

  const normalized = normalizeAlgerianPhone(phone);
  if (!normalized) {
    return res.status(400).json({
      success: false,
      error: 'يرجى إدخال رقم هاتف جزائري صالح (يبدأ بـ 05 أو 06 أو 07 ويتكون من 10 أرقام).',
    });
  }

  const userId = req.user.id;

  try {
    // 1. Check if another user already owns this phone number
    const duplicateCheck = await db.query(
      'SELECT id FROM users WHERE phone = $1 AND id != $2 LIMIT 1',
      [normalized, userId]
    );

    if (duplicateCheck.rows.length > 0) {
      return res.status(409).json({
        success: false,
        error: 'رقم الهاتف هذا مسجل بالفعل ومربوط بحساب آخر في النظام.',
      });
    }

    // 2. Update user phone number (keeping phone_verified false until confirmed)
    const updateResult = await db.query(
      `UPDATE users 
       SET phone = $1
       WHERE id = $2 
       RETURNING id, name, phone, email, firebase_uid, photo_url, role, is_active, phone_verified`,
      [normalized, userId]
    );

    if (updateResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'المستخدم غير موجود.',
      });
    }

    const updatedUser = updateResult.rows[0];

    // 3. Issue a fresh JWT containing the updated phone
    const newToken = generateToken(updatedUser);

    return res.status(200).json({
      success: true,
      message: 'تم حفظ وتحديث رقم الهاتف بنجاح! سيتم استخدامه لتأكيد طلباتك عند التوصيل 🛵',
      token: newToken,
      user: {
        id: updatedUser.id,
        name: updatedUser.name,
        full_name: updatedUser.name,
        phone: updatedUser.phone,
        email: updatedUser.email,
        photo_url: updatedUser.photo_url,
        role: updatedUser.role,
        phone_verified: updatedUser.phone_verified === true,
        status: updatedUser.is_active ? 'active' : 'pending',
      },
    });

  } catch (error) {
    console.error('❌ [Update Phone Error]:', error);
    return res.status(500).json({
      success: false,
      error: 'حدث خطأ في الخادم أثناء تحديث رقم الهاتف: ' + (error.message || 'حاول مجدداً'),
    });
  }
});

module.exports = router;
