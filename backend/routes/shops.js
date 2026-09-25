const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware } = require('../auth');

// List all shops in Sour El Ghozlane
router.get('/', async (req, res) => {
  try {
    const result = await db.query(
      `SELECT s.*, 
              (SELECT COUNT(*) FROM products p WHERE p.shop_id = s.id AND p.is_available = TRUE) AS product_count
       FROM shops s
       ORDER BY s.is_open DESC, s.name ASC`
    );
    res.json(result.rows);
  } catch (err) {
    console.error('Fetch shops error:', err);
    res.status(500).json({ error: 'تعذر جلب قائمة المتاجر' });
  }
});

// Get single shop with all products grouped
router.get('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const shopResult = await db.query(`SELECT * FROM shops WHERE id = $1`, [id]);
    if (shopResult.rows.length === 0) {
      return res.status(404).json({ error: 'المتجر غير موجود' });
    }

    const productsResult = await db.query(
      `SELECT * FROM products WHERE shop_id = $1 ORDER BY category ASC, name ASC`,
      [id]
    );

    res.json({
      shop: shopResult.rows[0],
      products: productsResult.rows,
    });
  } catch (err) {
    console.error('Fetch shop detail error:', err);
    res.status(500).json({ error: 'تعذر جلب بيانات المتجر' });
  }
});

// Update shop status (Open/Closed)
router.patch('/:id/status', authMiddleware(['shop', 'admin']), async (req, res) => {
  const { id } = req.params;
  const { is_open } = req.body;
  try {
    const result = await db.query(
      `UPDATE shops SET is_open = $1 WHERE id = $2 RETURNING *`,
      [is_open, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'تعذر تحديث حالة المتجر' });
  }
});

module.exports = router;
