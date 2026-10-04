require('dotenv').config();
const { pool } = require('./src/db');

async function seedData() {
  console.log('🌱 Starting DB Seeding for SGdelivery...');
  try {
    const shopResult = await pool.query(`
      INSERT INTO shops (name, category, neighborhood, address_description, lat, lng, zone_id, is_active)
      VALUES 
      ('مطعم الأوراس', 'مطاعم ومأكولات', 'وسط المدينة', 'شارع أول نوفمبر', 36.1485, 3.6905, 'sour_el_ghozlane', TRUE),
      ('بيتزا البرج', 'مطاعم ومأكولات', 'حي الوئام', 'مقابل ساحة الشهداء', 36.1520, 3.6960, 'sour_el_ghozlane', TRUE)
      ON CONFLICT DO NOTHING RETURNING id;
    `);

    console.log(`✅ Seeded ${shopResult.rowCount} shops.`);

    const shops = await pool.query('SELECT id FROM shops LIMIT 2');
    if (shops.rowCount > 0) {
      const shop1Id = shops.rows[0].id;
      const prodResult = await pool.query(`
        INSERT INTO products (shop_id, name, description, price_da, category)
        VALUES 
        ($1, 'شاورما دجاج', 'خبز سوري مع صلصة الثوم', 400, 'وجبات'),
        ($1, 'بيتزا مارغريتا', 'حجم عائلي', 500, 'وجبات')
        ON CONFLICT DO NOTHING;
      `, [shop1Id]);
      console.log(`✅ Seeded ${prodResult.rowCount} products for shop ${shop1Id}.`);
    }

    console.log('✅ Seeding complete!');
  } catch (err) {
    console.error('❌ Seeding Error:', err);
  } finally {
    process.exit(0);
  }
}

seedData();
