package se2gce17.openhab_se2.models;

import io.realm.annotations.RealmClass;

/**
 * Created by Nicolaj Pedersen on 01-12-2017.
 */

@RealmClass
public class OpenHABNotification implements io.realm.RealmModel{
    private String timestamp;
    private String deviceId;
    private int notificationId;
    private boolean read;

    public OpenHABNotification(String notification) {
        String[] data = notification.split(",");
        timestamp = data[0];
        deviceId = data[1];
    }

    public OpenHABNotification() {
    }

    public String getTimestamp() {
        return timestamp;
    }

    //TODO: adjust based on format
    public String getFormattedTimestamp(){
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int mNotificationId) {
        this.notificationId = mNotificationId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

}
