package ru.newlevel.hordemap;

import static androidx.core.content.ContextCompat.startForegroundService;

import static ru.newlevel.hordemap.DataUpdateService.getAllGeoDataFromDatabase;
import static ru.newlevel.hordemap.DataUpdateService.sendGeoDataToDatabase;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class MyWakefulReceiver extends BroadcastReceiver {
    public MyWakefulReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sendGeoDataToDatabase(User.getInstance().getDeviceId(), User.getInstance().getUserName(), DataUpdateService.getLatitude(), DataUpdateService.getLongitude());
        getAllGeoDataFromDatabase();

        Log.d("Horde map", "Запустился MyWakefulReceiver ");
        MyServiceUtils.startAlarmManager(context);
    //    Intent service = new Intent(context, DataUpdateService.class);
    //    ContextCompat.startForegroundService(context, service);
    }
}