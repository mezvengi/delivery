const { initializeApp, getApps, cert } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const fs = require('fs');
const path = require('path');

let firebaseApp = null;
let firebaseAuth = null;

function initFirebase() {
  const apps = getApps();
  if (apps.length > 0) {
    firebaseApp = apps[0];
    firebaseAuth = getAuth(firebaseApp);
    return { app: firebaseApp, auth: firebaseAuth };
  }

  const possiblePaths = [
    process.env.GOOGLE_APPLICATION_CREDENTIALS,
    '/app/serviceAccount.json',
    path.join(__dirname, '../../backend/serviceAccount.json'),
    path.join(__dirname, '../serviceAccount.json')
  ].filter(Boolean);

  let keyPath = null;
  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      keyPath = p;
      break;
    }
  }

  try {
    if (keyPath) {
      firebaseApp = initializeApp({
        credential: cert(keyPath)
      });
      console.log(`🔥 [Firebase Admin] Initialized successfully with credentials at: ${keyPath}`);
    } else {
      firebaseApp = initializeApp();
      console.log('🔥 [Firebase Admin] Initialized with application default credentials');
    }
    firebaseAuth = getAuth(firebaseApp);
  } catch (err) {
    console.error('⚠️ [Firebase Admin] Initialization error:', err.message);
  }

  return { app: firebaseApp, auth: firebaseAuth };
}

initFirebase();

module.exports = {
  getAuth: () => firebaseAuth || initFirebase().auth,
  initFirebase
};
