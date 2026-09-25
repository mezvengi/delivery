const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, requireRole } = require('../auth');

// List active shops in zone
router.get('/', async (req, res) => {
  const zoneId = req.query.zone_id || process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';
  try {
    const result = await pool.query(
      'SELECT id, name, category, address_description, lat, lng, zone_id, is_active FROM shops WHERE zone_id = $1 AND is_active = TRUE ORDER BY id ASC',
      [zoneId]
    );
    res.json({ shops: result.rows });
  } catch (err) {
    console.error('Error fetching shops:', err);
    res.status(500).json({ error: 'خطأ في جلب قائمة المتاجر' });
  }
});

// Get single shop
router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM shops WHERE id = $1', [req.params.id]);
    if (result.rowCount === 0) return res.status(404).json({ error: 'المتجر غير موجود' });
    res.json({ shop: result.rows[0] });
  } catch (err) {
    res.status(500).json({ error: 'خطأ في جلب المتجر' });
  }
});

// Get shop products
router.get('/:id/products', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM products WHERE shop_id = $1 AND is_available = TRUE ORDER BY id ASC',
      [req.params.id]
    );
    res.json({ products: result.rows });
  } catch (err) {
    res.status(500).json({ error: 'خطأ في جلب المنتجات' });
  }
});

// Create new shop (ADMIN or SHOP)
router.post('/', authMiddleware, requireRole('ADMIN', 'SHOP'), async (req, res) => {
  const { name, category, address_description, lat, lng, zone_id } = req.body;
  if (!name) return res.status(400).json({ error: 'اسم المتجر مطلوب' });

  try {
    const result = await pool.query(
      `INSERT INTO shops (user_id, name, category, address_description, lat, lng, zone_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        req.user.id,
        name,
        category || 'general',
        address_description || '',
        lat || 36.148,
        lng || 3.690,
        zone_id || req.user.zone_id || 'sour_el_ghozlane'
      ]
    );
    res.status(201).json({ success: true, shop: result.rows[0] });
  } catch (err) {
    console.error('Error creating shop:', err);
    res.status(500).json({ error: 'خطأ في إنشاء المتجر' });
  }
});

// Add product to shop (Shop owner or Admin)
router.post('/:id/products', authMiddleware, requireRole('admin', 'store', 'shop'), async (req, res) => {
  const shopId = req.params.id;
  const { name, description, price_da, image_url } = req.body;

  if (!name || price_da === undefined) {
    return res.status(400).json({ error: 'اسم المنتج والسعر بالدينار مطلوبان' });
  }

  try {
    const userRole = (req.user.role || '').toLowerCase();
    // If store role, check ownership
    if (userRole === 'shop' || userRole === 'store') {
      const check = await pool.query('SELECT user_id FROM shops WHERE id = $1', [shopId]);
      if (check.rowCount === 0 || check.rows[0].user_id !== req.user.id) {
        return res.status(403).json({ error: 'غير مصرح لك بإضافة منتجات لهذا المتجر' });
      }
    }

    const result = await pool.query(
      `INSERT INTO products (shop_id, name, description, price_da, image_url)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [shopId, name, description || '', price_da, image_url || '']
    );

    res.status(201).json({ success: true, product: result.rows[0] });
  } catch (err) {
    console.error('Error adding product:', err);
    res.status(500).json({ error: 'خطأ في إضافة المنتج' });
  }
});

module.exports = router;
