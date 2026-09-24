// Simple in-memory sliding window rate limiter
const attempts = new Map();

/**
 * Cleanup expired attempts every 5 minutes
 */
setInterval(() => {
  const now = Date.now();
  for (const [key, data] of attempts.entries()) {
    if (now > data.resetTime) {
      attempts.delete(key);
    }
  }
}, 5 * 60 * 1000).unref();

/**
 * Rate limiting middleware for authentication routes (login / brute force protection)
 * @param {Object} options
 * @param {number} options.windowMs Window time in milliseconds (default: 15 mins)
 * @param {number} options.max Maximum attempts per window (default: 5)
 */
function createRateLimiter(options = {}) {
  const windowMs = options.windowMs || 15 * 60 * 1000; // 15 minutes
  const max = options.max || 5;
  const message = options.message || 'تم تجاوز الحد الأقصى للمحاولات. يرجى الانتظار 15 دقيقة قبل المحاولة مرة أخرى.';

  return (req, res, next) => {
    // Key by IP address (plus phone if provided in body)
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'unknown-ip';
    const phone = req.body && req.body.phone ? String(req.body.phone).trim() : '';
    const key = phone ? `${ip}:${phone}` : ip;

    const now = Date.now();
    let record = attempts.get(key);

    if (!record || now > record.resetTime) {
      record = {
        count: 1,
        resetTime: now + windowMs
      };
      attempts.set(key, record);
      return next();
    }

    if (record.count >= max) {
      const waitMinutes = Math.ceil((record.resetTime - now) / 60000);
      return res.status(429).json({
        error: message,
        retryAfterMinutes: waitMinutes
      });
    }

    record.count += 1;
    next();
  };
}

// Reset attempts for a key upon successful login
function resetAttempts(req) {
  const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'unknown-ip';
  const phone = req.body && req.body.phone ? String(req.body.phone).trim() : '';
  const key = phone ? `${ip}:${phone}` : ip;
  attempts.delete(key);
}

module.exports = { createRateLimiter, resetAttempts };
