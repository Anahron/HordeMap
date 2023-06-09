package ru.newlevel.hordemap;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import static ru.newlevel.hordemap.MapsActivity.TIME_TO_SEND_DATA;
import static ru.newlevel.hordemap.MapsActivity.getViewModel;
import static ru.newlevel.hordemap.MapsActivity.isInactive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

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

    public static List<LatLng> locationHistory = new ArrayList<>();
    private Location location;
    private final Location[] lastLocation = {null};

    private final static int UPDATE_INTERVAL = 8000;
    private final static int FASTEST_INTERVAL = 5000;
    private final static int DISPLACEMENT = 8;

    private final static int DISPLACEMENT_INACTIVE = 2;

    public int NOTIFICATION_ID_MESSAGE = 2;
    private int countNewMessages = 0;
    private boolean isLocationActive = false;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private Runnable sendGeoTimer;
    private Handler handler;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;

    public static ArrayList<Long> sendTimesList = new ArrayList<>();

    public static synchronized DataUpdateService getInstance() {
        if (instance == null) {
            instance = new DataUpdateService();
        }
        return instance;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopDataUpdateService();
        stopLocationUpdates();
        alarmManager.cancel(pendingIntent);
        fusedLocationClient = null;
        locationRequest = null;
        locationCallback = null;
        handler = null;
        sendGeoTimer = null;
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createAlarmManager();
        instance = this;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        configureLocationRequestForActiveState();
        handler = new Handler();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification createNotification() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 9990, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel("CHANNEL_1", "GPS", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_1").setSmallIcon(R.mipmap.hordecircle_round).setContentTitle("Horde Map").setContentText("Horde Map получает GPS данные в фоновом режиме").setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pendingIntent).setAutoCancel(true).setCategory(Notification.CATEGORY_SERVICE).setTimeoutAfter(500);

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
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        int NOTIFICATION_ID = 1;
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == NOTIFICATION_ID) {
                isNotificationShow = true;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isNotificationShow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            super.onStartCommand(intent, flags, startId);
            if (locationCallback == null) locationCallback = new LocationCallback() {
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
                            Log.d("Horde map", location.getLatitude() + " " + location.getLongitude() + "   " + location.getAccuracy() + "   получены координаты ");
                            if (location.getAccuracy() < 30) {
                                myOnLocationResult();
                            } else if (lastLocation[0].getAccuracy() > location.getAccuracy()) {
                                lastLocation[0] = location;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Log.d("Horde map", " Запускаем locationCallback в onStartCommand");
            startLocationUpdates();
            startSendGeoHandler(TIME_TO_SEND_DATA);
        }
        return START_STICKY;
    }

    private void myOnLocationResult() {
        Log.d("Horde map", "Аккуратность < 30, проверяем на растояние");
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        MapsActivity.getViewModel().sendMarkerData(latitude, longitude);
        if (isInactive) {
            sendTimesList.add(System.currentTimeMillis());
        }
        if (SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(lastLocation[0].getLatitude(), lastLocation[0].getLongitude())) > 8) {    // Проверяем если растояние меньше 8 метров межу последней точкой и полученой - не добавляем
            Log.d("Horde map", "Растояние > 8 добавляем в locationHistory и в lastLocation");
            locationHistory.add(0, new LatLng(latitude, longitude));
            // Отправляем данные
            Log.d("Horde map", "В locationHistory добавлено " + latitude + " " + longitude);
            if (locationHistory.size() > 2)
                // проверка прошлой точки на неликвидность
                checkLastLocationForError();
        } else {
            Log.d("Horde map", "Растояние < 8 метров, пропускаем");
        }
        lastLocation[0] = location;
    }


    private void startLocationUpdates() {
        if (!isLocationActive) {
            isLocationActive = true;
            Log.d("Horde map", " Запустили locationCallback");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void stopLocationUpdates() {
        if (isLocationActive) {
            isLocationActive = false;
            Log.d("Horde map", "выключаем locationCallback");
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void startSendGeoHandler(long updateInterval) {
        sendGeoTimer = new Runnable() {
            @Override
            public void run() {
                alarmManager.cancel(pendingIntent);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + updateInterval + 5000, pendingIntent);
                getViewModel().checkForNewMessages();
                handler.removeCallbacks(sendGeoTimer);
                handler.postDelayed(this, updateInterval);
            }
        };
        handler.post(sendGeoTimer);
    }

    protected void switchToInactiveState() {
        if (fusedLocationClient != null) {
            Log.d("Horde map", "переключили на инактив");
            stopLocationUpdates();
            configureLocationRequestForInactiveState();
            startLocationUpdates();
        }
    }

    protected void switchToActiveState() {
        if (fusedLocationClient != null) {
            Log.d("Horde map", "переключили на актив");
            stopLocationUpdates();
            configureLocationRequestForActiveState();
            startLocationUpdates();
        }
    }

    private void configureLocationRequestForInactiveState() {
        locationRequest.setInterval(TIME_TO_SEND_DATA);
        locationRequest.setSmallestDisplacement(DISPLACEMENT_INACTIVE);
        locationRequest.setFastestInterval(TIME_TO_SEND_DATA);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void configureLocationRequestForActiveState() {
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void stopDataUpdateService() {
        Log.d("Horde map", "locationCallback остановлен");
        if (handler != null)
            handler.removeCallbacksAndMessages(null); // Удаляем все задачи из handler
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    protected void restartSendGeoTimer(long newDelayMillis) {
        Log.d("Horde map", "handler перезапущен");
        //Удаляем предыдущий запланированный Runnable
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        if (handler != null) handler.removeCallbacksAndMessages(null); // Удаляем задачи из handler
        startSendGeoHandler(newDelayMillis);
    }

    protected void showNewMessageNotification() {
        countNewMessages++;
        // Создание канала уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("CHANNEL_2", "Horde Message", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        // Создание интента для запуска активити при нажатии на уведомление
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 9992, intent, PendingIntent.FLAG_IMMUTABLE);

        // Создание уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_2").setSmallIcon(R.drawable.hordecircle).setContentTitle("Сообщение").setContentText("В чате новое сообщение" + "(" + countNewMessages + ")").setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pendingIntent).setAutoCancel(true);

        // Получение NotificationManager
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID_MESSAGE);
        }

        // Отображение уведомления
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_MESSAGE, builder.build());
        }
    }

    private void createAlarmManager() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmManagerReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
    }
}