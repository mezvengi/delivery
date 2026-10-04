const express = require('express');
const router = express.Router();
const { pool } = require('../db');

const DEFAULT_NEIGHBORHOODS = [
  { name_arabic: 'وسط المدينة', name_french: 'Centre Ville', lat: 36.1480, lon: 3.6900 },
  { name_arabic: 'حي 500 مسكن', name_french: '500 Logements', lat: 36.1512, lon: 3.6945 },
  { name_arabic: 'حي الشهداء', name_french: 'Cité Chouhada', lat: 36.1450, lon: 3.6850 },
  { name_arabic: 'حي الرمل', name_french: 'Cité Er-Raml', lat: 36.1420, lon: 3.6980 },
  { name_arabic: 'حي 200 مسكن', name_french: '200 Logements', lat: 36.1540, lon: 3.6880 },
  { name_arabic: 'حي النصر', name_french: 'Cité En-Nasr', lat: 36.1465, lon: 3.7020 },
  { name_arabic: 'حي المصالحة الوطنية', name_french: 'Cité Réconciliation', lat: 36.1560, lon: 3.6920 },
  { name_arabic: 'حي 100 مسكن التساهمي', name_french: '100 Logements LSP', lat: 36.1410, lon: 3.6820 },
  { name_arabic: 'حي السلام', name_french: 'Cité Es-Salam', lat: 36.1495, lon: 3.6790 },
  { name_arabic: 'طريق البويرة', name_french: 'Route de Bouira', lat: 36.1600, lon: 3.6950 },
  { name_arabic: 'طريق سيدي عيسى', name_french: 'Route de Sidi Aïssa', lat: 36.1380, lon: 3.6920 },
  { name_arabic: 'طريق عين بسام', name_french: 'Route d Ain Bessem', lat: 36.1520, lon: 3.6750 }
];

// GET all neighborhoods
router.get('/', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM neighborhoods ORDER BY id ASC');
    if (result.rows.length === 0) {
      return res.json(DEFAULT_NEIGHBORHOODS);
    }
    res.json(result.rows);
  } catch (err) {
    res.json(DEFAULT_NEIGHBORHOODS);
  }
});

// POST new neighborhood
router.post('/', async (req, res) => {
  const { name_arabic, name_french, lat, lon } = req.body;
  if (!name_arabic || !name_arabic.trim()) {
    return res.status(400).json({ error: 'اسم الحي بالعربية مطلوب' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO neighborhoods (name_arabic, name_french, lat, lon)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (name_arabic) DO UPDATE
       SET name_french = EXCLUDED.name_french, lat = EXCLUDED.lat, lon = EXCLUDED.lon
       RETURNING *`,
      [
        name_arabic.trim(),
        (name_french || '').trim(),
        parseFloat(lat) || 36.1480,
        parseFloat(lon) || 3.6900
      ]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error saving neighborhood:', err);
    res.status(500).json({ error: 'فشل في حفظ الحي' });
  }
});

// PUT update neighborhood
router.put('/:id', async (req, res) => {
  const { id } = req.params;
  const { name_arabic, name_french, lat, lon } = req.body;

  try {
    const result = await pool.query(
      `UPDATE neighborhoods
       SET name_arabic = COALESCE($1, name_arabic),
           name_french = COALESCE($2, name_french),
           lat = COALESCE($3, lat),
           lon = COALESCE($4, lon)
       WHERE id = $5 RETURNING *`,
      [
        name_arabic ? name_arabic.trim() : null,
        name_french !== undefined ? name_french.trim() : null,
        lat ? parseFloat(lat) : null,
        lon ? parseFloat(lon) : null,
        id
      ]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'الحي غير موجود' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error updating neighborhood:', err);
    res.status(500).json({ error: 'فشل في تعديل الحي' });
  }
});

// DELETE neighborhood
router.delete('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await pool.query('DELETE FROM neighborhoods WHERE id = $1 RETURNING *', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'الحي غير موجود' });
    }
    res.json({ success: true, deleted: result.rows[0] });
  } catch (err) {
    console.error('Error deleting neighborhood:', err);
    res.status(500).json({ error: 'فشل في حذف الحي' });
  }
});

module.exports = router;
