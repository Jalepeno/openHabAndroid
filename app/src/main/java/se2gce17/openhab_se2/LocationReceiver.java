package se2gce17.openhab_se2;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPollerResult;
import se2gce17.openhab_se2.models.OpenHABConfig;
import se2gce17.openhab_se2.models.OpenHABLocation;
import se2gce17.openhab_se2.models.OpenHABUser;

import static android.content.ContentValues.TAG;
import static android.util.Base64.DEFAULT;

/**
 * Created by Nicolaj Pedersen on 23-10-2017.
 */

public class LocationReceiver extends BroadcastReceiver {

    private Location home;
    private String user;
    private String name;


    @Override
    public void onReceive(Context context, Intent intent) {

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

                sendLocationDataToWebsite(loc, openHABUser);
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
}
