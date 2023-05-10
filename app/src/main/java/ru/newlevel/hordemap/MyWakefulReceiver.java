package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class MyWakefulReceiver extends WakefulBroadcastReceiver {
    public MyWakefulReceiver() {
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Horde map", "Запустился MyWakefulReceiver ");
        MyServiceUtils.startAlarmManager(context);
        Intent service = new Intent(context, GeoUpdateService.class);
        service.setAction("com.newlevel.ACTION_SEND_DATA");
        ContextCompat.startForegroundService(context, service);
        MyWakefulReceiver.completeWakefulIntent(intent);
    }
}