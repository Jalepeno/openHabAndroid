package se2gce17.openhab_se2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPollerResult;

import static android.content.ContentValues.TAG;

/**
 * Created by Nicolaj Pedersen on 23-10-2017.
 */

public class LocationReceiver  extends BroadcastReceiver {

    private Location home;
    private String username;


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e("LocationReceiver","intent has been wakened");
        Bundle b=intent.getExtras();

        home = (Location) intent.getExtras().get("home");
        username = intent.getExtras().getString("user");

        LocationPollerResult locationResult = new LocationPollerResult(b);
        String msg;

        Location loc=locationResult.getLocation();
        if (loc==null) {
            loc=locationResult.getLastKnownLocation();

            if (loc==null) {
                msg=locationResult.getError();
            }
            else {
                msg="TIMEOUT, lastKnown="+loc.toString();
            }
        }
        else {
            msg=loc.toString();
        }

        if (msg==null) {
            msg="Invalid broadcast received!";
        }

        sendLocationDataToWebsite(loc);
    }


    protected void sendLocationDataToWebsite(Location location) {
        NetworkTask task = new NetworkTask();
        Integer[] params = new Integer[]{Integer.valueOf(calcLocationProximity(home,location,100))};

        task.execute(params);

    }


    /**
     *
     * @param loc1
     * @param loc2
     * @param distanceProx proimity in meters must be >= 0
     * @return returns 1 in locations are within proximity of eachotther, else return 0;
     */
    private int calcLocationProximity(Location loc1, Location loc2, int distanceProx) {

        float distance = loc1.distanceTo(loc2);
        if ((int) distance <= distanceProx) {
            return 1;
        }
        return 0;

    }

    private class NetworkTask extends AsyncTask<Integer,Void,Void>{

        private OkHttpClient client;
        private final String urlPath= "http://95.85.57.71:8080/NorthqGpsService/gps?";


        @Override
        protected Void doInBackground(Integer... integers) {
            try {
                client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(urlPath+"user="+username+"&data="+integers[0])
                        .build();

                Response response = client.newCall(request).execute();
                response.body().string();
                Log.e(TAG, "-- sending location --- is close to home: "+integers[0]);
            }catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
