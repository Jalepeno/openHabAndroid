package se2gce17.openhab_se2.models;

import io.realm.Realm;
import io.realm.annotations.RealmClass;

/**
 * Created by Nicolaj Pedersen on 21-11-2017.
 */
@RealmClass
public class OpenHABConfig implements io.realm.RealmModel {
    private static OpenHABConfig instance;

    private String url;
    private int homeRadius;

    public OpenHABConfig(){

    }

    public static OpenHABConfig getInstance(){
        if(instance == null){
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    instance = realm.where(OpenHABConfig.class).findFirst();

                    // if no instance exist at this point we will generate a new instance of it with default values
                    if(instance == null){
                        instance = realm.createObject(OpenHABConfig.class);

                        instance.setBackupUrl(); // default value for our project server url
                    }

                }
            });

        }
        return instance;
    }

    public void setBackupUrl(){
        this.url = "http://se2-openhab03.compute.dtu.dk:9090/NorthqGpsService/gps?";
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static void setInstance(OpenHABConfig instance) {
        OpenHABConfig.instance = instance;
    }

    public int getHomeRadius() {
        return homeRadius;
    }

    public void setHomeRadius(int homeRadius) {
        this.homeRadius = homeRadius;
    }
}