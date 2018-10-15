## MongoDB Stitch App Setup
This app relies on MongoDB Stitch application to periodically report the collected data of the phone to a remote MongoDB collection.

To set up a MongoDB Stitch app, follow this guide: https://docs.mongodb.com/stitch/procedures/create-stitch-app/
 
After creating the app, go to the MongoDB Stitch console of your app.

Note the Application ID of your Stitch App which is displayed when you click `Settings` on the left panel.
 
Open `Rules` from the left panel and add collection with the following information:
 - Database Name: `watchgate`
 - Collection Name: `status`
  
Create a role for the collection just created, and allow insertion (Check on 'Insert Documents').

Open `Functions` from the left panel and create a new function using the name and content of js file inside [functions](https://github.com/binokaryg/watchgate_android/tree/master/mongodb-stitch/functions) directory.

Go to `Users` on the left panel, click on Providers and allow anonymous access by enabling "Allow users to log in anonymously".
 
Note: If you don't want to enable anonymous to the Stitch app, you may have to implement some kind of user authentication in the Stitch app and also in the Android app.
