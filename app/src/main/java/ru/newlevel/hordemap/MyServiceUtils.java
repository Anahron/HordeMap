package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import java.util.UUID;

public class MyServiceUtils {

    @SuppressLint("SuspiciousIndentation")
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
    }

    public static void stopGeoUpdateService() {
        MapsActivity.permissionForGeoUpdate = false;
        MarkersHandler.markersOff();
        MapsActivity.getViewModel().stopLoadGeoData();
        MapsActivity.getViewModel().stopLoadMessages();
        DataUpdateService.getInstance().stopDataUpdateService();
    }

    public static String getDeviceId(Context context) {
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null) {
            return androidId;
        } else {
            return UUID.randomUUID().toString();
        }
    }
}
