// src/models/Match.js


const mongoose = require('mongoose');

const matchSchema = new mongoose.Schema({
    date: { type: Date, required: true },
    role: { type: String, required: true },          // "Chasseur" ou "Proie"
    duree: { type: Number, required: true },         // en minutes, secondes, etc.
    distance: { type: Number, required: true },      // en km ou mètres
    resultat: { type: String, required: true }       // "Victoire du chasseur", "Victoire de la proie"...
}, { timestamps: true });

module.exports = mongoose.model('Match', matchSchema);
