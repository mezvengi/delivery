const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || path.join(__dirname, 'serviceAccount.json');
let isInitialized = false;

if (fs.existsSync(serviceAccountPath)) {
  try {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    isInitialized = true;
    console.log('✅ [Firebase Admin]: Initialized successfully with serviceAccount.json');
  } catch (error) {
    console.error('❌ [Firebase Admin Error]: Failed to initialize with serviceAccount.json:', error.message);
  }
} else {
  console.warn(`⚠️ [Firebase Admin Warning]: serviceAccount.json not found at ${serviceAccountPath}. Real ID token verification will require this file on your server.`);
}

module.exports = {
  admin,
  isInitialized: () => isInitialized
};
