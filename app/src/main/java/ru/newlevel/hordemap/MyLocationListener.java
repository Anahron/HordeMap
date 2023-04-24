package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class MyLocationListener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1001;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1001;
    public static double latitude;
    public static double longitude;
    public static List<LatLng> locationHistory = new ArrayList<>();
    private static MyLocationListener instance = null;
    private static FusedLocationProviderClient fusedLocationClient;
    private static LocationCallback locationCallback;
    private static Location location;

    private MyLocationListener() {
    }

    public static MyLocationListener getInstance() {
        if (instance == null) {
            instance = new MyLocationListener();
        }
        return instance;
    }

    public static void startLocationListener() {
        final Location[] lastLocation = {null};
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setSmallestDisplacement(1);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                System.out.println(location.getLatitude() + "   " + location.getLongitude() + "  " + location.getAccuracy());
                if (location.getAccuracy() < 22) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    if (lastLocation[0] == null || location.distanceTo(lastLocation[0]) < 2000) {
                        locationHistory.add(0, new LatLng(latitude, longitude));
                        if (locationHistory.size() > 3) {
                            LatLng firstLatLng = locationHistory.get(0);
                            LatLng secondLatLng = locationHistory.get(1);
                            LatLng thirdLatLng = locationHistory.get(2);
                            LatLng fourthLatLng = locationHistory.get(3);
                            double distance1 = SphericalUtil.computeDistanceBetween(firstLatLng, secondLatLng);
                            System.out.println("Дистанция до прошлой: " + distance1);
                            double distance2 = SphericalUtil.computeDistanceBetween(firstLatLng, thirdLatLng);
                            System.out.println("Дистанция до позапрошлой: " + distance2);
                            double distance3 = SphericalUtil.computeDistanceBetween(firstLatLng, fourthLatLng);
                            System.out.println("Дистанция до предпоследней: " + distance3);
                            if (distance3 < distance2 && distance3 < distance1) {
                                // Удаляем среднюю точку из списка
                                System.out.println("координата 2 удалена");
                                locationHistory.remove(1);
                            } else if (distance2 < distance1) {
                                // Удаляем вторую точку из списка
                                System.out.println("координата 1 удалена");
                                locationHistory.remove(1);
                            }
                        }
                        System.out.println("В coordinates добавлено " + latitude + " " + longitude);
                        lastLocation[0] = location;
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
}