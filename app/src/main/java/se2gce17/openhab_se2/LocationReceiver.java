package se2gce17.openhab_se2;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.Preferences;

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

import static android.content.ContentValues.TAG;
import static android.util.Base64.DEFAULT;

/**
 * Created by Nicolaj Pedersen on 23-10-2017.
 */

public class LocationReceiver  extends BroadcastReceiver {

    private Location home;
    private String user;
    private String name;


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e("LocationReceiver","intent has been wakened");
        Bundle b=intent.getExtras();

        LocationPollerResult locationResult = new LocationPollerResult(b);
        final Location loc=locationResult.getLocation();
        if(loc == null){
            Log.e(TAG,"the current location i null");
            return;
        }



        Realm realm = Realm.getDefaultInstance();
        final OpenHABUser openHABUser = realm.where(OpenHABUser.class).findFirst();
        if(openHABUser == null){
            Log.e(TAG,"the user is null");
        }





        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {


                RealmResults<OpenHABLocation> results = realm.where(OpenHABLocation.class).findAll();

                for(OpenHABLocation l : results){
                    if(calcLocationProximity(loc,l.getLocation(),l.getRadius())==1){
                        openHABUser.setLastLocation(l);
                    }
                }
            }
        });

     /*
        home = (Location) intent.getExtras().get("location");
        user = intent.getExtras().getString("user");
        name = intent.getExtras().getString("username");
     */

        sendLocationDataToWebsite(loc,openHABUser);
    }


    protected void sendLocationDataToWebsite(Location location,OpenHABUser oHABuser) {
        // ex "1;anders_home" for user with name = anders and is currently within the range of his home

       // int proximity = calcLocationProximity(location,oHABuser.getLastLocation().getLocation(),oHABuser.getLastLocation().getRadius());
        int proximity = 1;
        Log.d(TAG, "is close to "+oHABuser.getLastLocation().getDbName()+": "+proximity);
        if(proximity == -1){ // error check, if current location or last location is null
            return;
        }

      //  String data = ""+proximity+";"+oHABuser.getLastLocation().getDbName();
        String data = ""+proximity+";"+"Home";
        String encrypedData = encrypt(data);
        Log.d(TAG,"data: "+ data);
        Log.d(TAG,"encrypedData: "+encrypedData);
        String[] params = new String[]{oHABuser.getName(),oHABuser.getUser(),encrypedData};

        NetworkTask task = new NetworkTask();


    //    Integer[] params = new Integer[]{Integer.valueOf(calcLocationProximity(home,location,100))};

        task.execute(params);

    }

    private final byte[] keyValue = "Beercalc12DTU123".getBytes();

    // Generates a key
    private Key generateKey() {
        Key key = new SecretKeySpec(keyValue, "AES");
        return key;
    }

    @TargetApi(11)
    public String encrypt(String plainText) {
        try {
            Cipher AesCipher = Cipher.getInstance("AES");
            AesCipher.init(Cipher.ENCRYPT_MODE, generateKey());


            return new String(Base64.encode(AesCipher.doFinal(plainText.getBytes()),DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @TargetApi(11)
    public String decrypt(String cipherText){
        try {
            Cipher AesCipher;
            AesCipher = Cipher.getInstance("AES");
            AesCipher.init(Cipher.DECRYPT_MODE, generateKey());
            System.out.println(AesCipher.doFinal(Base64.decode(cipherText.getBytes(),DEFAULT)).length);

            return new String(AesCipher.doFinal(Base64.decode(cipherText.getBytes(),DEFAULT)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /**
     *
     * @param loc1
     * @param loc2
     * @param distanceProx proimity in meters must be >= 0
     * @return returns 1 in locations are within proximity of eachotther, else return 0;
     */
    private int calcLocationProximity(Location loc1, RealmLocationWrapper loc2, int distanceProx) {

        Location lastLocation = new Location("");
        lastLocation.setLatitude(loc2.getLatitude());
        lastLocation.setLongitude(loc2.getLongitude());

        if(loc1 == null || loc2 == null){
            return -1;
        }

        float distance = loc1.distanceTo(lastLocation);
        if ((int) distance <= distanceProx) {
            return 1;
        }
        return 0;

    }

    private class NetworkTask extends AsyncTask<String,Void,Void>{

        private OkHttpClient client;
        private final String urlPath= "http://95.85.57.71:8080/NorthqGpsService/gps?";


        @Override
        protected Void doInBackground(String... strings) {
            try {
                client = new OkHttpClient();
                // params
                // 0 = name;
                // 1 = user;
                // 2 = data(encrypted)

                Request request = new Request.Builder()
                        .url(urlPath+"user="+strings[1]+"&name="+strings[0]+"&data="+strings[2])
                        .build();

                Response response = client.newCall(request).execute();
                response.body().string();

                Log.e(TAG,"-- sending location -- "+"user="+strings[1]+"&name="+strings[0]);
            }catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
