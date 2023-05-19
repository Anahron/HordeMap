package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataUpdateService.getInstance;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

public class MyServiceUtils {

    public static AlarmManager alarmMgr;
    public static PendingIntent pendingIntent;
    public static final int NOTIFICATION_ID = 1;
    private static Intent intentMyWakefulReceiver;
    public static final String ACTION_FOREGROUND_SERVICE = "com.example.ACTION_FOREGROUND_SERVICE";


    @SuppressLint("ShortAlarm")
    public static void startAlarmManager(Context context) {
        Log.d("Horde map", "Запустился Аларм Менеджер " + getInstance());
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        if (intentMyWakefulReceiver == null)
            intentMyWakefulReceiver = new Intent(context, MyWakefulReceiver.class);
        if (pendingIntent == null)
            pendingIntent = PendingIntent.getBroadcast(context, 9991, intentMyWakefulReceiver, PendingIntent.FLAG_IMMUTABLE);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 25000, pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification createNotification(Context context) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 9990, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel("CHANNEL_1", "GPS", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CHANNEL_1")
                .setSmallIcon(R.mipmap.hordecircle_round)
                .setContentTitle("Horde Map")
                .setContentText("Horde Map получает GPS данные в фоновом режиме")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(500);
        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void checkAndStartForeground(DataUpdateService dataUpdateService) {
        System.out.println("В метод checkAndStartForeground пришло : " + dataUpdateService);
        NotificationManager notificationManager = MapsActivity.getContext().getSystemService(NotificationManager.class);

        boolean notificationDisplayed = false;
        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
            if (statusBarNotification.getId() == NOTIFICATION_ID) {
                notificationDisplayed = true;
                break;
            }
        }
        if (!notificationDisplayed) {
            Log.d("Horde map", "Запустили сервис startForeground");
            dataUpdateService.startForeground(NOTIFICATION_ID, MyServiceUtils.createNotification(dataUpdateService.getBaseContext()));
        } else {
            Log.d("Horde map", "Сервис startForeground уже запущен");
        }
    }

    public static void startGeoUpdateService(Context context) {
        MapsActivity.permissionForGeoUpdate = true;
        MarkersHandler.isMarkersON = true;
        Intent service = new Intent(context, DataUpdateService.class);
        service.setAction(ACTION_FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.println("Запускаем  context.startForegroundService(service);");
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
        MyServiceUtils.startAlarmManager(context);
    }

    public static void stopGeoUpdateService(Context context) {
        MarkersHandler.markersOff();
        MapsActivity.permissionForGeoUpdate = false;
        Intent intent = new Intent(context, DataUpdateService.class);
        if (MyServiceUtils.alarmMgr != null)
            MyServiceUtils.alarmMgr.cancel(MyServiceUtils.pendingIntent);
        context.stopService(intent);
    }


    public static String getDeviceId(Context context) {
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null) {
            return androidId;
        } else {
            try {
                AdvertisingIdClient.Info advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                return advertisingIdInfo.getId();
            } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                return UUID.randomUUID().toString();
            }
        }
    }
}
