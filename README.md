# Watchgate
This app was originally designed to work with MongoDB Stitch to report the vitals and balance of Android phones with Nepal Telecom sim card. The aim is to make it fully configurable so that it can be usable in other networks and countries.

Please follow the following steps to set up the app:

1. [MongoDB Stitch App Setup](mongodb-stitch/README.md)
2. [Configuring the App](#configuring-the-app)

## Configuring the App
### i. Build
- Open the project in Android Studio.
- In the file [app/src/main/res/values/mongodb-stitch.xml](app/src/main/res/values/mongodb-stitch.xml), replace `yourappid` with your MongoDB Stitch App ID you should have from [step 1](mongodb-stitch/README.md).
Build and generate apk. The minimum supported API level is 21 (Android 5.0 Lollipop).

### ii. Install APK
Install the generated apk in the phone that you want to monitor (should be Android 5.0 Lollipop or above).

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

#### Query Settings
- Query SMS Destination: The number (usually shortcode), where you want to send balance query SMS to.
- Balance Info SMS Source: The number (usually shortcode), from where you get the balance information.
- Balance Query Message (Prepaid): The message to be sent to query SMS for prepaid mobiles.
- Balance Query Message (Postpaid): The message to be sent to query SMS for postpaid mobiles.

#### Advanced
- Prepaid Balance Regex: Regex for balance amount to be matched against the balance message on a prepaid system.
- Postpaid Balance Due Regex: Regex for balance due amount to be matched against the balance message on a postpaid system.
- Postpaid Balance Credit Regex: Regex for balance credit available amount to be matched against the balance message on a postpaid system.
- Stitch Update Function: In case you have named your stitch app function name differently, update it here.

### iv. Start Reporting
Once you have completed the above configuration, you can start scheduled reporting.
- First, test if everything works by tapping the refresh button. The app will send message to the network provider. Allow any requests from the system to send premium SMS. If everything is set up correctly, you should receive SMS with balance information after some time. A toast message should appear, and the new balance should be reflected in the UI. Shortly, the app will also try to update your stats to the server.
- If everything works as expected, start the scheduler by tapping on START button.
- You can see the status of your scheduled tasks by clicking on INFO button. If you want to stop the future scheduled tasks, tap STOP button.
- Please ensure that the app is whitelisted as a protected app, so that it is not killed by the system. This setting differs from phone to phone.
