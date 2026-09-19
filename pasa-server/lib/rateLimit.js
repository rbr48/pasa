'use strict';

// Lightweight in-memory rate limiter (no external dependency).
// Suitable for a single-process personal deployment. Keyed by client IP.
function rateLimit({ windowMs, max, message }) {
  const hits = new Map(); // ip -> { count, resetAt }
  // Periodically evict stale buckets so the map cannot grow unbounded.
  setInterval(() => {
    const now = Date.now();
    for (const [ip, rec] of hits) if (now > rec.resetAt) hits.delete(ip);
  }, windowMs).unref?.();

  return (req, res, next) => {
    const ip = req.ip || (req.connection && req.connection.remoteAddress) || 'unknown';
    const now = Date.now();
    let rec = hits.get(ip);
    if (!rec || now > rec.resetAt) {
      rec = { count: 0, resetAt: now + windowMs };
      hits.set(ip, rec);
    }
    rec.count += 1;
    if (rec.count > max) {
      const retryAfter = Math.ceil((rec.resetAt - now) / 1000);
      res.set('Retry-After', String(retryAfter));
      return res.status(429).json({ ok: false, description: message || 'Too many requests, slow down.' });
    }
    next();
  };
}

module.exports = { rateLimit };
