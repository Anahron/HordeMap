package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class MyWakefulReceiver extends BroadcastReceiver {
    public static boolean isInactive = false;

    public MyWakefulReceiver() {
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        MapsActivity.getViewModel().sendMarkerData(DataUpdateService.getLatitude(), DataUpdateService.getLongitude());
        if (!isInactive)
            MapsActivity.getViewModel().checkForNewMessages();
        Log.d("Horde map", "Запустился MyWakefulReceiver ");
        MyServiceUtils.startAlarmManager(context);
        Intent service = new Intent(context, DataUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.println("Запускаем  context.startForegroundService(service);");
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}