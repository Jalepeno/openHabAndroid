package se2gce17.openhab_se2;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se2gce17.openhab_se2.models.OpenHABConfig;
import se2gce17.openhab_se2.models.OpenHABUser;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Nicolaj Pedersen on 24-11-2017.
 */

public class NotificationReceiver extends BroadcastReceiver {
    Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        mIntent = intent;
        Log.e("Notification","intent for notification received");
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                OpenHABUser user = realm.where(OpenHABUser.class).findFirst();

                OpenHABConfig config = realm.where(OpenHABConfig.class).findFirst();


                new NotificationsTask().execute(config.getUrl(),user.getUser());
            }
        });
    }

    private class NotificationsTask extends AsyncTask<String,Void,String>{
        private OkHttpClient client;

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

            Log.e("Notification","Notifications: "+s);
//            NotificationCompat.Builder mBuilder =
//                    new NotificationCompat.Builder(mIntent.)
//                            .setSmallIcon(R.drawable.notification_icon)
//                            .setContentTitle("My notification")
//                            .setContentText("Hello World!");
        }
    }
}
