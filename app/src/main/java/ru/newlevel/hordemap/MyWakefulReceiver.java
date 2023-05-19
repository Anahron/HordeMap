package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
    }
}