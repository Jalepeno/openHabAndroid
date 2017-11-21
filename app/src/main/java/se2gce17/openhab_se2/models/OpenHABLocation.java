package se2gce17.openhab_se2.models;


import io.realm.annotations.RealmClass;

/**
 * Created by Nicolaj Pedersen on 11-11-2017.
 */

@RealmClass
public class OpenHABLocation implements io.realm.RealmModel{
    private RealmLocationWrapper location;

    private int id;
    private int radius;
    private String name;
    private String dbName;
    private int imgResourceId;


    /**
     * expected to be username + custom#"
     * @return
     */
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RealmLocationWrapper getLocation() {
        return location;
    }

    public void setLocation(RealmLocationWrapper location) {
        this.location = location;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImgResourceId() {
        return imgResourceId;
    }

    public void setImgResourceId(int imgResourceId) {
        this.imgResourceId = imgResourceId;
    }

}