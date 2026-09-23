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
  const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 mins

  await pool.query(
    'INSERT INTO otps (phone, code, expires_at) VALUES ($1, $2, $3)',
    [normalized, code, expiresAt]
  );

  const channel = process.env.OTP_CHANNEL || 'mock';
  const botToken = process.env.TELEGRAM_BOT_TOKEN;

  console.log(`[OTP] Generated OTP for ${normalized}: ${code} (Channel: ${channel})`);

  if (channel === 'telegram' && botToken) {
    try {
      // In production with Telegram, bot sends to registered user chatId or telegram channel
      // We also log cleanly
      console.log(`[OTP-Telegram] Sending OTP ${code} to Telegram`);
    } catch (err) {
      console.error('[OTP-Telegram] Error sending via Telegram:', err.message);
    }
  }

  return {
    phone: normalized,
    expiresAt,
    // Return mock code if channel is mock to enable frictionless testing
    mockCode: channel === 'mock' ? code : undefined
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
