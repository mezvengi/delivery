const express = require('express');
const router = express.Router();

router.get('/active', (req, res) => {
  res.json({
    zone_id: process.env.DEFAULT_ZONE_ID || 'sour_el_ghozlane',
    name_ar: 'بلدية سور الغزلان (ولاية البويرة)',
    center: {
      lat: parseFloat(process.env.CENTER_LAT || '36.148000'),
      lng: parseFloat(process.env.CENTER_LNG || '3.690000')
    },
    geofence_radius_km: parseFloat(process.env.GEOFENCE_RADIUS_KM || '8.0'),
    currency: process.env.CURRENCY || 'DA',
    fixed_delivery_fee_da: parseFloat(process.env.FIXED_DELIVERY_FEE || '200'),
    payment_methods: ['COD'],
    neighborhoods: [
      'وسط المدينة',
      'حي الوئام',
      'حي 114 مسكن',
      'حي ذراع البرج',
      'حي عين مريم',
      'حي الرامي',
      'حي باب الجزائر',
      'حي باب البوسعادة',
      'المنطقة الصناعية',
      'حي النصر'
    ]
  });
});

module.exports = router;
