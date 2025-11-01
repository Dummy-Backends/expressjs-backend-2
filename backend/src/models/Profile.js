// backend/src/models/Profile.js

const mongoose = require('mongoose');

const GameHistorySchema = new mongoose.Schema({
    opponentId: { type: String, required: true },
    result: { type: String, enum: ['win', 'loss', 'draw'], required: true },
    scoreChange: { type: Number, default: 0 },
    timestamp: { type: Date, default: Date.now }
});

const ProfileSchema = new mongoose.Schema({
    // CRITICAL: This is the UNIQUE Firebase User ID (UID)
    userId: {
        type: String,
        required: true,
        unique: true
    },
    nickname: {
        type: String,
        required: true,
        unique: true,
        default: 'NewPlayer'
    },
    profile_picture_url: {
        type: String,
        default: 'https://placehold.co/150x150/cccccc/333333/png?text=Avatar' 
    },
    score: {
        type: Number,
        default: 100 // Starting ELO/Score
    },
    wins: {
        type: Number,
        default: 0
    },
    losses: {
        type: Number,
        default: 0
    },
    winrate: {
        type: Number,
        default: 0.0
    },
    history_of_games: [GameHistorySchema]
}, { timestamps: true });

module.exports = mongoose.model('Profile', ProfileSchema);
