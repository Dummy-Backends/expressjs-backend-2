// backend/src/middleware/authMiddleware.js

const admin = require('../config/firebaseAdmin');

/**
 * Middleware to verify Firebase ID Token from the 'Authorization' header.
 * Attaches the decoded Firebase UID to the request object (req.user.id).
 */
const protect = async (req, res, next) => {
    let idToken;

    // 1. Check for token in the 'Authorization: Bearer <token>' header
    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
        try {
            idToken = req.headers.authorization.split(' ')[1];

            // 2. Verify the token using Firebase Admin SDK
            const decodedToken = await admin.auth().verifyIdToken(idToken);
            
            // 3. Attach the Firebase UID to the request object
            req.user = { id: decodedToken.uid }; 

            next();
        } catch (error) {
            console.error("Token verification failed:", error.message);
            // Deny access if token is invalid or expired
            return res.status(401).json({ message: 'Not authorized, token failed' });
        }
    }

    if (!idToken) {
        return res.status(401).json({ message: 'Not authorized, no token' });
    }
};

module.exports = protect;
