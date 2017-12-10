package se2gce17.openhab_se2.models;

import io.realm.annotations.RealmClass;

/**
 * @Author Nicolaj & Dan - Initial contribution
 */
@RealmClass
public class OpenHABNotification implements io.realm.RealmModel{

    private String timestamp;
    private String deviceId;
    private int notificationId;
    private boolean read;

    /**
     * OpenHABNotification is used for notifications received from the openHAB server. This timestamp
     * is used for comparison with the latest OpenHABNotification stored in the realm database.
     * If this newly generated notification is newer, the notification will be show to the user.
     * @param notification expected format example: "2017-12-24 23:59:59.0,Device1"
     */
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
