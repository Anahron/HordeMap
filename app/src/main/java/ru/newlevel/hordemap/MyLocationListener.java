package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class MyLocationListener {

    private static MyLocationListener instance = null;
    private static LocationManager locationManager;
    private static double latitude;
    private static double longitude;

    private MyLocationListener() {
    }

    public static MyLocationListener getInstance() {
        if (instance == null) {
            instance = new MyLocationListener();
        }
        return instance;
    }

    public static void startLocationListener() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                System.out.println(location.getProvider() + this.toString());
                Log.d("TAG", "latitude: " + latitude + ", longitude: " + longitude);
            }
        };
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 1, locationListener);
    }

    public static String getLastKnownLocation() {
        @SuppressLint("MissingPermission")
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        @SuppressLint("MissingPermission")
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (latitude == 0.0) {
            MyLocationListener.latitude = locationGPS.getLatitude();
            MyLocationListener.longitude = locationGPS.getLatitude();
        }
        if (longitude == 0.0) {
            MyLocationListener.latitude = locationNet.getLatitude();
            MyLocationListener.longitude = locationNet.getLatitude();
        }
        return latitude+"/"+longitude;
    }

}
