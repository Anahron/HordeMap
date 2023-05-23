package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmManagerReceiver extends BroadcastReceiver {

    public AlarmManagerReceiver() {
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        MapsActivity.getViewModel().sendMarkerData(DataUpdateService.getLatitude(), DataUpdateService.getLongitude());
        Log.d("Horde map", "Запустился AlarmManagerReceiver ");
    }
}