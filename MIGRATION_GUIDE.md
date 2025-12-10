# Migration Guide: From MongoDB Atlas Device SDKs to Self-Hosted Backend

This guide helps existing users migrate from the deprecated MongoDB Atlas Device SDKs (Realm/Stitch) to the new self-hosted backend solution.

## What Changed?

### Before (Deprecated)
- Used MongoDB Realm SDK for authentication
- Called Atlas Functions via Realm SDK
- Configured with Realm API keys
- Data stored directly in MongoDB Atlas via Realm sync

### After (Current)
- Direct HTTP POST requests to your backend
- Standard REST API with optional Bearer token authentication
- Configured with backend URL and API key
- You control the backend and database

## Migration Steps

### Step 1: Update the Android App

1. **Download the latest APK** or build from the updated source code
2. **Install** the new version (it will replace the old one)
3. **Configure the new settings** in the app:
   - Open Settings → Advanced
   - Set **Backend API URL**: Your server endpoint (e.g., `https://your-server.com/api/status`)
   - Set **API Key** (Optional): Your authentication token

### Step 2: Set Up Your Backend

Choose one of these options:

#### Option A: Use the Provided Node.js Backend

1. Deploy the example from `backend/examples/nodejs/`
2. Configure MongoDB connection
3. Set an API key for authentication
4. Ensure HTTPS is enabled

See [backend/examples/nodejs/README.md](backend/examples/nodejs/README.md) for detailed instructions.

#### Option B: Create Your Own Backend

Implement an HTTP POST endpoint that:
- Accepts JSON payloads as specified in [backend/README.md](backend/README.md)
- Returns HTTP 200 on success
- Optionally validates Bearer token authentication

### Step 3: Configure MongoDB (If Continuing to Use It)

If you're using MongoDB:

1. Keep your existing database and collection (`watchgate.status`)
2. The data structure is compatible - dates are now strings instead of BSON dates
3. Update any queries that rely on BSON date types

Or switch to any other database (PostgreSQL, MySQL, etc.)

### Step 4: Test the Migration

1. In the Android app, tap the refresh button
2. Check that data appears in your backend
3. Monitor logs for any errors
4. Verify scheduled reporting works

## Configuration Comparison

### Old Configuration (Deprecated)
- MongoDB Stitch App ID
- Realm API Key
- Stitch Update Function Name

### New Configuration
- Backend API URL
- API Key (Optional)

## Data Format Changes

The JSON payload structure remains the same, with one change:

**Dates**: Now sent as strings using Java's `Date.toString()` format
```
Before: { "date": ISODate("2025-12-10T20:43:49.368Z") }
After:  { "date": "Mon Dec 10 2025 20:43:49 GMT+0000" }
```

## Rollback Plan

If you need to rollback:

1. Reinstall the previous APK version
2. Restore the old configuration (Realm API key, etc.)
3. Note: You cannot roll back after September 30, 2025 when Atlas Device SDKs are deprecated

## Troubleshooting

### App says "Backend URL not configured"
→ Go to Settings → Advanced → Backend API URL and enter your server URL

### Network requests failing
→ Check that:
- Backend URL is correct and accessible
- Backend is running
- HTTPS certificate is valid
- API key matches (if configured)

### Data not appearing in database
→ Check backend logs for errors

### Firebase Cloud Messaging not working
→ FCM configuration is unchanged - check your `google-services.json` file

## Support

For issues related to:
- **Android app**: Create an issue on GitHub
- **Backend implementation**: Check backend/README.md
- **MongoDB**: Refer to MongoDB documentation

## Timeline

- **Now**: Migration recommended
- **September 30, 2025**: MongoDB Atlas Device SDKs stop working
- **After Sep 30, 2025**: Must use new self-hosted solution

## Benefits of Migration

✅ No dependency on deprecated services
✅ Full control over your infrastructure  
✅ Flexible deployment options
✅ No vendor lock-in
✅ Can use any database
✅ Better for long-term maintenance
