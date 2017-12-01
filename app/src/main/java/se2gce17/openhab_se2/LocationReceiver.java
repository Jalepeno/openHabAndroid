package se2gce17.openhab_se2;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;


import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPollerResult;
import se2gce17.openhab_se2.models.OpenHABConfig;
import se2gce17.openhab_se2.models.OpenHABLocation;
import se2gce17.openhab_se2.models.OpenHABNotification;
import se2gce17.openhab_se2.models.OpenHABUser;

import static android.content.ContentValues.TAG;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * This is the the service that sends the location data from the app to the openHAB database.
 * the database url is specified in the settings tab of app, and saved in the OpenHABConfig stored
 * in the realm database.
 *
 * this services is called through the cwac_locpoll package library, that makes use of a wakeful intent.

 * Created by Nicolaj Pedersen on 23-10-2017.
 */
public class LocationReceiver extends BroadcastReceiver {

    private Location home;
    private String user;
    private String name;
    private static boolean sendNotification = false;
    private static int latestNotificationId=0;


    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.e("LocationReceiver", "intent has been wakened");
        Bundle b = intent.getExtras();

        LocationPollerResult locationResult = new LocationPollerResult(b);
        final Location loc = locationResult.getLocation();
        if (loc == null) {
            Log.e(TAG, "the current location i null");
            return;
        }

        Realm realm = Realm.getDefaultInstance();
        final OpenHABUser openHABUser = realm.where(OpenHABUser.class).findFirst();
        if (openHABUser == null) {
            Log.e(TAG, "the user is null");
            return;
        }

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                RealmResults<OpenHABLocation> results = realm.where(OpenHABLocation.class).findAll();

                for (OpenHABLocation l : results) {
                    if (Utils.calcLocationProximity(loc, l.getLocation(), l.getRadius()) == 1) {
                        openHABUser.setLastLocation(l);
                        // in case of overlapping locations, the first will get picked.
                        // this is to prioritize the home location (index 0)
                        break;
                    }
                }
                if (openHABUser == null) {
                    return;
                }
                if (openHABUser.getLastLocation() == null) {
                    openHABUser.setLastLocation(results.get(0));
                }

                // sending location data to server
                sendLocationDataToWebsite(loc, openHABUser);


                // retrieving notification list
                OpenHABConfig config = realm.where(OpenHABConfig.class).findFirst();
                NotificationsTask task = new NotificationsTask();
                task.setContext(context);
                new NotificationsTask().execute(config.getUrl(),openHABUser.getUser());
            }
        });

     /*
        home = (Location) intent.getExtras().get("location");
        user = intent.getExtras().getString("user");
        name = intent.getExtras().getString("username");
     */


    }


    protected void sendLocationDataToWebsite(Location location, OpenHABUser oHABuser) {
        // ex "1;anders_home" for user with name = anders and is currently within the range of his home

        int proximity = Utils.calcLocationProximity(location, oHABuser.getLastLocation().getLocation(), oHABuser.getLastLocation().getRadius());
        // int proximity = 1;
        Log.d(TAG, "is close to " + oHABuser.getLastLocation().getDbName() + ": " + proximity + "  --  location id:" + oHABuser.getLastLocation().getId());
        if (proximity == -1) { // error check, if current location or last location is null
            return;
        }

        String data = "" + proximity + ";" + oHABuser.getLastLocation().getDbName();
        // String data = ""+proximity+";"+"Home";
        String encrypedData = Utils.encrypt(data);

        NetworkTask task = new NetworkTask();


        //    Integer[] params = new Integer[]{Integer.valueOf(calcLocationProximity(home,location,100))};

        task.execute(oHABuser.getUser(), encrypedData);

    }


    /**
     * NetworkTask is the specified task to send the computed location data to the openHAB server
     * The sever url is found in the openHABConfig, and the user data is found in the uOpenHABUser.
     * The data is retrieved from the database, and the location data is calculated and encrypted
     * before added to the task.
     */
    private class NetworkTask extends AsyncTask<String, Void, Void> {

        private OkHttpClient client;


        @Override
        protected Void doInBackground(final String... strings) {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    OpenHABConfig config = realm.where(OpenHABConfig.class).findFirst();
                    client = new OkHttpClient();
                    final String user = strings[0];
                    final String data = strings[1];
                    // params
                    // 0 = name;
                    // 1 = user;
                    // 2 = data(encrypted)

                    try {
                        Request request = new Request.Builder()
                                .url(config.getUrl() + "user=" + user+  "&data=" + data)
                                .build();

                        Response response = client.newCall(request).execute();

                        //response.body().string();

                        Log.e(TAG, "-- sending location -- " + "user=" + strings[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            return null;
        }
    }


    /**
     * this task is used to get the latest notifications from the database
     * in the postExecution the fetced data is compared with the stored data
     * if any new data will promt a notification.
     */
    private class NotificationsTask extends AsyncTask<String,Void,String>{
        private OkHttpClient client;
        private Context mContext;


        public void setContext(Context context){
            mContext = context;
        }

        @Override
        protected String doInBackground(String... strings) {

            try {
                client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(strings[0]+"getNotification="+strings[1])
                        .build();

                Response response = client.newCall(request).execute();

                return Utils.decrypt(response.body().string());

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String[] notifications = s.split(":");
            OpenHABNotification notification = new OpenHABNotification(notifications[0]);
            OpenHABNotification latestNotification = compareNotifications(notifications);

            if(latestNotification != null && !latestNotification.isRead()) {
                latestNotification.setRead(true);
                // The id of the channel.
                String CHANNEL_ID = "openhab_channel_01";
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(mContext)
                                .setSmallIcon(R.mipmap.smart_home_logo)
                                .setChannel(CHANNEL_ID)
                                .setContentTitle(mContext.getResources().getString(R.string.notification_header))
                                .setContentText(mContext.getResources().getString(R.string.notification_message,notification.getDeviceId(),notification.getFormattedTimestamp()));
                NotificationManager mNotifyMgr =
                        (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
                // Builds the notification and issues it.
                mNotifyMgr.notify(latestNotification.getNotificationId(), mBuilder.build());

            }
        }
    }



    private OpenHABNotification compareNotifications(final String[] notifications) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<OpenHABNotification> storedNotifications = realm.where(OpenHABNotification.class).findAllSorted("device_id");
                OpenHABNotification latestStoredNotification = storedNotifications.get(0);

                for(int loopi = notifications.length-1;loopi>=0;loopi--){ // look at the earliest notification first
                    String[] notifValues = notifications[loopi].split(",");

                    // add values to a new object
                    OpenHABNotification newestNotification = realm.createObject(OpenHABNotification.class);
                    newestNotification.setTimestamp(notifValues[0]);
                    newestNotification.setDeviceId(notifValues[1]);

                    // compare with current notifications
                    if(Utils.compareNotifications(newestNotification,latestStoredNotification)>0){ // already null secure
                        newestNotification.setRead(false);
                        if(latestStoredNotification != null){ // if null the database is empty
                            latestNotificationId = latestStoredNotification.getNotificationId()+1;
                            newestNotification.setNotificationId(latestNotificationId);
                        }else{
                            latestNotificationId = 1;
                            newestNotification.setNotificationId(latestNotificationId); // notifications will be indexed by 1
                        }
                        latestStoredNotification = newestNotification;
                        sendNotification = true;
                    }
                }
                RealmResults<OpenHABNotification> purgeResults = realm.where(OpenHABNotification.class).equalTo("notificationId",0).findAll();
                purgeResults.deleteAllFromRealm();

            }
        });
        if(sendNotification){
            return realm.where(OpenHABNotification.class).equalTo("notificationId",latestNotificationId).findFirst();
        }

        return null;
    }
}
