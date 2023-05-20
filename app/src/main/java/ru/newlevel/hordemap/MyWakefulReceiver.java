package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MyServiceUtils.ACTION_FOREGROUND_SERVICE;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class MyWakefulReceiver extends BroadcastReceiver {
    public MyWakefulReceiver() {
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        MapsActivity.getViewModel().sendMarkerData(DataUpdateService.getLatitude(), DataUpdateService.getLongitude());
        MapsActivity.getViewModel().checkForNewMessages();
        Log.d("Horde map", "Запустился MyWakefulReceiver ");
        MyServiceUtils.startAlarmManager(context);
        Intent service = new Intent(context, DataUpdateService.class);
        service.setAction(ACTION_FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.println("Запускаем  context.startForegroundService(service) из BroadcastReceiver");
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}