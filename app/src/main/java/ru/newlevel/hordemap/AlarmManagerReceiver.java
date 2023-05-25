package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MapsActivity.TIME_TO_SEND_DATA;

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
        DataUpdateService.getInstance().restartSendGeoTimer(TIME_TO_SEND_DATA);
        Log.d("Horde map", "Запустился AlarmManagerReceiver ");
    }
}