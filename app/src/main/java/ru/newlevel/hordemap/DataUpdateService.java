package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MyServiceUtils.ACTION_FOREGROUND_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class DataUpdateService extends Service {

    @SuppressLint("StaticFieldLeak")
    private static DataUpdateService instance = null;

    private static double latitude;
    private static double longitude;

    public static List<LatLng> locationHistory = new ArrayList<>();
    private static boolean requestingLocationUpdates = false;

    private static Location location;
    private static final Location[] lastLocation = {null};

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 2001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 2002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 2003;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION = 2004;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 2005;
    private static final int MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS = 2006;
    private static final int REQUEST_CODE_FOREGROUND_SERVICE = 2012;

    private final static int UPDATE_INTERVAL = 3000;
    private final static int FASTEST_INTERVAL = 2000;
    private final static int DISPLACEMENT = 3;

    public static final int NOTIFICATION_ID = 1;

    public static synchronized DataUpdateService getInstance() {
        if (instance == null) {
            instance = new DataUpdateService();
        }
        return instance;
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("BatteryLife")
    protected void setPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), permissions, MY_PERMISSIONS_REQUEST_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.FOREGROUND_SERVICE}, REQUEST_CODE_FOREGROUND_SERVICE);
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission_group.SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission_group.SENSORS}, MY_PERMISSIONS_REQUEST_SENSOR);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.WAKE_LOCK}, MY_PERMISSIONS_REQUEST_WAKE_LOCK);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.SCHEDULE_EXACT_ALARM}, MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification createNotification() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 9990, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel("CHANNEL_1", "GPS", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_1")
                .setSmallIcon(R.mipmap.hordecircle_round)
                .setContentTitle("Horde Map")
                .setContentText("Horde Map получает GPS данные в фоновом режиме")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(500);
        return builder.build();
    }

    private void checkLastLocationForError() {
        LatLng firstLatLng = locationHistory.get(0);
        LatLng secondLatLng = locationHistory.get(1);
        LatLng thirdLatLng = locationHistory.get(2);
        double distance1 = SphericalUtil.computeDistanceBetween(firstLatLng, secondLatLng);   // до 2 = 11
        Log.d("Horde map", "Дистанция до прошлой: " + distance1);
        double distance2 = SphericalUtil.computeDistanceBetween(firstLatLng, thirdLatLng);    // до 3 = 14
        Log.d("Horde map", "Дистанция до позапрошлой: " + distance2);
        if (distance2 < distance1) {
            Log.d("Horde map", "координата 1 удалена");
            locationHistory.remove(1);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_FOREGROUND_SERVICE.equals(intent.getAction())) {
            System.out.println("onStartCommand в ACTION_FOREGROUND_SERVICE вызвана");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForeground(NOTIFICATION_ID, createNotification());
            }
            setPermission();
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(UPDATE_INTERVAL);
            locationRequest.setFastestInterval(FASTEST_INTERVAL);
            locationRequest.setSmallestDisplacement(DISPLACEMENT);
            super.onStartCommand(intent, flags, startId);
            LocationCallback locationCallback = new LocationCallback() {
                @SuppressLint("SuspiciousIndentation")
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    location = locationResult.getLastLocation();
                    try {
                        if (location != null) {
                            if (lastLocation[0] == null) {
                                lastLocation[0] = location;
                            }
                            Log.d("Horde map", location.getLatitude() + " " + location.getLongitude() + "   " + location.getAccuracy() + "   получены координаты " + this);
                            if (location.getAccuracy() < 25) {
                                Log.d("Horde map", "Аккуратность < 25, проверяем на растояние");
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                if (SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(lastLocation[0].getLatitude(), lastLocation[0].getLongitude())) > 8) {    // Проверяем если растояние меньше 8 метров межу последней точкой и полученой - не добавляем
                                    Log.d("Horde map", "Растояние > 8 добавляем в locationHistory и в lastLocation");
                                    locationHistory.add(0, new LatLng(latitude, longitude));
                                    // Отправляем данные
                                    MapsActivity.getViewModel().sendMarkerData(latitude, longitude);
                                    Log.d("Horde map", "В locationHistory добавлено " + latitude + " " + longitude);
                                    if (locationHistory.size() > 2)
                                        // проверка прошлой точки на неликвидность
                                        checkLastLocationForError();
                                } else {
                                    Log.d("Horde map", "Растояние < 8 метров, пропускаем");
                                }
                                lastLocation[0] = location;
                            } else if (lastLocation[0].getAccuracy() > location.getAccuracy()) {
                                lastLocation[0] = location;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            if (latitude == 0.0) {
                Log.d("Horde map", " Координаты 0.0 выключаем слушатель");
                requestingLocationUpdates = false;
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }

            if (!requestingLocationUpdates) {
                requestingLocationUpdates = true;
                Log.d("Horde map", " Запустили locationCallback");
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }
        return START_STICKY;
    }
}