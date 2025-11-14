// src/routes/matchRoutes.js

const express = require('express');
const router = express.Router();
const Match = require('../models/Match');

// POST : créer un match
router.post('/', async (req, res) => {
    try {
        const match = new Match(req.body);
        await match.save();
        res.status(201).json({ message: "Match ajouté avec succès", match });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});


// GET : récupérer tous les matchs
router.get('/', async (req, res) => {
    try {
        const matches = await Match.find().sort({ date: -1 });
        res.json(matches);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
