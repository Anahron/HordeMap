package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.MapsActivity.mMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class ImportantMarkers {
    private static final int  markerSize = 60;
    private static final List<Marker> importantMarkers = new ArrayList<>();
    private static final Bitmap swords1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords1);
    private static final Bitmap swords2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords2);
    private static final Bitmap swords3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords3);
    private static final Bitmap swords4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords4);
    private static final Bitmap swords5 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords5);
    private static final Bitmap swords6 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords6);
    private static final Bitmap swords7 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords7);
    private static final Bitmap swords8 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords8);
    private static final Bitmap swords9 = BitmapFactory.decodeResource(context.getResources(), R.drawable.swords9);
    private static final Bitmap blue_camp = BitmapFactory.decodeResource(context.getResources(), R.drawable.blue_camp);
    private static final Bitmap yellow_camp = BitmapFactory.decodeResource(context.getResources(), R.drawable.yellow_camp);
    private static final BitmapDescriptor swordsicon1 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords1, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon2 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords2, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon3 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords3, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon4 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords4, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon5 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords5, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon6 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords6, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon7 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords7, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon8 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords8, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon9 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords9, markerSize, markerSize, false));
    private static final BitmapDescriptor blue_campicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(blue_camp, markerSize, markerSize, false));
    private static final BitmapDescriptor yellow_campicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(yellow_camp, markerSize, markerSize, false));

    public static void importantMarkersCreate(){
        importantMarkers.clear();
        MarkerOptions marker1 = new MarkerOptions().position(new LatLng(55.6739849,85.1152591)).icon(swordsicon1);
        importantMarkers.add(mMap.addMarker(marker1));
        MarkerOptions marker2 = new MarkerOptions().position(new LatLng(55.671909,85.1136278)).icon(swordsicon2);
        importantMarkers.add(mMap.addMarker(marker2));
        MarkerOptions marker3 = new MarkerOptions().position(new LatLng(55.6698473,85.1120416)).icon(swordsicon3);
        importantMarkers.add(mMap.addMarker(marker3));
        MarkerOptions marker4 = new MarkerOptions().position(new LatLng(55.6706025,85.1084187)).icon(swordsicon4);
        importantMarkers.add(mMap.addMarker(marker4));
        MarkerOptions marker5 = new MarkerOptions().position(new LatLng(55.6727025,85.1099604)).icon(swordsicon5);
        importantMarkers.add(mMap.addMarker(marker5));
        MarkerOptions marker6 = new MarkerOptions().position(new LatLng(55.6746783,85.1116493)).icon(swordsicon6);
        importantMarkers.add(mMap.addMarker(marker6));
        MarkerOptions marker7 = new MarkerOptions().position(new LatLng(55.6754975,85.1079993)).icon(swordsicon7);
        importantMarkers.add(mMap.addMarker(marker7));
        MarkerOptions marker8 = new MarkerOptions().position(new LatLng(55.6734572,85.1064004)).icon(swordsicon8);
        importantMarkers.add(mMap.addMarker(marker8));
        MarkerOptions marker9 = new MarkerOptions().position(new LatLng(55.6713818,85.104792)).icon(swordsicon9);
        importantMarkers.add(mMap.addMarker(marker9));
        MarkerOptions markerM1 = new MarkerOptions().position(new LatLng(55.6677,85.1148)).title("Мертвяк ЮГ").icon(blue_campicon);
        importantMarkers.add(mMap.addMarker(markerM1));
        MarkerOptions markerM2 = new MarkerOptions().position(new LatLng(55.6704,85.1004)).title("Мертвяк СЕВЕР").icon(yellow_campicon);
        importantMarkers.add(mMap.addMarker(markerM2));

        for (Marker marker : importantMarkers ) {
           marker.setVisible(true);
        }
    }
}
