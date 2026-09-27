#!/bin/bash
set -e

echo "=========================================="
echo "🚀 تحديث منصة SGdelivery لسور الغزلان"
echo "=========================================="

echo "📥 1. جاري سحب أحدث التعديلات من GitHub..."
git pull origin main

echo "🔄 2. إعادة تشغيل خدمات Caddy و Backend..."
docker compose restart backend caddy

echo "=========================================="
echo "✅ تم تحديث السيرفر بنجاح!"
echo "🌐 تفقد الرابط: https://sour.serveirc.com/?role=admin"
echo "=========================================="
