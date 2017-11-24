package se2gce17.openhab_se2.models;

import io.realm.annotations.RealmClass;


/**
 * Created by Nicolaj Pedersen on 11-11-2017.
 */

@RealmClass
public class OpenHABUser implements io.realm.RealmModel {

    private String user;
    private OpenHABLocation lastLocation;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    public OpenHABLocation getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(OpenHABLocation lastLocation) {
        this.lastLocation = lastLocation;
    }

}