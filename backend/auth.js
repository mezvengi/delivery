const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const db = require('./db');

const JWT_SECRET = process.env.JWT_SECRET || 'sour_el_ghozlane_default_jwt_secret_2026';

/**
 * Normalizes Algerian phone numbers:
 * Inputs: "0550123456", "0660123456", "0770123456", "550123456", "+213550123456", "00213550123456"
 * Output: "+213550123456"
 */
function normalizeAlgerianPhone(input) {
  if (!input) return null;
  let cleaned = input.toString().replace(/[\s\-\(\)]/g, '').trim();

  if (cleaned.startsWith('00213')) {
    cleaned = '+213' + cleaned.substring(5);
  } else if (cleaned.startsWith('213')) {
    cleaned = '+' + cleaned;
  } else if (cleaned.startsWith('0') && (cleaned[1] === '5' || cleaned[1] === '6' || cleaned[1] === '7')) {
    cleaned = '+213' + cleaned.substring(1);
  } else if (cleaned.length === 9 && (cleaned[0] === '5' || cleaned[0] === '6' || cleaned[0] === '7')) {
    cleaned = '+213' + cleaned;
  }

  // Validate: must be +213 followed by 5/6/7 and 8 digits (total 13 chars)
  const regex = /^\+213[567][0-9]{8}$/;
  if (!regex.test(cleaned)) {
    return null;
  }
  return cleaned;
}

function generateToken(user) {
  return jwt.sign(
    {
      id: user.id,
      phone: user.phone,
      role: user.role,
      name: user.name,
    },
    JWT_SECRET,
    { expiresIn: '30d' }
  );
}

function verifyToken(token) {
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch (err) {
    return null;
  }
}

function authMiddleware(allowedRoles = []) {
  return (req, res, next) => {
    const authHeader = req.headers['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'رمز التوثيق غير متوفر (Unauthorized)' });
    }
    const token = authHeader.split(' ')[1];
    const decoded = verifyToken(token);
    if (!decoded) {
      return res.status(401).json({ error: 'جلسة التوثيق منتهية أو غير صالحة' });
    }
    if (allowedRoles.length > 0 && !allowedRoles.includes(decoded.role)) {
      return res.status(403).json({ error: 'ليس لديك صلاحية للوصول إلى هذا المورد' });
    }
    req.user = decoded;
    next();
  };
}

async function sendTelegramOtp(phone, code) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chatId = process.env.TELEGRAM_ADMIN_CHAT_ID;
  if (!token || !chatId) {
    console.log(`[Mock OTP for Sour El Ghozlane] Phone: ${phone} | Code: ${code}`);
    return;
  }
  try {
    const message = `🛵 رمز تحقق سور الغزلان (Sour Delivery):\nالرقم: ${phone}\nالرمز: ${code}\nصالح لمدة 5 دقائق.`;
    const url = `https://api.telegram.org/bot${token}/sendMessage?chat_id=${chatId}&text=${encodeURIComponent(message)}`;
    // Native fetch in Node 18+
    await fetch(url);
  } catch (err) {
    console.error('[Telegram OTP Error]:', err.message);
  }
}

module.exports = {
  normalizeAlgerianPhone,
  generateToken,
  verifyToken,
  authMiddleware,
  hashPassword: (p) => bcrypt.hash(p, 10),
  comparePassword: (p, h) => bcrypt.compare(p, h),
  sendTelegramOtp,
};
