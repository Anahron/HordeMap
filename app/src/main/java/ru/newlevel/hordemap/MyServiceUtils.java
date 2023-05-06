package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.getInstance;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class MyServiceUtils {

    public static AlarmManager alarmMgr;
    public static PendingIntent pendingIntent;

    @SuppressLint("ShortAlarm")
    public static void startAlarmManager(Context context) {
        Log.d("Horde map", "Запустился Аларм Менеджер " + getInstance());
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        Intent intent = new Intent(context, MyWakefulReceiver.class);
        intent.setAction("com.newlevel.ACTION_SEND_DATA");
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 25000, pendingIntent);
    //    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 25000, pendingIntent);
        Log.d("Horde map", "Аларм менеджер отработал " + getInstance());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification createNotification(Context context) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel("CHANNEL_1", "GPS", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CHANNEL_1")
                .setSmallIcon(R.mipmap.hordecircle_round)
                .setContentTitle("Horde Map")
                .setContentText("Приложение работает в фоновом режиме")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(200);
        return builder.build();
    }
    public static void destroyAlarmManager(){
        if (pendingIntent != null) {
            Log.d("Horde map", "Аларм менеджер Остановлен в методе onDestroy");
            MyServiceUtils.alarmMgr.cancel(pendingIntent);
        }
    }
}
