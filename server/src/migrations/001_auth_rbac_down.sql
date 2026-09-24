-- 001_auth_rbac_down.sql
-- Rollback migration for 001_auth_rbac_up.sql

BEGIN;

DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS stores CASCADE;
DROP TABLE IF EXISTS drivers CASCADE;
DROP TABLE IF EXISTS customers CASCADE;

-- Revert users table modifications
ALTER TABLE users DROP COLUMN IF EXISTS status;
ALTER TABLE users DROP COLUMN IF EXISTS updated_at;

-- Restore role casing to legacy uppercase if needed
UPDATE users SET role = 'CUSTOMER' WHERE role = 'customer';
UPDATE users SET role = 'DRIVER' WHERE role = 'driver';
UPDATE users SET role = 'SHOP' WHERE role = 'store';
UPDATE users SET role = 'ADMIN' WHERE role = 'admin';

COMMIT;
