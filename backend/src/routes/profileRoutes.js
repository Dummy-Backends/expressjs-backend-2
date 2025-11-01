// backend/src/routes/profileRoutes.js

const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const protect = require('../middleware/authMiddleware');
const Profile = require('../models/Profile');
const admin = require('../config/firebaseAdmin'); // Used for first-time profile check

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, '../../uploads/profile_pictures');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer for file uploads
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
        // Use userId from the authenticated user + timestamp + extension
        const userId = req.user?.id || 'unknown';
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, `${userId}-${uniqueSuffix}${ext}`);
    }
});

const fileFilter = (req, file, cb) => {
    // Accept only image files
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('Only image files are allowed!'), false);
    }
};

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

// --- Utility function to calculate Win Rate ---
const calculateWinRate = (wins, losses) => {
    if (wins + losses === 0) return 0;
    return (wins / (wins + losses)) * 100;
};

// @route   GET /api/profile
// @desc    Get the current authenticated user's profile
// @access  Private (Requires JWT)
router.get('/', protect, async (req, res) => {
    try {
        const profile = await Profile.findOne({ userId: req.user.id });

        if (!profile) {
            // If profile doesn't exist, create an initial one
            const newProfile = await Profile.create({ userId: req.user.id });
            return res.status(201).json(newProfile);
        }

        res.json(profile);
    } catch (error) {
        res.status(500).json({ message: 'Server error retrieving profile.' });
    }
});

// @route   PUT /api/profile/customize
// @desc    Update user nickname or profile picture URL
// @access  Private (Requires JWT)
router.put('/customize', protect, async (req, res) => {
    const { nickname, profile_picture_url } = req.body;
    try {
        const updateFields = {};
        if (nickname) updateFields.nickname = nickname;
        if (profile_picture_url) updateFields.profile_picture_url = profile_picture_url;

        const profile = await Profile.findOneAndUpdate(
            { userId: req.user.id },
            { $set: updateFields },
            { new: true, runValidators: true }
        );

        if (!profile) return res.status(404).json({ message: 'Profile not found.' });

        res.json(profile);
    } catch (error) {
        // Handle MongoDB unique key error (e.g., nickname already taken)
        if (error.code === 11000) { 
            return res.status(400).json({ message: 'Nickname is already taken.' });
        }
        res.status(500).json({ message: 'Server error during customization update.' });
    }
});

// @route   POST /api/profile/upload-image
// @desc    Upload profile picture image file
// @access  Private (Requires JWT)
router.post('/upload-image', protect, upload.single('image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ message: 'No image file provided.' });
        }

        // Get current profile to check for old image
        const profile = await Profile.findOne({ userId: req.user.id });
        
        // If user has an old profile picture, delete it
        if (profile && profile.profile_picture_url) {
            try {
                const oldUrl = profile.profile_picture_url;
                
                // Skip deletion if it's a placeholder or external URL
                if (oldUrl.includes('placehold.co') || oldUrl.includes('http') && !oldUrl.includes(req.user.id)) {
                    // This is likely an external URL or placeholder, skip deletion
                } else {
                    // Extract filename from the URL
                    // URL format: http://host:port/uploads/filename
                    // or just /uploads/filename
                    let oldFilename = null;
                    
                    // Try to extract filename from URL path
                    const urlMatch = oldUrl.match(/\/([^\/\?]+)(?:\?|$)/);
                    if (urlMatch && urlMatch[1]) {
                        // Extract the last part after the last slash
                        const parts = oldUrl.split('/');
                        const lastPart = parts[parts.length - 1];
                        // Remove query parameters if any
                        oldFilename = lastPart.split('?')[0];
                    }
                    
                    // Also check if filename starts with userId (our naming convention)
                    if (oldFilename && oldFilename.startsWith(req.user.id)) {
                        const oldFilePath = path.join(uploadsDir, oldFilename);
                        
                        if (fs.existsSync(oldFilePath)) {
                            fs.unlinkSync(oldFilePath);
                            console.log(`Deleted old profile picture: ${oldFilename}`);
                        }
                    }
                }
            } catch (deleteError) {
                // Log error but don't fail the upload
                console.error('Error deleting old profile picture:', deleteError);
            }
        }

        // Construct the URL for the uploaded image
        const baseUrl = process.env.BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
        const imageUrl = `${baseUrl}/uploads/${req.file.filename}`;

        res.json({
            success: true,
            imageUrl: imageUrl,
            filename: req.file.filename
        });
    } catch (error) {
        console.error('Error uploading image:', error);
        res.status(500).json({ message: 'Server error uploading image.' });
    }
});

// @route   DELETE /api/profile/delete-image
// @desc    Delete a profile picture by filename
// @access  Private (Requires JWT)
router.delete('/delete-image', protect, async (req, res) => {
    try {
        const { filename } = req.body;
        
        if (!filename) {
            return res.status(400).json({ message: 'Filename is required.' });
        }

        // Security: Ensure the filename doesn't contain path traversal
        if (filename.includes('..') || filename.includes('/') || filename.includes('\\')) {
            return res.status(400).json({ message: 'Invalid filename.' });
        }

        // Get user's profile to verify they own this image
        const profile = await Profile.findOne({ userId: req.user.id });
        if (!profile) {
            return res.status(404).json({ message: 'Profile not found.' });
        }

        // Check if this image belongs to the user (by checking if filename starts with userId)
        if (!filename.startsWith(req.user.id)) {
            return res.status(403).json({ message: 'You can only delete your own images.' });
        }

        const filePath = path.join(uploadsDir, filename);
        
        if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
            res.json({ success: true, message: 'Image deleted successfully.' });
        } else {
            res.status(404).json({ message: 'Image file not found.' });
        }
    } catch (error) {
        console.error('Error deleting image:', error);
        res.status(500).json({ message: 'Server error deleting image.' });
    }
});

// @route   POST /api/profile/game_result
// @desc    Update score, wins, losses, winrate, and history after a game
// @access  Private (Requires JWT)
router.post('/game_result', protect, async (req, res) => {
    const { didWin, scoreChange, opponentId } = req.body; 

    // Basic Validation
    if (typeof didWin !== 'boolean' || !scoreChange || !opponentId) {
        return res.status(400).json({ message: 'Missing required game result fields.' });
    }

    try {
        const profile = await Profile.findOne({ userId: req.user.id });
        if (!profile) return res.status(404).json({ message: 'Profile not found.' });

        // 1. Update Core Stats
        let newWins = profile.wins;
        let newLosses = profile.losses;
        
        if (didWin) {
            newWins += 1;
        } else {
            newLosses += 1;
        }

        // 2. Calculate New Win Rate
        const newWinRate = calculateWinRate(newWins, newLosses);

        // 3. Prepare Updates
        const updates = {
            $inc: { score: scoreChange }, // $inc increments the value
            wins: newWins,
            losses: newLosses,
            winrate: newWinRate,
            // $push adds a new item to the history_of_games array
            $push: {
                history_of_games: {
                    opponentId,
                    result: didWin ? 'win' : 'loss', 
                    scoreChange 
                }
            }
        };

        const updatedProfile = await Profile.findOneAndUpdate(
            { userId: req.user.id },
            updates,
            { new: true }
        );

        res.json(updatedProfile);

    } catch (error) {
        res.status(500).json({ message: 'Server error processing game result.' });
    }
});

module.exports = router;
