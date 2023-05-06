package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.sender;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class MyWakefulReceiver extends BroadcastReceiver {
    public MyWakefulReceiver() {
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Horde map", "Запустился метод MyWakefulReceiver " + this + " -  получает координаты и вызывает sendData в новом потоке");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService::MyWakeLock");
        wakeLock.acquire(10000L /*1 minutes*/);
        Intent service = new Intent(context, DataSender.class);
        service.setAction("com.newlevel.ACTION_SEND_DATA");
        ContextCompat.startForegroundService(context, service);
        setResultCode(Activity.RESULT_OK);
        Thread thread = new Thread(() -> sender.sendGPS());
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wakeLock.release();
    }
}