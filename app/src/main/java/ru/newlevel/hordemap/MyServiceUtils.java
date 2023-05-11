package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.GeoUpdateService.getInstance;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class MyServiceUtils {

    public static AlarmManager alarmMgr;
    public static PendingIntent pendingIntent;
    public static final int NOTIFICATION_ID = 1;

    @SuppressLint("ShortAlarm")
    public static void startAlarmManager(Context context) {
        Log.d("Horde map", "Запустился Аларм Менеджер " + getInstance());
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        Intent intent = new Intent(context, MyWakefulReceiver.class);
        intent.setAction("com.newlevel.ACTION_SEND_DATA");
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 25000, pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification createNotification(Context context) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

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
    public static void checkAndStartForeground(GeoUpdateService geoUpdateService) {
        System.out.println("В метод checkAndStartForeground пришло : " + geoUpdateService);
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
            geoUpdateService.startForeground(NOTIFICATION_ID, MyServiceUtils.createNotification(MapsActivity.getContext()));
        } else {
            Log.d("Horde map", "Сервис startForeground уже запущен");
        }
    }

    public static void startGeoUpdateService(Context context) {
        MapsActivity.permissionForGeoUpdate = true;
        MarkersHandler.isMarkersON = true;
        GeoUpdateService sender = GeoUpdateService.getInstance();
        sender.exchangeGPSData();//обновление списка координат сразу после запуска не дожидаясь алармменеджера
        Intent service = new Intent(context, GeoUpdateService.class);
        service.setAction("com.newlevel.ACTION_SEND_DATA");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
            MyServiceUtils.startAlarmManager(context);
        }
    }

    public static void stopGeoUpdateService(Context context) {
        MarkersHandler.markersOff();
        MapsActivity.permissionForGeoUpdate = false;
        Intent intent = new Intent(context, GeoUpdateService.class);
        if (MyServiceUtils.alarmMgr != null)
            MyServiceUtils.alarmMgr.cancel(MyServiceUtils.pendingIntent);
        context.stopService(intent);
    }
}
