const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware } = require('../auth');

// Add new product
router.post('/', authMiddleware(['shop', 'admin']), async (req, res) => {
  const { shop_id, name, description, price, category, image_url } = req.body;
  if (!shop_id || !name || !price) {
    return res.status(400).json({ error: 'يرجى تحديد المتجر، اسم الوجبة والسعر' });
  }

  try {
    const result = await db.query(
      `INSERT INTO products (shop_id, name, description, price, category, image_url)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [shop_id, name, description || '', price, category || 'وجبات', image_url || '']
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Create product error:', err);
    res.status(500).json({ error: 'تعذر إضافة المنتج' });
  }
});

// Update product availability
router.patch('/:id/availability', authMiddleware(['shop', 'admin']), async (req, res) => {
  const { id } = req.params;
  const { is_available } = req.body;
  try {
    const result = await db.query(
      `UPDATE products SET is_available = $1 WHERE id = $2 RETURNING *`,
      [is_available, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'تعذر تحديث توفر الوجبة' });
  }
});

// Delete product
router.delete('/:id', authMiddleware(['shop', 'admin']), async (req, res) => {
  const { id } = req.params;
  try {
    await db.query(`DELETE FROM products WHERE id = $1`, [id]);
    res.json({ success: true, message: 'تم حذف المنتج بنجاح' });
  } catch (err) {
    res.status(500).json({ error: 'تعذر حذف المنتج' });
  }
});

module.exports = router;
