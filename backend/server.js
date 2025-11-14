// backend/server.js

const express = require('express');
const cors = require('cors');
const path = require('path');
const connectDB = require('./src/config/db');
require('dotenv').config({ path: './.env' }); // Load secrets

// Import Routes
const profileRoutes = require('./src/routes/profileRoutes');
const matchRoutes = require('./src/routes/matchRoutes'); // <-- AJOUT

// Initialize MongoDB Connection
connectDB();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors()); // Allows cross-origin requests (necessary for mobile app)
app.use(express.json()); // Allows parsing of JSON request bodies

// Serve uploaded images statically
app.use('/uploads', express.static(path.join(__dirname, 'uploads/profile_pictures')));

// Route Mounting
app.use('/api/profile', profileRoutes);
app.use('/api/matches', matchRoutes);   // <-- AJOUT

// Simple root route
app.get('/', (req, res) => {
    res.send('ChaseGo Backend API is running.');
});

// Start the server
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
