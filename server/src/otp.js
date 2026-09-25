const { pool } = require('./db');

function normalizePhone(phone) {
  if (!phone) return '';
  let cleaned = phone.replace(/[^\d+]/g, '');
  if (cleaned.startsWith('0')) {
    cleaned = '+213' + cleaned.substring(1);
  } else if (!cleaned.startsWith('+') && cleaned.startsWith('213')) {
    cleaned = '+' + cleaned;
  } else if (!cleaned.startsWith('+')) {
    cleaned = '+213' + cleaned;
  }
  return cleaned;
}

async function sendOTP(phone) {
  const normalized = normalizePhone(phone);
  const code = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 mins

  await pool.query(
    'INSERT INTO otps (phone, code, expires_at) VALUES ($1, $2, $3)',
    [normalized, code, expiresAt]
  );

  const adminWhatsAppPhone = process.env.ADMIN_WHATSAPP_PHONE || '213555000000';
  const telegramBotUsername = process.env.TELEGRAM_BOT_USERNAME || 'SGdelivery_bot';

  const whatsappText = encodeURIComponent(`السلام عليكم، كود تفعيل حسابي في تطبيق SGdelivery لسور الغزلان هو: *${code}* (رقم هاتفي: ${normalized})`);
  const whatsappUrl = `https://wa.me/${adminWhatsAppPhone}?text=${whatsappText}`;
  const telegramUrl = `https://t.me/${telegramBotUsername}?start=verify_${code}`;

  console.log(`[OTP] Generated OTP for ${normalized}: ${code}`);

  return {
    phone: normalized,
    code,
    expiresAt,
    whatsappUrl,
    telegramUrl
  };
}

async function verifyOTP(phone, code) {
  const normalized = normalizePhone(phone);
  const res = await pool.query(
    `SELECT * FROM otps 
     WHERE phone = $1 AND code = $2 AND used = FALSE AND expires_at > NOW()
     ORDER BY created_at DESC LIMIT 1`,
    [normalized, code.trim()]
  );

  if (res.rowCount === 0) {
    return false;
  }

  const otpRecord = res.rows[0];
  await pool.query('UPDATE otps SET used = TRUE WHERE id = $1', [otpRecord.id]);
  return true;
}

module.exports = { normalizePhone, sendOTP, verifyOTP };
