const express = require('express');
const { MongoClient } = require('mongodb');

const app = express();
app.use(express.json());

// Configuration from environment variables
const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/watchgate';
const API_KEY = process.env.API_KEY; // Optional

let client;
let db;

// Connect to MongoDB
async function connectDB() {
  try {
    client = new MongoClient(MONGODB_URI);
    await client.connect();
    db = client.db();
    console.log('Connected to MongoDB');
  } catch (error) {
    console.error('MongoDB connection error:', error);
    process.exit(1);
  }
}

// Middleware to verify API key if configured
function verifyApiKey(req, res, next) {
  if (!API_KEY) {
    return next(); // No API key configured, skip verification
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing or invalid authorization header' });
  }

  const token = authHeader.substring(7);
  if (token !== API_KEY) {
    return res.status(401).json({ error: 'Invalid API key' });
  }

  next();
}

// Status update endpoint
app.post('/api/status', verifyApiKey, async (req, res) => {
  try {
    const statusData = req.body;

    // Basic validation
    if (!statusData.id) {
      return res.status(400).json({ error: 'Missing required field: id' });
    }

    // Add server-side timestamp
    const document = {
      ...statusData,
      receivedAt: new Date()
    };

    // Insert into MongoDB
    const collection = db.collection('status');
    const result = await collection.insertOne(document);

    console.log(`Status update received from instance: ${statusData.id}`);

    res.json({
      success: true,
      message: `Inserted document with _id: ${result.insertedId}`,
      id: statusData.id,
      date: statusData.date
    });
  } catch (error) {
    console.error('Error processing status update:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date() });
});

// Start server
connectDB().then(() => {
  app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    console.log(`API Key authentication: ${API_KEY ? 'enabled' : 'disabled'}`);
  });
});

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, closing server...');
  if (client) {
    await client.close();
  }
  process.exit(0);
});
