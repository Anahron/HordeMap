package ru.newlevel.hordemap;

import android.location.Location;
import android.os.Bundle;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class MarkerUpdator extends MapsActivity {
    private static  GoogleMap mMap;
    private static int id;
    private static ArrayList<Marker> markers;

    public MarkerUpdator(GoogleMap mMap, int id) {
        MarkerUpdator.mMap = mMap;
        MarkerUpdator.id = id;
    }
    public static void create(HashMap<Integer, String> map) {
        if (markers.size() > 0) {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
        }
        for (Integer id : map.keySet()) {
            if (MarkerUpdator.id != id) {
                System.out.println(map.get(id));
                String[] data = Objects.requireNonNull(map.get(id)).split("/");
                Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(data[2]), Double.parseDouble(data[3]))).title(data[0] +" " + data[4]));
                markers.add(marker);
            }
        }
    }
    public static void destroy() {
        if (markers.size() > 0) {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
        }
    }
}


