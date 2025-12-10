# Self-Hosted Backend Setup

This app requires a self-hosted backend server to store and manage phone status data. The backend replaces the deprecated MongoDB Atlas Device SDKs (Realm/Stitch) that were previously used.

## Requirements

Your backend needs to provide an HTTP POST endpoint that accepts JSON data from the Android app.

## API Specification

### Endpoint

Your backend should expose an endpoint (e.g., `https://your-server.com/api/status`) that accepts HTTP POST requests.

### Request Format

The app will send HTTP POST requests with the following JSON structure:

```json
{
  "date": "Mon Dec 10 2025 20:43:49 GMT+0000",
  "lastSMSInDate": "Mon Dec 10 2025 18:30:00 GMT+0000",
  "id": "my-phone-instance",
  "balanceDate": "Mon Dec 10 2025 20:00:00 GMT+0000",
  "balance": 250,
  "battery": 85,
  "temp": 32,
  "wifi": "MyWiFi",
  "plugged": false,
  "data": true,
  "wifiStrength": -45,
  "carrier": "Nepal Telecom",
  "remainingSMS": 100,
  "smsPackInfoDate": "Mon Dec 10 2025 19:00:00 GMT+0000"
}
```

### Request Headers

- `Content-Type: application/json`
- `Authorization: Bearer <api-key>` (if API key is configured in the app)

### Field Descriptions

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `date` | String | Current timestamp | Yes |
| `lastSMSInDate` | String | Timestamp of last SMS received | Yes |
| `id` | String | Phone instance identifier | Yes |
| `balanceDate` | String | Timestamp when balance was queried | Conditional* |
| `balance` | Integer | Prepaid balance amount | Conditional* |
| `balanceDue` | Integer | Postpaid balance due | Conditional* |
| `balanceCredit` | Integer | Postpaid credit available | Conditional* |
| `battery` | Integer | Battery percentage (0-100) | Yes |
| `temp` | Integer | Battery temperature (Celsius) | Yes |
| `wifi` | String | WiFi SSID or "N/A" | Yes |
| `plugged` | Boolean | Charging status | Yes |
| `data` | Boolean | Mobile data enabled | Yes |
| `wifiStrength` | Integer | WiFi signal strength (dBm) | Yes |
| `carrier` | String | Mobile carrier name | Yes |
| `remainingSMS` | Integer | Remaining SMS in pack | Optional |
| `smsPackInfoDate` | String | SMS pack info timestamp | Optional |

*Note: For prepaid SIMs, `balance` is sent. For postpaid SIMs, `balanceDue` and `balanceCredit` are sent.

### Response

Your backend should return HTTP 200 OK on success with any valid JSON response.

Example:
```json
{
  "success": true,
  "message": "Status updated successfully"
}
```

## Implementation Examples

See the [examples](examples/) directory for complete backend implementations in various languages and frameworks.

## Security Considerations

1. **Use HTTPS**: Always use HTTPS in production
2. **API Key Authentication**: Configure an API key in both app and backend
3. **Rate Limiting**: Implement rate limiting to prevent abuse
4. **Input Validation**: Validate all incoming data

## Deployment Options

- VPS/Dedicated Server with Node.js/Python/PHP
- Cloud Platforms: AWS, Google Cloud, Azure, DigitalOcean
- Docker containers
- Serverless: AWS Lambda, Google Cloud Functions

## Migration from MongoDB Atlas

If you previously used MongoDB Atlas Device SDKs, your data structure is compatible. The main changes are:
- Date fields are now strings instead of BSON dates
- Direct HTTP POST instead of Realm SDK function calls
- Authentication via Bearer token instead of Realm API keys
