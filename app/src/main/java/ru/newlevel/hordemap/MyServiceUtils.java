package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataUpdateService.getInstance;
import static ru.newlevel.hordemap.MapsActivity.TIME_TO_SEND_DATA;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

public class MyServiceUtils {

    public static AlarmManager alarmMgr;
    public static PendingIntent pendingIntent;
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
        long timeInMillis = System.currentTimeMillis() + TIME_TO_SEND_DATA;
        System.out.println("аларм менеджер установится с " + TIME_TO_SEND_DATA);
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent);
        alarmMgr.setAlarmClock(alarmClockInfo, pendingIntent);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
    }


    public static void startGeoUpdateService(Context context) {
        MapsActivity.permissionForGeoUpdate = true;
        MarkersHandler.isMarkersON = true;
        Intent service = new Intent(context, DataUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.println("Запускаем  context.startForegroundService(service);");
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
        MapsActivity.getViewModel().loadGeoDataListener();
        MyServiceUtils.startAlarmManager(context);

    }

    public static void stopGeoUpdateService(Context context) {
        MapsActivity.permissionForGeoUpdate = false;
        MarkersHandler.markersOff();
        Intent intent = new Intent(context, DataUpdateService.class);
        if (MyServiceUtils.alarmMgr != null)
            MyServiceUtils.alarmMgr.cancel(MyServiceUtils.pendingIntent);
        context.stopService(intent);
        MapsActivity.getViewModel().stopLoadGeoData();
        MapsActivity.getViewModel().stopLoadMessages();
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
