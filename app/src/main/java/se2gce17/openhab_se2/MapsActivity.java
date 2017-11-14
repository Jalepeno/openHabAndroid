package se2gce17.openhab_se2;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import io.realm.Realm;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPoller;
import se2gce17.openhab_se2.cwac_loclpoll.LocationPollerParameter;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private final int MY_PERMISSIONS_REQUEST_LOCATION = 123;
    private LocationRequest locationRequest;
    private SwitchCompat serviceSwitch;
    private TextInputEditText userEt;
    private TextInputEditText nameEt;
    private ImageButton homeImg;
    private Location currentLocation;
    private RealmLocationWrapper home;

    private Intent serviceIntent;

    private static final int PERIOD=60000; 	// 1 minute
    private PendingIntent pi=null;
    private AlarmManager mgr=null;
    private Realm realm;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        serviceSwitch = (SwitchCompat) findViewById(R.id.drawer_service_switch);
        userEt = (TextInputEditText) findViewById(R.id.drawer_user_et);
        nameEt = (TextInputEditText) findViewById(R.id.drawer_name_et);
        homeImg = (ImageButton) findViewById(R.id.mark_home_imgview);
        serviceSwitch.setEnabled(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        getDataFromDb();

        if (home == null) {
            homeImg.setImageResource(R.drawable.ic_home_red);
        } else {
            homeImg.setImageResource(R.drawable.ic_home_green);
            if(userEt.getText().length() >2 && nameEt.getText().length() >2){
                serviceSwitch.setEnabled(true);
            }
        }


        // click listener for home button
        homeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(home == null){
                    markCurrentLocationAsHome();
                }else{

                }
            }
        });

        // event listener for switch
        serviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    // in case of home location has not been set, and username not set
                    // we cannot start using service.
                    if (home == null || userEt.getText().length() < 3 || nameEt.getText().length() < 3) {
                        serviceSwitch.setChecked(false);
                    } else {
                        startService();
                    }
                } else {
                    if (mgr != null) {
                        mgr.cancel(pi);
                        mgr = null;
                        Toast
                                .makeText(MapsActivity.this,
                                        "Service cancelled",
                                        Toast.LENGTH_LONG)
                                .show();
                    }

                }
            }
        });

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
                                ActivityCompat.requestPermissions(MapsActivity.this,
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

    private void markCurrentLocationAsHome() {

            if (mGoogleApiClient != null) {
                if(home == null){
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmLocationWrapper wrapper = realm.createObject(RealmLocationWrapper.class);
                            wrapper.setTime(currentLocation.getTime());
                            wrapper.setLatitude(currentLocation.getLatitude());
                            wrapper.setLongitude(currentLocation.getLongitude());
                            home = wrapper;

                            OpenHABLocation home = realm.createObject(OpenHABLocation.class);


                            home.setLocation(wrapper);
                            home.setId(0);
                            home.setName("Home");
                            home.setDbName("Home");
                            home.setRadius(50); //TODO: make configurable
                            home.setImgResourceId(R.drawable.ic_home_green);


                            OpenHABUser user = realm.createObject(OpenHABUser.class);
                            user.setName(nameEt.getText().toString().trim());
                            user.setUser(userEt.getText().toString().trim());
                            user.setLastLocation(home);
                        }
                    });
                    homeImg.setImageResource(R.drawable.ic_home_green);

//                    mMap.addMarker(new MarkerOptions().position(new LatLng(home.getLatitude(),home.getLongitude())).title("Home"));
                    serviceSwitch.setEnabled(true);
                }
            }


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMinZoomPreference(5f);
        mMap.setMaxZoomPreference(15f);

        if(checkLocationPermission()){
            Location l = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if(l != null){
                LatLng currentLocation = new LatLng(l.getLatitude(), l.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));

            }
        }

    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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



    public void startService(){
        Log.e("Service","Starting service ");
        mgr=(AlarmManager)getSystemService(ALARM_SERVICE);

        Intent i=new Intent(this, LocationPoller.class);

        Bundle bundle = new Bundle();
        LocationPollerParameter parameter = new LocationPollerParameter(bundle);

        // this will be the intent that the LocationReceiver will receive
        Intent broardcastIntent = new Intent(this, LocationReceiver.class);

        final String SOME_ACTION = "se2gce17.openhab_se2.LocationReceiver";
        IntentFilter intentFilter = new IntentFilter(SOME_ACTION);
        this.registerReceiver(new LocationReceiver(), intentFilter);


        parameter.setIntentToBroadcastOnCompletion(broardcastIntent);
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




    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // milliseconds
        locationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
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
       // Log.d("MAPS","new location has been found!!!! --- lat: "+location.getLatitude()+" -- long:"+location.getLongitude());
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if(mMap != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
    }

    public void getDataFromDb() {
        realm = Realm.getDefaultInstance();

        OpenHABUser user = realm.where(OpenHABUser.class).findFirst();

        if(user != null){
            userEt.setText(user.getUser());
            nameEt.setText(user.getName());

        }
        OpenHABLocation location = realm.where(OpenHABLocation.class).equalTo("id",0).findFirst();
        if(location != null){
            home = location.getLocation();
        }
    }


}
