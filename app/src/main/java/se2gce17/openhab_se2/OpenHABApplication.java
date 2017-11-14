package se2gce17.openhab_se2;

import android.app.Application;
import io.realm.Realm;

/**
 * Created by Nicolaj Pedersen on 11-11-2017.
 */
public class OpenHABApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Realm. Should only be done once when the application starts.
        Realm.init(this);
    }
}