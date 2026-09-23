const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, requireRole } = require('../auth');
const { broadcastOrderStatus } = require('../websocket');

// Create order (Customer)
router.post('/', authMiddleware, async (req, res) => {
  const {
    shop_id,
    items, // [{ product_id, quantity }]
    delivery_neighborhood,
    delivery_description,
    delivery_lat,
    delivery_lng,
    notes
  } = req.body;

  if (!shop_id || !items || !Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ error: 'بيانات الطلب والمنتجات غير مكتملة' });
  }

  if (!delivery_neighborhood) {
    return res.status(400).json({ error: 'يرجى تحديد الحي / العنوان' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // Verify shop exists
    const shopRes = await client.query('SELECT * FROM shops WHERE id = $1', [shop_id]);
    if (shopRes.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'المتجر غير موجود' });
    }
    const shop = shopRes.rows[0];

    // Calculate items total
    let itemsTotal = 0;
    const resolvedItems = [];

    for (const item of items) {
      const pRes = await client.query('SELECT * FROM products WHERE id = $1 AND shop_id = $2', [item.product_id, shop_id]);
      if (pRes.rowCount === 0) {
        await client.query('ROLLBACK');
        return res.status(400).json({ error: `المنتج رقم ${item.product_id} غير متوفر في هذا المتجر` });
      }
      const prod = pRes.rows[0];
      const qty = parseInt(item.quantity || 1);
      const subtotal = parseFloat(prod.price_da) * qty;
      itemsTotal += subtotal;

      resolvedItems.push({
        product_id: prod.id,
        product_name: prod.name,
        unit_price_da: parseFloat(prod.price_da),
        quantity: qty,
        subtotal_da: subtotal
      });
    }

    const deliveryFee = parseFloat(process.env.FIXED_DELIVERY_FEE || '200');
    const totalAmount = itemsTotal + deliveryFee;
    const zoneId = shop.zone_id || process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane';
    const initialStatus = req.body.driver_id ? 'CONFIRMED' : 'NEW';

    // Insert order
    const orderRes = await client.query(
      `INSERT INTO orders (
        customer_id, shop_id, driver_id, status, zone_id,
        delivery_neighborhood, delivery_description, delivery_lat, delivery_lng,
        items_total_da, delivery_fee_da, total_amount_da, payment_method, notes
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'COD', $13)
      RETURNING *`,
      [
        req.user.id,
        shop_id,
        req.body.driver_id || null,
        initialStatus,
        zoneId,
        delivery_neighborhood,
        delivery_description || '',
        delivery_lat || null,
        delivery_lng || null,
        itemsTotal,
        deliveryFee,
        totalAmount,
        notes || ''
      ]
    );

    const newOrder = orderRes.rows[0];

    // Insert order items
    for (const ri of resolvedItems) {
      await client.query(
        `INSERT INTO order_items (order_id, product_id, product_name, unit_price_da, quantity, subtotal_da)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [newOrder.id, ri.product_id, ri.product_name, ri.unit_price_da, ri.quantity, ri.subtotal_da]
      );
    }

    await client.query('COMMIT');

    newOrder.items = resolvedItems;
    newOrder.shop_name = shop.name;

    // Real-time broadcast
    broadcastOrderStatus(newOrder);

    res.status(201).json({ success: true, order: newOrder });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error creating order:', err);
    res.status(500).json({ error: 'خطأ في إنشاء الطلب' });
  } finally {
    client.release();
  }
});

// List orders (Filtered by role)
router.get('/', authMiddleware, async (req, res) => {
  try {
    let query = `
      SELECT o.*, s.name as shop_name, c.full_name as customer_name, c.phone as customer_phone,
             d.full_name as driver_name, d.phone as driver_phone
      FROM orders o
      LEFT JOIN shops s ON o.shop_id = s.id
      LEFT JOIN users c ON o.customer_id = c.id
      LEFT JOIN users d ON o.driver_id = d.id
    `;
    const params = [];

    if (req.user.role === 'CUSTOMER') {
      params.push(req.user.id);
      query += ` WHERE o.customer_id = $${params.length}`;
    } else if (req.user.role === 'DRIVER') {
      params.push(req.user.id);
      query += ` WHERE o.driver_id = $${params.length}`;
    } else if (req.user.role === 'SHOP') {
      params.push(req.user.id);
      query += ` WHERE s.user_id = $${params.length}`;
    } // ADMIN gets all orders

    query += ' ORDER BY o.id DESC';
    const result = await pool.query(query, params);
    res.json({ orders: result.rows });
  } catch (err) {
    console.error('Error fetching orders:', err);
    res.status(500).json({ error: 'خطأ في جلب الطلبات' });
  }
});

// Get order details
router.get('/:id', authMiddleware, async (req, res) => {
  try {
    const orderRes = await pool.query(
      `SELECT o.*, s.name as shop_name, c.full_name as customer_name, c.phone as customer_phone,
              d.full_name as driver_name, d.phone as driver_phone
       FROM orders o
       LEFT JOIN shops s ON o.shop_id = s.id
       LEFT JOIN users c ON o.customer_id = c.id
       LEFT JOIN users d ON o.driver_id = d.id
       WHERE o.id = $1`,
      [req.params.id]
    );

    if (orderRes.rowCount === 0) return res.status(404).json({ error: 'الطلب غير موجود' });
    const order = orderRes.rows[0];

    const itemsRes = await pool.query('SELECT * FROM order_items WHERE order_id = $1', [order.id]);
    order.items = itemsRes.rows;

    res.json({ order });
  } catch (err) {
    res.status(500).json({ error: 'خطأ في جلب تفاصيل الطلب' });
  }
});

// Customer selects driver for their order
router.post('/:id/select-driver', authMiddleware, async (req, res) => {
  const { driver_id } = req.body;
  if (!driver_id) return res.status(400).json({ error: 'يرجى اختيار السائق' });

  try {
    // Verify order belongs to customer (or user is ADMIN)
    const orderCheck = await pool.query('SELECT * FROM orders WHERE id = $1', [req.params.id]);
    if (orderCheck.rowCount === 0) return res.status(404).json({ error: 'الطلب غير موجود' });
    const order = orderCheck.rows[0];

    if (req.user.role !== 'ADMIN' && order.customer_id !== req.user.id) {
      return res.status(403).json({ error: 'غير مصرح لك بتعديل هذا الطلب' });
    }

    if (!['NEW', 'CONFIRMED'].includes(order.status)) {
      return res.status(400).json({ error: 'لا يمكن تغيير السائق بعد انطلاق التوصيل' });
    }

    // Verify driver exists and is DRIVER
    const driverCheck = await pool.query('SELECT id, full_name, phone FROM users WHERE id = $1 AND role = $2', [driver_id, 'DRIVER']);
    if (driverCheck.rowCount === 0) return res.status(404).json({ error: 'السائق المختار غير متوفر' });

    const orderRes = await pool.query(
      `UPDATE orders 
       SET driver_id = $1, status = 'CONFIRMED', updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [driver_id, req.params.id]
    );

    const updated = orderRes.rows[0];
    broadcastOrderStatus(updated);

    res.json({ success: true, order: updated, message: 'تم تعيين السائق للطلب بنجاح' });
  } catch (err) {
    console.error('Error selecting driver:', err);
    res.status(500).json({ error: 'خطأ في تعيين السائق' });
  }
});

// Assign driver (Admin only)
router.post('/:id/assign-driver', authMiddleware, requireRole('ADMIN'), async (req, res) => {
  const { driver_id } = req.body;
  if (!driver_id) return res.status(400).json({ error: 'معرف السائق مطلوب' });

  try {
    const driverCheck = await pool.query('SELECT id, full_name, phone FROM users WHERE id = $1 AND role = $2', [driver_id, 'DRIVER']);
    if (driverCheck.rowCount === 0) return res.status(404).json({ error: 'السائق غير موجود' });

    const orderRes = await pool.query(
      `UPDATE orders 
       SET driver_id = $1, status = CASE WHEN status = 'NEW' THEN 'CONFIRMED' ELSE status END, updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [driver_id, req.params.id]
    );

    if (orderRes.rowCount === 0) return res.status(404).json({ error: 'الطلب غير موجود' });
    const order = orderRes.rows[0];

    broadcastOrderStatus(order);
    res.json({ success: true, order });
  } catch (err) {
    console.error('Error assigning driver:', err);
    res.status(500).json({ error: 'خطأ في تعيين السائق' });
  }
});

// Update order status (Shop, Driver, Admin)
router.patch('/:id/status', authMiddleware, async (req, res) => {
  const { status } = req.body;
  const allowed = ['NEW', 'CONFIRMED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  if (!status || !allowed.includes(status)) {
    return res.status(400).json({ error: 'حالة الطلب غير صالحة' });
  }

  try {
    const orderRes = await pool.query(
      'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
      [status, req.params.id]
    );

    if (orderRes.rowCount === 0) return res.status(404).json({ error: 'الطلب غير موجود' });
    const order = orderRes.rows[0];

    broadcastOrderStatus(order);
    res.json({ success: true, order });
  } catch (err) {
    console.error('Error updating order status:', err);
    res.status(500).json({ error: 'خطأ في تحديث حالة الطلب' });
  }
});

module.exports = router;
