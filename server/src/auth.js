const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { pool } = require('./db');

if (process.env.NODE_ENV === 'production') {
  if (!process.env.JWT_ACCESS_SECRET && !process.env.JWT_SECRET) {
    console.error('❌ FATAL: JWT_ACCESS_SECRET (or JWT_SECRET) must be set in production!');
    process.exit(1);
  }
  if (!process.env.JWT_REFRESH_SECRET) {
    console.error('❌ FATAL: JWT_REFRESH_SECRET must be set in production!');
    process.exit(1);
  }
}
const JWT_ACCESS_SECRET = process.env.JWT_ACCESS_SECRET || process.env.JWT_SECRET || 'sour-dev-only-access-secret';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'sour-dev-only-refresh-secret';
const ACCESS_TOKEN_EXPIRES_IN = process.env.ACCESS_TOKEN_EXPIRES_IN || '15m';
const REFRESH_TOKEN_EXPIRES_IN = process.env.REFRESH_TOKEN_EXPIRES_IN || '7d';

/**
 * Validate and normalize Algerian phone number.
 * Accepts formats: 05XXXXXXXX, 06XXXXXXXX, 07XXXXXXXX, +213XXXXXXXXX, 213XXXXXXXXX, 00213XXXXXXXXX
 */
function validateAlgerianPhone(phone) {
  if (!phone || typeof phone !== 'string') {
    return { valid: false, error: 'رقم الهاتف مطلوب' };
  }

  // Remove whitespace, dashes, slashes
  let cleaned = phone.replace(/[\s\-\(\)\.]/g, '');

  if (cleaned.startsWith('00213')) {
    cleaned = '+213' + cleaned.substring(5);
  } else if (cleaned.startsWith('213')) {
    cleaned = '+' + cleaned;
  } else if (cleaned.startsWith('0')) {
    cleaned = '+213' + cleaned.substring(1);
  } else if (!cleaned.startsWith('+')) {
    cleaned = '+213' + cleaned;
  }

  // Algerian mobile and general pattern (+213 followed by 9 digits starting with 5, 6, 7, 2, 3, or 4)
  const algerianRegex = /^\+213[2-9]\d{8}$/;
  if (!algerianRegex.test(cleaned)) {
    return {
      valid: false,
      error: 'رقم الهاتف غير صالح. يجب أن يكون رقماً جزائرياً صحيحاً (مثال: 0555000000 أو +213555000000)'
    };
  }

  return { valid: true, phone: cleaned };
}

/**
 * Hash a token string using SHA-256 for secure DB storage
 */
function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

/**
 * Generate Access Token (15 minutes by default)
 */
function generateAccessToken(user) {
  return jwt.sign(
    {
      id: user.id,
      phone: user.phone,
      role: (user.role || '').toLowerCase(),
      status: user.status || 'active',
      zone_id: user.zone_id,
      full_name: user.full_name
    },
    JWT_ACCESS_SECRET,
    { expiresIn: ACCESS_TOKEN_EXPIRES_IN }
  );
}

/**
 * Generate Refresh Token (7 days by default) and save to DB
 */
async function generateRefreshToken(userId) {
  const tokenId = crypto.randomUUID();
  const token = jwt.sign(
    {
      userId,
      jti: tokenId
    },
    JWT_REFRESH_SECRET,
    { expiresIn: REFRESH_TOKEN_EXPIRES_IN }
  );

  const tokenHash = hashToken(token);
  // Calculate expiration date (7 days from now)
  const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

  await pool.query(
    `INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked)
     VALUES ($1, $2, $3, FALSE)`,
    [userId, tokenHash, expiresAt]
  );

  return token;
}

/**
 * Generate token pair (Access + Refresh)
 */
async function generateTokenPair(user) {
  const accessToken = generateAccessToken(user);
  const refreshToken = await generateRefreshToken(user.id);
  return {
    accessToken,
    refreshToken,
    expiresIn: ACCESS_TOKEN_EXPIRES_IN
  };
}

/**
 * Verify Refresh Token, revoke it, and issue a fresh Token Pair (Refresh Token Rotation)
 */
async function rotateRefreshToken(token) {
  if (!token) {
    throw new Error('الرمز غير موجود');
  }

  let decoded;
  try {
    decoded = jwt.verify(token, JWT_REFRESH_SECRET);
  } catch (err) {
    throw new Error('رمز التحديث غير صالح أو منتهي الصلاحية');
  }

  const tokenHash = hashToken(token);
  const res = await pool.query(
    `SELECT * FROM refresh_tokens 
     WHERE token_hash = $1 AND revoked = FALSE AND expires_at > NOW()`,
    [tokenHash]
  );

  if (res.rowCount === 0) {
    throw new Error('رمز التحديث تم إبطاله أو غير موجود');
  }

  // Revoke the used refresh token immediately
  await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = $1', [tokenHash]);

  // Fetch current user details
  const userRes = await pool.query('SELECT * FROM users WHERE id = $1', [decoded.userId]);
  if (userRes.rowCount === 0) {
    throw new Error('المستخدم غير موجود');
  }

  const user = userRes.rows[0];
  if (user.status === 'suspended') {
    throw new Error('تم تعليق هذا الحساب. يرجى مراجعة إدارة النظام');
  }

  // Generate new pair
  return generateTokenPair(user);
}

/**
 * Revoke a refresh token (used on Logout)
 */
async function revokeRefreshToken(token) {
  if (!token) return false;
  try {
    const tokenHash = hashToken(token);
    const result = await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = $1', [tokenHash]);
    return result.rowCount > 0;
  } catch (err) {
    console.error('Error revoking token:', err);
    return false;
  }
}

/**
 * Revoke all active refresh tokens for a user (security wipe)
 */
async function revokeAllUserTokens(userId) {
  await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = $1', [userId]);
}

/**
 * Express middleware to authenticate Access Token
 */
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'غير مصرح - يرجى تسجيل الدخول أولاً' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_ACCESS_SECRET);
    if (decoded.status === 'suspended') {
      return res.status(403).json({ error: 'تم تعليق هذا الحساب. يرجى التواصل مع الإدارة' });
    }
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'انتهت صلاحية الجلسة أو الرمز غير صالح' });
  }
}

/**
 * Express middleware to enforce Role-Based Access Control (RBAC)
 */
function requireRole(...roles) {
  const normalizedRoles = roles.map(r => r.toLowerCase());
  return (req, res, next) => {
    if (!req.user || !normalizedRoles.includes((req.user.role || '').toLowerCase())) {
      return res.status(403).json({ error: 'ليست لديك الصلاحية لتنفيذ هذا الإجراء' });
    }
    next();
  };
}

module.exports = {
  validateAlgerianPhone,
  generateAccessToken,
  generateRefreshToken,
  generateTokenPair,
  rotateRefreshToken,
  revokeRefreshToken,
  revokeAllUserTokens,
  authMiddleware,
  requireRole,
  hashToken
};
