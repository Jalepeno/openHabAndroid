package se2gce17.openhab_se2;

import android.app.Application;
import io.realm.Realm;

/**
 * @Author Nicolaj & Aslan - Initial contribution
 */
public class OpenHABApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Realm. Should only be done once when the application starts.
        Realm.init(this);
    }
}