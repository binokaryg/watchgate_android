# Node.js Express Backend for Watchgate

This is a simple self-hosted backend implementation for the Watchgate Android app using Node.js, Express, and MongoDB.

## Requirements

- Node.js 14 or higher
- MongoDB 4.4 or higher

## Installation

1. Install dependencies:
```bash
npm install
```

2. Set up environment variables (create a `.env` file or export them):
```bash
export MONGODB_URI="mongodb://localhost:27017/watchgate"
export API_KEY="your-secret-api-key-here"  # Optional
export PORT=3000  # Optional, defaults to 3000
```

3. Start the server:
```bash
npm start
```

For development with auto-reload:
```bash
npm run dev
```

## Configuration

- `MONGODB_URI`: MongoDB connection string (default: `mongodb://localhost:27017/watchgate`)
- `API_KEY`: Optional API key for authentication. If set, all requests must include `Authorization: Bearer <API_KEY>` header
- `PORT`: Server port (default: 3000)

## API Endpoints

### POST /api/status
Receives phone status updates from the Android app.

**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <API_KEY>` (if API_KEY is configured)

**Request Body:**
```json
{
  "id": "phone-instance-id",
  "date": "Mon Dec 10 2025 20:43:49 GMT+0000",
  "battery": 85,
  "temp": 32,
  "wifi": "MyWiFi",
  "plugged": false,
  "data": true,
  "wifiStrength": -45,
  "carrier": "Nepal Telecom"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Inserted document with _id: ...",
  "id": "phone-instance-id",
  "date": "..."
}
```

### GET /health
Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "timestamp": "2025-12-10T20:43:49.368Z"
}
```

## Docker Deployment

Create a `Dockerfile`:

```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

EXPOSE 3000

CMD ["node", "server.js"]
```

Build and run:
```bash
docker build -t watchgate-backend .
docker run -p 3000:3000 \
  -e MONGODB_URI="mongodb://mongo:27017/watchgate" \
  -e API_KEY="your-secret-key" \
  watchgate-backend
```

## MongoDB Indexes

For better performance, create these indexes:

```javascript
db.status.createIndex({ "id": 1, "date": -1 });
db.status.createIndex({ "receivedAt": -1 });
```

## Security Notes

- Always use HTTPS in production
- Set a strong API_KEY
- Consider implementing rate limiting
- Validate and sanitize input data
- Keep MongoDB connection secure
