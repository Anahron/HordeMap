package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MyLocationListener {

    private static LocationManager locationManager;
    public static double latitude;
    public static double longitude;
    public static List<LatLng> coordinates = new ArrayList<>();
    private static final int REQUEST_INTERNET_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final float MAX_DISTANCE = 1000.0f; // максимальное допустимое расстояние в метрах
    private static Location previousLocation;
    private static MyLocationListener instance = null;

    private MyLocationListener() {
    }

    public static MyLocationListener getInstance() {
        if (instance == null) {
            instance = new MyLocationListener();
        }
        return instance;
    }

    public void startLocationListener() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = location -> {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            coordinates.add(new LatLng(latitude, longitude));
            Log.d("TAG", "latitude: " + latitude + ", longitude: " + longitude);
        };
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 7000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 7000, 0, locationListener);
    }

    public static String getLastKnownLocation() {
        @SuppressLint("MissingPermission") Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        @SuppressLint("MissingPermission") Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (latitude == 0.0 && locationGPS != null) {
            MyLocationListener.latitude = locationGPS.getLatitude();
            MyLocationListener.longitude = locationGPS.getLatitude();
        }
        if (longitude == 0.0 && locationGPS != null) {
            MyLocationListener.latitude = locationNet.getLatitude();
            MyLocationListener.longitude = locationNet.getLatitude();
        }
        return latitude + "/" + longitude;
    }
}
