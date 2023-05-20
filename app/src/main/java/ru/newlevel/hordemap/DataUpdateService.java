package ru.newlevel.hordemap;

import static android.content.ContentValues.TAG;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static ru.newlevel.hordemap.MyServiceUtils.ACTION_FOREGROUND_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;

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
    private final static int UPDATE_INTERVAL = 10000;
    private final static int FASTEST_INTERVAL = 8000;
    private final static int DISPLACEMENT = 5;

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

    private Handler handler;
    private Runnable sendGeoTimer;

    @Override
    public void onDestroy() {
      //  super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        boolean isNotificationShow = false;
        System.out.println("onStartCommand в ACTION_FOREGROUND_SERVICE вызвана");
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == NOTIFICATION_ID) {
                isNotificationShow = true;
                System.out.println("Нотификация показывает, ничего не делаем");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isNotificationShow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.startForeground(NOTIFICATION_ID, createNotification(), FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                this.startForeground(NOTIFICATION_ID, createNotification());
            }
            System.out.println("Нотификации НЕТ, запускаем startForeground");
            handler = new Handler();
            sendGeoTimer = new Runnable() {
                @Override
                public void run() {
                    System.out.println("отсылаются данные в handler'e ");
                    MapsActivity.getViewModel().sendMarkerData(DataUpdateService.getLatitude(), DataUpdateService.getLongitude());
                    handler.postDelayed(this, 30000);
                }
            };
            handler.post(sendGeoTimer);
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