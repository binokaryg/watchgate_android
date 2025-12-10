# Watchgate
This app monitors the vitals and balance of Android phones with Nepal Telecom SIM cards and reports them to a self-hosted backend server. The app is designed to be configurable for use with other networks and countries.

**Note**: This app has been migrated from MongoDB Atlas Device SDKs (Realm/Stitch) to a self-hosted backend solution due to the deprecation of Atlas Device SDKs (September 30, 2025).

Please follow the following steps to set up the app:

1. [Self-Hosted Backend Setup](#self-hosted-backend-setup)
2. [Configuring the App](#configuring-the-app)

## Self-Hosted Backend Setup

The app now requires a self-hosted backend API endpoint to receive status updates. See [backend/README.md](backend/README.md) for instructions on setting up the backend server.

Your backend should expose an HTTP POST endpoint that accepts JSON data with the following structure:

```json
{
  "date": "2025-12-10T20:43:49.368Z",
  "lastSMSInDate": "2025-12-10T18:30:00.000Z",
  "id": "my-phone-instance",
  "balanceDate": "2025-12-10T20:00:00.000Z",
  "balance": 250,
  "battery": 85,
  "temp": 32,
  "wifi": "MyWiFi",
  "plugged": false,
  "data": true,
  "wifiStrength": -45,
  "carrier": "Nepal Telecom",
  "remainingSMS": 100,
  "smsPackInfoDate": "2025-12-10T19:00:00.000Z"
}
```

## Configuring the App
### i. Build
- Open the project in Android Studio.
- Build and generate APK. The minimum supported API level is 21 (Android 5.0 Lollipop).

### ii. Install APK
Install the generated APK on the phone that you want to monitor (should be Android 5.0 Lollipop or above).

### iii. App Settings
- Open the app and allow permissions.
- Open settings from the menu of the app

#### General Settings
- Instance ID: Enter a name that you want to identify your phone with. It will act as an ID of your phone instance. Recommended to not include space or special characters. 
- Mobile Number: Optional, useless for now.
- SIM Type: Nepal telecom has two types of GSM networks. Switch to postpaid if you have postpaid SIM.

#### Interval Settings
- Balance Query Interval (minutes, default: 240): How often you want to query the balance from the network provider. Minimum is 15 minutes.
- Balance Query Minimum Interval (minutes, default: 10): The app will not allow any balance query request to the network provider until these many minutes have passed after the previous request. We don't want to overwhelm the network provider with lots of balance query requests in a short time.
- Reporting Interval (minutes, default: 30): How often you want to report your stats to the server. Minimum is 15 minutes.
- Periodic Reporting Minimum Interval (minutes, default: 10): The app will not allow scheduled update requests to the server until these many minutes have passed after the previous request. Sometimes, if the phone has been offline for a long time, many periodic (scheduled) update requests may start one after another as soon as the phone gets online. We keep this interval so that the server is not overwhelmed.
- One Time Reporting Minimum Interval (minutes, default: 3): The app will not allow any update request to the server until these many minutes have passed after the previous request. One time reporting happens when user specifically updates (using button with refresh icon), or new balance info is available.
- One Time Reporting Initial Wait Time (seconds, default: 180): The app will wait for the specified seconds before starting the task of one time reporting messages under certain conditions, such as SMS pack check is enabled and a new message is identified as a balance information or an SMS pack information (determined using regex check, see [Advanced](#advanced)).

#### Balance Query Settings
- Query SMS Destination: The number (usually shortcode), where you want to send balance query SMS to.
- Balance Info SMS Source: The number (usually shortcode), from where you get the balance information.
- Balance Query Message (Prepaid): The message to be sent to query SMS for prepaid mobiles. (e.g. BL)
- Balance Query Message (Postpaid): The message to be sent to query SMS for postpaid mobiles. (e.g. CB)

#### SMS Pack Settings
- SMS Packs: Enable if you want to check and report the status of SMS packs.
- Query SMS Destination: The number (usually shortcode), where you want to send SMS pack query SMS to.
- SMS Info SMS Source: The number (usually shortcode), from where you get the SMS pack information.
- SMS Query Message: The message to be sent to query SMS pack for prepaid mobiles. (e.g. SMS300)

#### Advanced
- Prepaid Balance Regex: Regex for balance amount to be matched against the balance message on a prepaid plan.
- Postpaid Balance Due Regex: Regex for balance due amount to be matched against the balance message on a postpaid plan.
- Postpaid Balance Credit Regex: Regex for balance credit available amount to be matched against the balance message on a postpaid plan.
- Prepaid Balance Top Up Message Regex: Regex for new balance amount as specified in the top up success message on a prepaid plan.
- Postpaid Balance Top Up Message Regex: Regex for identifying that topup was successful (without new balance information), on a postpaid plan.
- SMS Pack Info Regex: Regex for number of SMS available in an SMS pack info message.
- SMS Pack Null Regex: Regex for identifying that there is no SMS pack plan active or available.
- SMS Pack Activated Message Regex: Regex for identifying that SMS pack subscription was successful.
- **Backend API URL**: The URL of your self-hosted backend endpoint (e.g., `https://your-server.com/api/status`)
- **API Key (Optional)**: Optional API key for authentication with your backend server

### iv. Start Reporting
Once you have completed the above configuration, you can start scheduled reporting.
- First, test if everything works by tapping the refresh button. The app will send a message to the network provider. Allow any requests from the system to send premium SMS. If everything is set up correctly, you should receive an SMS with balance information after some time. A toast message should appear, and the new balance should be reflected in the UI. Shortly, the app will also try to update your stats to the server.
- If everything works as expected, start the scheduler by tapping on the START button.
- You can see the status of your scheduled tasks by clicking on the INFO button. If you want to stop the future scheduled tasks, tap the STOP button.
- Please ensure that the app is whitelisted as a protected app, so that it is not killed by the system. This setting differs from phone to phone.

## Migration Notes

If you were using the old MongoDB Atlas Device SDK version:
- The old Realm API Key and Stitch Update Function settings have been removed
- Configure the new Backend API URL and optional API Key in the Advanced settings
- The data format remains the same, but dates are now sent as ISO 8601 strings instead of BSON dates
- Firebase Cloud Messaging for remote commands is still supported and unchanged
