package se2gce17.openhab_se2;

import android.location.Location;

/**
 * Created by Nicolaj Pedersen on 21-11-2017.
 */

public class Utils {

    public static final String url = "http://95.85.57.71:8080/NorthqGpsService/gps?";

    /**
     *
     * @param loc1 current location
     * @param loc2 targeted location
     * @param distanceProx proimity in meters must be >= 0
     * @return returns 1 in locations are within proximity of eachotther, else return 0;
     */
    public static int calcLocationProximity(Location loc1, RealmLocationWrapper loc2, int distanceProx) {

        if(loc1 == null || loc2 == null){
            return -1;
        }

        Location lastLocation = new Location("");
        lastLocation.setLatitude(loc2.getLatitude());
        lastLocation.setLongitude(loc2.getLongitude());



        float distance = loc1.distanceTo(lastLocation);
        if ((int) distance <= distanceProx) {
            return 1;
        }
        return 0;
    }
}
