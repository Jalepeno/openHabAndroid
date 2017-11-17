package se2gce17.openhab_se2;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nicolaj Pedersen on 15-11-2017.
 */
public class LocationListAdapter extends ArrayAdapter<OpenHABLocation> {

    private Context context;
    private ArrayList<OpenHABLocation> locations;
    private Location currentLocation;



    public LocationListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public void setLocations(ArrayList<OpenHABLocation> locations) {
        this.locations = locations;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.location_list_layout,parent,false);
        TextView tv = (TextView) rowView.findViewById(R.id.list_location_name_tv);
        ImageView iv = (ImageView) rowView.findViewById(R.id.list_location_iv);
        LinearLayout ll = (LinearLayout) rowView.findViewById(R.id.list_location_ll);

        tv.setText(locations.get(position).getName());
        iv.setImageResource(locations.get(position).getImgResourceId());

        if(currentLocation != null){
            if(calcLocationProximity(locations.get(position).getLocation(),locations.get(position).getRadius())== 1){
                ll.setBackgroundResource(R.color.orange500);
            }else{
                ll.setBackgroundResource(R.color.white_solid);
            }
        }else{
            ll.setBackgroundResource(R.color.white_solid);
        }



        return rowView;




    }

    private int calcLocationProximity( RealmLocationWrapper loc2, int distanceProx) {

        Location lastLocation = new Location("");
        lastLocation.setLatitude(loc2.getLatitude());
        lastLocation.setLongitude(loc2.getLongitude());

        if(currentLocation == null || loc2 == null){
            return -1;
        }

        float distance = currentLocation.distanceTo(lastLocation);
        if ((int) distance <= distanceProx) {
            return 1;
        }
        return 0;

    }
}
