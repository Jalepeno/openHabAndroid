package se2gce17.openhab_se2;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import se2gce17.openhab_se2.models.OpenHABLocation;

import java.util.ArrayList;

/**
 * This adapter is used to handle the extra locations marked by the used and stored in the database.
 * the locations are shown in the main view, and for now only implements a TextView for displaying name
 * and the root LinearLayout for highlighting.
 *
 * @Author Nicolaj & Dan - Initial contribution
 */
public class LocationListAdapter extends ArrayAdapter<OpenHABLocation> {

    private Context context;
    private ArrayList<OpenHABLocation> locations;
    private Location currentLocation;
    int viewResource;

    public LocationListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.viewResource = resource;
    }


    public void setLocations(ArrayList<OpenHABLocation> locations) {
        this.locations = locations;
    }

    @Override
    public int getCount() {
        return locations.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.location_list_layout,parent,false);
        TextView tv = (TextView) rowView.findViewById(R.id.list_location_name_tv);
        LinearLayout ll = (LinearLayout) rowView.findViewById(R.id.list_location_ll);

        tv.setText(locations.get(position).getName());

        // background is highlighted in orange oif the location is within proximity
        if(currentLocation != null){
            if(Utils.calcLocationProximity(currentLocation,locations.get(position).getLocation(),locations.get(position).getRadius())== 1){
                ll.setBackgroundResource(R.color.orange500);
            }else{
                ll.setBackgroundResource(R.color.white_solid);
            }
        }else{
            ll.setBackgroundResource(R.color.white_solid);
        }
        return rowView;
    }

    public void setCurrentLocation(Location location) {
        currentLocation = location;
    }
}
