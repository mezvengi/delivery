const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'sour-default-jwt-secret-key-change-me';

function generateToken(user) {
  return jwt.sign(
    {
      id: user.id,
      phone: user.phone,
      role: user.role,
      zone_id: user.zone_id,
      full_name: user.full_name
    },
    JWT_SECRET,
    { expiresIn: '30d' }
  );
}

function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'غير مصرح - الرجاء تسجيل الدخول أولاً' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'الجلسة منتهية أو الرمز غير صالح' });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'ليست لديك الصلاحية لتنفيذ هذا الإجراء' });
    }
    next();
  };
}

module.exports = { generateToken, authMiddleware, requireRole };
