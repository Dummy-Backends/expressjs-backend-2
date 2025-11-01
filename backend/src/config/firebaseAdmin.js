// backend/src/config/firebaseAdmin.js

const admin = require('firebase-admin');
const serviceAccount = require('../../serviceAccountKey.json'); // NOTE: See Setup Instruction below

try {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log('Firebase Admin SDK initialized successfully.');
} catch (error) {
    if (!admin.apps.length) {
        console.error("Error initializing Firebase Admin SDK:", error);
    }
}

module.exports = admin;
