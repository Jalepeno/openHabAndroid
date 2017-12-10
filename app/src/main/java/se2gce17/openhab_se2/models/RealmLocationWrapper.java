package se2gce17.openhab_se2.models;

import io.realm.annotations.RealmClass;

/**
 * @Author Nicolaj & Dan - Initial contribution
 */
@RealmClass
public class RealmLocationWrapper implements io.realm.RealmModel{
    private double latitude;
    private double longitude;
    private long time;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
