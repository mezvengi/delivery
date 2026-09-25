const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgres://sour_user:sour_pass_2026@localhost:5432/sour_delivery',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

pool.on('error', (err) => {
  console.error('[PostgreSQL Error] Unexpected error on idle client:', err);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool,
};
