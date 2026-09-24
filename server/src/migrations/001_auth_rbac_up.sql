-- 001_auth_rbac_up.sql
-- Migration: Add Auth & RBAC support (users status, role profile tables, refresh_tokens)

BEGIN;

-- 1. Update users table with status and updated_at
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active';
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Normalize existing roles to lowercase standard ('customer', 'driver', 'store', 'admin')
UPDATE users SET role = 'customer' WHERE LOWER(role) = 'customer';
UPDATE users SET role = 'driver' WHERE LOWER(role) = 'driver';
UPDATE users SET role = 'store' WHERE LOWER(role) IN ('shop', 'store');
UPDATE users SET role = 'admin' WHERE LOWER(role) = 'admin';

-- Ensure all existing users have status 'active'
UPDATE users SET status = 'active' WHERE status IS NULL;

-- 2. Customers profile table
CREATE TABLE IF NOT EXISTS customers (
  id SERIAL PRIMARY KEY,
  user_id INTEGER UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  address TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backfill existing customers
INSERT INTO customers (user_id)
SELECT id FROM users WHERE role = 'customer'
ON CONFLICT (user_id) DO NOTHING;

-- 3. Drivers profile table
CREATE TABLE IF NOT EXISTS drivers (
  id SERIAL PRIMARY KEY,
  user_id INTEGER UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  vehicle_type VARCHAR(50) NOT NULL DEFAULT 'moto',
  license_plate VARCHAR(50) NOT NULL DEFAULT 'غير محدد',
  is_available BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backfill existing drivers
INSERT INTO drivers (user_id)
SELECT id FROM users WHERE role = 'driver'
ON CONFLICT (user_id) DO NOTHING;

-- 4. Stores profile table
CREATE TABLE IF NOT EXISTS stores (
  id SERIAL PRIMARY KEY,
  user_id INTEGER UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(150) NOT NULL,
  category VARCHAR(50) DEFAULT 'general',
  address TEXT,
  phone VARCHAR(20),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backfill existing stores from shops table
INSERT INTO stores (user_id, name, category, address, phone, is_active, created_at)
SELECT s.user_id, s.name, s.category, s.address_description, u.phone, s.is_active, s.created_at
FROM shops s
JOIN users u ON s.user_id = u.id
ON CONFLICT (user_id) DO NOTHING;

-- 5. Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

COMMIT;
