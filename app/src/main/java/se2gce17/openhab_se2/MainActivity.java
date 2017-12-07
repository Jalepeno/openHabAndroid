package se2gce17.openhab_se2;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPoller;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPollerParameter;

import se2gce17.openhab_se2.models.OpenHABConfig;
import se2gce17.openhab_se2.models.OpenHABLocation;
import se2gce17.openhab_se2.models.OpenHABUser;
import se2gce17.openhab_se2.models.RealmLocationWrapper;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean showingDialog = false;

    private final int MY_PERMISSIONS_REQUEST_LOCATION = 963;
    private LocationRequest locationRequest;
    private SwitchCompat serviceSwitch;
/*    private ImageView userEditIv;
    private ImageView settingsIv;*/
    private TextView userTv;
    private ImageView homeImg;
  //  private ImageButton addLocationBtn;
    private Location currentLocation;
    private RealmLocationWrapper home;
    private ListView locationLl;
    private LocationListAdapter adapter;
    private LinearLayout homeBackground;
    private ProgressBar getHomeProgress;
    private FloatingActionButton fab;

    private Intent serviceIntent;

    private static final int PERIOD = 60000; 	// 1 minute
    private PendingIntent pi=null;
    private AlarmManager mgr=null;
    private Realm realm;
    private OpenHABUser user;
    private ArrayList<OpenHABLocation> locations;
  //  private static OpenHABConfig config;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

/*        userEditIv = (ImageView) findViewById(R.id.drawer_account_iv);
        settingsIv = (ImageView) findViewById(R.id.drawer_settings_iv);*/
        fab = (FloatingActionButton) findViewById(R.id.fab);
        serviceSwitch = (SwitchCompat) findViewById(R.id.drawer_service_switch);
        userTv = (TextView) findViewById(R.id.drawer_user_tv);
        homeImg = (ImageView) findViewById(R.id.mark_home_imgview);
        locationLl = (ListView) findViewById(R.id.drawer_location_list);
  //      addLocationBtn = (ImageButton) findViewById(R.id.drawer_add_location_btn);
        homeBackground = (LinearLayout) findViewById(R.id.drawer_home_location_ll);
        getHomeProgress = (ProgressBar) findViewById(R.id.mark_home_progress);

        serviceSwitch.setEnabled(isMyServiceRunning(LocationReceiver.class));
        serviceSwitch.setChecked(isMyServiceRunning(LocationReceiver.class));



/*        userEditIv.setClickable(true);
        settingsIv.setClickable(true);*/

/*        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);*/

        getDataFromDb();
        setupListView();

        CheckHomeEnable();



        setViewListeners();
        if(checkLocationPermission()){ // if we dont have permission for location, we cannot use app.

            // google client setup
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_user:
                startEditUserDialog();
                return true;
            case R.id.menu_settings:
                startSettingsDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean CheckHomeEnable(){

        if(OpenHABConfig.getInstance().getEncryptionKey() == null || OpenHABConfig.getInstance().getEncryptionKey().isEmpty() ||
                OpenHABConfig.getInstance().getUrl() == null || OpenHABConfig.getInstance().getUrl().isEmpty() ||
                user == null || user.getUser().isEmpty()){
            homeImg.setImageResource(R.drawable.ic_home_disabled);
            homeImg.setClickable(false);
            return false;
        }
        else if (home == null) {
            homeImg.setImageResource(R.drawable.ic_home_red);
            homeImg.setClickable(true);

        } else {
            homeImg.setImageResource(R.drawable.ic_home_green);
            homeImg.setClickable(true);
            serviceSwitch.setEnabled(true);
        }
        return true;

    }


    private void setViewListeners() {

        // click listener for home button
        homeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(OpenHABConfig.getInstance().getUrl() != null && OpenHABConfig.getInstance().getEncryptionKey() != null && !OpenHABConfig.getInstance().getEncryptionKey().isEmpty()){
                    GetHomeTask task = new GetHomeTask();
                    task.execute(OpenHABConfig.getInstance().getUrl(),userTv.getText().toString(),OpenHABConfig.getInstance().getEncryptionKey());
                }else{

                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationDialog();
            }
        });


        // event listener for switch
        serviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    // in case of home location has not been set, and username not set
                    // we cannot start using service.
                    if (home == null || userTv.getText().length() < 3) {
                        serviceSwitch.setChecked(false);
                    } else {
                        startService();
                    }
                } else {
                    stopService();
                    Toast.makeText(MainActivity.this,
                                    "Service cancelled",
                            Toast.LENGTH_LONG)
                            .show();


                }
            }
        });

    }

    private void startSettingsDialog() {
        if(showingDialog){
            return;
        }
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.settings_dialog, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextInputEditText urlEt = promptsView.findViewById(R.id.settings_url_et);
        final TextInputEditText keyEt = promptsView.findViewById(R.id.settings_encryption_key_et);
        final Button clearLocDbBtn = promptsView.findViewById(R.id.settings_clear_location_db_btn);
        final Button clearUserDbBtn = promptsView.findViewById(R.id.settings_clear_user_db_btn);
        final Button doneBtn = promptsView.findViewById(R.id.settings_done_btn);
        urlEt.setText(OpenHABConfig.getInstance().getUrl());
        keyEt.setText(OpenHABConfig.getInstance().getEncryptionKey());


        final AlertDialog alertDialog = alertDialogBuilder.create();

        clearLocDbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.delete(OpenHABLocation.class);
                        locations.clear();
                        adapter.setLocations(locations);
                        adapter.notifyDataSetInvalidated();
                        locationLl.invalidate();
                        homeBackground.setBackgroundResource(R.color.white_solid);
                        home = null;
                        stopService();
                        serviceSwitch.setChecked(false);
                        serviceSwitch.setEnabled(false);
                        homeImg.setImageResource(R.drawable.ic_home_red);
                    }
                });
            }
        });

        clearUserDbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.delete(OpenHABUser.class);
                        user = null;
                        userTv.setText("");
                        stopService();
                        serviceSwitch.setChecked(false);
                        serviceSwitch.setEnabled(false);
                    }
                });
            }
        });

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        OpenHABConfig.setInstance(realm.where(OpenHABConfig.class).findFirst());

                        OpenHABConfig conf = OpenHABConfig.getInstance();

                        if(urlEt.getText().toString().isEmpty()){
                            conf.setBackupUrl();
                        }else{
                            conf.setUrl(urlEt.getText().toString().trim());
                        }
                        // adding new encryption key here.. this will be used for AES encryption
                        if(!keyEt.getText().toString().isEmpty()){
                            conf.setEncryptionKey(keyEt.getText().toString().trim());

                        }
                        homeImg.setClickable(true);

                    }
                });

                alertDialog.dismiss();
            }
        });

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                showingDialog = false;
            }
        });
        showingDialog = true;
        // show it
        alertDialog.show();
    }

    private void startEditUserDialog() {
        if(showingDialog){
            return;
        }
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.change_user_diaog, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextInputEditText settingUserEt = (TextInputEditText) promptsView
                .findViewById(R.id.change_user_et);
        final Button okBtn = (Button) promptsView.findViewById(R.id.change_user_ok_btn);
        final Button cancelBtn = (Button) promptsView.findViewById(R.id.change_user_cancel_btn);

        if(user != null){
            settingUserEt.setText(user.getUser());
        }
        // create alert dialog
        final AlertDialog alertDialog = alertDialogBuilder.create();

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(settingUserEt.getText().length() < 2 ){
                    return;
                }
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        user = realm.where(OpenHABUser.class).findFirst();
                        if(user == null){
                            user = realm.createObject(OpenHABUser.class);
                        }
                        user.setUser(settingUserEt.getText().toString());
                    }
                });
                userTv.setText(user.getUser());

                // finding home by using new name..
                if(CheckHomeEnable()){
                    new GetHomeTask().execute(OpenHABConfig.getInstance().getUrl(),user.getUser(),OpenHABConfig.getInstance().getEncryptionKey());
                }

                alertDialog.dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                showingDialog = false;

            }
        });
        showingDialog = true;
        // show it
        alertDialog.show();
    }

    private void startLocationDialog(){
        if(showingDialog){
            return;
        }
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.new_location_setup, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextInputEditText locationName = (TextInputEditText) promptsView.findViewById(R.id.new_location_name_et);
        final TextInputEditText locationRadius = (TextInputEditText) promptsView.findViewById(R.id.new_location_radius_et);

        final Button cancelBtn = (Button) promptsView.findViewById(R.id.new_location_cancel_brn);
        final Button markBtn = (Button) promptsView.findViewById(R.id.new_location_mark_btn);

        final AlertDialog alertDialog = alertDialogBuilder.create();

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        markBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLocation == null){
                    Toast.makeText(MainActivity.this,
                            "Location data not found",
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (locationName.getText().length() > 2 && locationRadius.getText().length() > 0) {

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            int locationId = 1;
                            RealmResults<OpenHABLocation> existingLocations = realm.where(OpenHABLocation.class).findAll();
                            for (OpenHABLocation l : existingLocations) {
                                if (l.getId() > locationId) {
                                    locationId = l.getId();
                                }
                            }
                            locationId++;

                            OpenHABLocation newLocation = realm.createObject(OpenHABLocation.class);

                            RealmLocationWrapper wrapLocation = realm.createObject(RealmLocationWrapper.class);
                            wrapLocation.setLatitude(currentLocation.getLatitude());
                            wrapLocation.setLongitude(currentLocation.getLongitude());
                            wrapLocation.setTime(System.currentTimeMillis());

                            newLocation.setId(locationId);
                            newLocation.setRadius(Integer.valueOf(locationRadius.getText().toString()));
                            newLocation.setName(locationName.getText().toString());
                            newLocation.setDbName(user.getUser() + "_" + locationName.getText().toString());
                            newLocation.setLocation(wrapLocation);
                            Log.d("NorthQ", "new Location: (Name: "+newLocation.getName()
                                    +", lat: "+String.valueOf(newLocation.getLocation().getLatitude())
                                    +", long: "+String.valueOf(newLocation.getLocation().getLongitude()));
                        }
                    });
                }
                updateLocations();
                alertDialog.dismiss();
            }
        });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                showingDialog = false;
                CheckHomeEnable();
            }
        });
        showingDialog = true;
        alertDialog.show();
    }

    private void updateLocations() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<OpenHABLocation> foundLocations = realm.where(OpenHABLocation.class).findAll();

                locations.clear();
                for(OpenHABLocation l : foundLocations){
                    if(l.getId()>0){
                        locations.add(l);
                    }
                }

            }
        });
        adapter.setLocations(locations);
        adapter.notifyDataSetInvalidated();

    }

    private void setupListView() {
        adapter = new LocationListAdapter(this,R.layout.location_list_layout);
        adapter.setLocations(locations);
        locationLl.setAdapter(adapter);
        locationLl.invalidate();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    protected void onStart() {
        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        if(mGoogleApiClient != null)
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onBackPressed() {
      /*  DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }*/
        super.onBackPressed();
    }


    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        //locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }


    /**
     * starts the wakeful intent service
     */
    public void startService(){
        Log.e("Service","Starting service ");
        mgr=(AlarmManager)getSystemService(ALARM_SERVICE);

        Intent i=new Intent(this, LocationPoller.class);

        Bundle bundle = new Bundle();
        LocationPollerParameter parameter = new LocationPollerParameter(bundle);

        // this will be the intent that the LocationReceiver will receive
        Intent broardcastIntentLocation = new Intent(this, LocationReceiver.class);
        final String SEND_LOCATION = "se2gce17.openhab_se2.LocationReceiver";
        IntentFilter intentFilterLocation = new IntentFilter(SEND_LOCATION);
        this.registerReceiver(new LocationReceiver(), intentFilterLocation);




        parameter.setIntentToBroadcastOnCompletion(broardcastIntentLocation);
//        parameter.setIntentToBroadcastOnCompletion(broardcastIntentNotification);


        // try GPS and fall back to NETWORK_PROVIDER
        parameter.setProviders(new String[] {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER});
        parameter.setTimeout(60000); // 1 minutes
        i.putExtras(bundle);


        pi=PendingIntent.getBroadcast(this, 0, i, 0);
        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                PERIOD,
                pi);

        Toast
                .makeText(this,
                        "Location polling every minute begun",
                        Toast.LENGTH_LONG)
                .show();
    }

    public void stopService(){
        if (mgr != null) {
            mgr.cancel(pi);
            mgr = null;

        }
    }



    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // milliseconds
        locationRequest.setFastestInterval(5000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(checkLocationPermission()){
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        adapter.serCurrentLocation(location);
        if(home != null) {
            if (Utils.calcLocationProximity(location, home, 50) == 1) {
                homeBackground.setBackgroundResource(R.color.orange500);

            } else {
                homeBackground.setBackgroundResource(R.color.white_solid);
            }
        }
        adapter.notifyDataSetInvalidated();
        locationLl.invalidate();

        Log.d("MAPS","new location has been found!!!! --- lat: "+location.getLatitude()+" -- long:"+location.getLongitude());
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if(mMap != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
    }

    /**
     * connectes to the Realm database and fetches the data
     */
    public void getDataFromDb() {
        realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                user = realm.where(OpenHABUser.class).findFirst();
                if(user == null){
                        user = realm.createObject(OpenHABUser.class);
                }
                if(user.getUser() != null){
                    userTv.setText(user.getUser());
                }

                OpenHABLocation location = realm.where(OpenHABLocation.class).equalTo("id",0).findFirst();
                if(location != null){
                    home = location.getLocation();
                }

                RealmResults<OpenHABLocation> results = realm.where(OpenHABLocation.class).findAll();
                locations  = new ArrayList<>();
                for(OpenHABLocation l : results){
                    if(l.getId()>0){// id of home is 0, which we don't want to add here
                        System.out.println("location found: "+l.getName());
                        locations.add(l);
                    }
                }
                OpenHABConfig config = realm.where(OpenHABConfig.class).findFirst();
                if(config == null){
                    config = realm.createObject(OpenHABConfig.class);
                    config.setBackupUrl();
                }
                OpenHABConfig.setInstance(config);

                Log.e("DB data","my url: "+config.getUrl());
            }
        });

    }



    private class GetHomeTask extends AsyncTask<String,Void,String>{
        private OkHttpClient client;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getHomeProgress.setVisibility(View.VISIBLE);
            homeImg.setVisibility(View.INVISIBLE);


        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                client = new OkHttpClient();


                Request request = new Request.Builder()
                        .url(strings[0]+"getHome="+strings[1])
                        .build();


                Response response = client.newCall(request).execute();
                if(response.body() != null){
                    String responseBody = response.body().string();
                    Log.e("GET_HOME","response: "+responseBody);
                    String decrypted = Utils.decrypt(strings[2],responseBody);
                    Log.e("GET_HOME","decrypted respose: "+decrypted);
                    return decrypted;

                }
                return null;
            }catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String returnValue) {
            super.onPostExecute(returnValue);
            getHomeProgress.setVisibility(View.INVISIBLE);
            homeImg.setVisibility(View.VISIBLE);
            if(returnValue == null){

                // if invalid location is return we cannot use service.
                homeImg.setImageResource(R.drawable.ic_home_red);
                Toast.makeText(MainActivity.this,getResources().getString(R.string.toast_get_home_no_user),Toast.LENGTH_LONG).show();
                return;
            }else if(returnValue.isEmpty()){
                // if invalid location is return we cannot use service.
                homeImg.setImageResource(R.drawable.ic_home_red);
                Toast.makeText(MainActivity.this,getResources().getString(R.string.toast_get_home_no_user),Toast.LENGTH_LONG).show();
                return;
            }
            Log.e("response", returnValue);
            String[] result = returnValue.split(","); // this is the format that can be copied directly from google maps
            final double latitude = Double.valueOf(result[0].trim());
            final double longitude = Double.valueOf(result[1].trim());
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmLocationWrapper rlw = realm.createObject(RealmLocationWrapper.class);
                    rlw.setLatitude(latitude);
                    rlw.setLongitude(longitude);
                    rlw.setTime(System.currentTimeMillis());

                    OpenHABLocation newHome = realm.where(OpenHABLocation.class).equalTo("id",0).findFirst();
                    if(newHome == null){
                        newHome = realm.createObject(OpenHABLocation.class);
                    }
                    newHome.setDbName("Home");
                    newHome.setName("Home");
                    newHome.setId(0);
                    newHome.setImgResourceId(R.drawable.ic_home_green);
                    newHome.setRadius(OpenHABConfig.getInstance().getHomeRadius());
                    newHome.setLocation(rlw);
                    home = rlw;
                    homeImg.setImageResource(R.drawable.ic_home_green);
                    serviceSwitch.setEnabled(true);
                }
            });

        }
    }
}