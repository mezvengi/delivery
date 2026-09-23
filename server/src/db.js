const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const pool = new Pool({
  host: process.env.DB_HOST || 'postgres',
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER || 'sour_admin',
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME || 'sour_delivery',
});

async function initDB() {
  const client = await pool.connect();
  try {
    console.log('[DB] Connected to PostgreSQL. Initializing tables...');

    // Users table
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        phone VARCHAR(20) UNIQUE NOT NULL,
        full_name VARCHAR(100),
        password_hash VARCHAR(255),
        role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER, DRIVER, SHOP, ADMIN
        zone_id VARCHAR(50) NOT NULL DEFAULT 'sour_el_ghozlane',
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);

    // OTP table
    await client.query(`
      CREATE TABLE IF NOT EXISTS otps (
        id SERIAL PRIMARY KEY,
        phone VARCHAR(20) NOT NULL,
        code VARCHAR(6) NOT NULL,
        expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
        used BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
      CREATE INDEX IF NOT EXISTS idx_otps_phone ON otps(phone);
    `);

    // Shops table
    await client.query(`
      CREATE TABLE IF NOT EXISTS shops (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        name VARCHAR(150) NOT NULL,
        category VARCHAR(50) DEFAULT 'general',
        address_description TEXT,
        lat NUMERIC(10, 6) DEFAULT 36.148000,
        lng NUMERIC(10, 6) DEFAULT 3.690000,
        zone_id VARCHAR(50) NOT NULL DEFAULT 'sour_el_ghozlane',
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);

    // Products table
    await client.query(`
      CREATE TABLE IF NOT EXISTS products (
        id SERIAL PRIMARY KEY,
        shop_id INTEGER REFERENCES shops(id) ON DELETE CASCADE,
        name VARCHAR(150) NOT NULL,
        description TEXT,
        price_da NUMERIC(10, 2) NOT NULL,
        image_url TEXT,
        is_available BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);

    // Orders table
    await client.query(`
      CREATE TABLE IF NOT EXISTS orders (
        id SERIAL PRIMARY KEY,
        customer_id INTEGER REFERENCES users(id),
        shop_id INTEGER REFERENCES shops(id),
        driver_id INTEGER REFERENCES users(id),
        status VARCHAR(30) NOT NULL DEFAULT 'NEW', -- NEW, CONFIRMED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
        zone_id VARCHAR(50) NOT NULL DEFAULT 'sour_el_ghozlane',
        delivery_neighborhood VARCHAR(150) NOT NULL,
        delivery_description TEXT,
        delivery_lat NUMERIC(10, 6),
        delivery_lng NUMERIC(10, 6),
        items_total_da NUMERIC(10, 2) NOT NULL DEFAULT 0,
        delivery_fee_da NUMERIC(10, 2) NOT NULL DEFAULT 200,
        total_amount_da NUMERIC(10, 2) NOT NULL DEFAULT 200,
        payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
        notes TEXT,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);

    // Order items table
    await client.query(`
      CREATE TABLE IF NOT EXISTS order_items (
        id SERIAL PRIMARY KEY,
        order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
        product_id INTEGER REFERENCES products(id),
        product_name VARCHAR(150) NOT NULL,
        unit_price_da NUMERIC(10, 2) NOT NULL,
        quantity INTEGER NOT NULL DEFAULT 1,
        subtotal_da NUMERIC(10, 2) NOT NULL
      );
    `);

    // Driver locations table
    await client.query(`
      CREATE TABLE IF NOT EXISTS driver_locations (
        driver_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        lat NUMERIC(10, 6) NOT NULL,
        lng NUMERIC(10, 6) NOT NULL,
        heading NUMERIC(6, 2) DEFAULT 0,
        is_online BOOLEAN DEFAULT TRUE,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);

    // Seed default admin account if not existing
    const adminPhone = process.env.ADMIN_PHONE || '+213555000000';
    const adminPass = process.env.ADMIN_PASSWORD || 'AdminSour2026!';
    const existingAdmin = await client.query('SELECT id FROM users WHERE phone = $1', [adminPhone]);
    if (existingAdmin.rowCount === 0) {
      const hashed = await bcrypt.hash(adminPass, 10);
      await client.query(`
        INSERT INTO users (phone, full_name, password_hash, role, zone_id)
        VALUES ($1, $2, $3, 'ADMIN', $4)
      `, [adminPhone, 'مسؤول النظام (سور الغزلان)', hashed, process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane']);
      console.log(`[DB] Default admin account seeded: ${adminPhone}`);
    }

    console.log('[DB] Database schema and initial data ready.');
  } finally {
    client.release();
  }
}

module.exports = { pool, initDB };
