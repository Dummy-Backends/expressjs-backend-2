// backend/server.js

const express = require('express');
const cors = require('cors');
const connectDB = require('./src/config/db');
require('dotenv').config({ path: './.env' }); // Load secrets

// Import Routes
const profileRoutes = require('./src/routes/profileRoutes');

// Initialize MongoDB Connection
connectDB();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors()); // Allows cross-origin requests (necessary for mobile app)
app.use(express.json()); // Allows parsing of JSON request bodies

// Route Mounting
app.use('/api/profile', profileRoutes);

// Simple root route
app.get('/', (req, res) => {
    res.send('ChaseGo Backend API is running.');
});

// Start the server
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
