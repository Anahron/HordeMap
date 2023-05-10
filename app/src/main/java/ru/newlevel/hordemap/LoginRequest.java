package ru.newlevel.hordemap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class LoginRequest {
    private static SharedPreferences prefs;
    private static Long id = 0L;
    private static String name;

    public static Long getId() {
        return id;
    }

    public static String getName() {
        return name;
    }

    static void onLoginFailure(Context context) {
        MapsActivity.makeToast("Авторизация НЕ пройдена, обмен гео данными запрещен");
        stopGeoUpdateService(context);
    }

    static void onLoginSuccess(String phoneNumber, String answer, Context context) {
        name = answer;
        id = Long.parseLong(phoneNumber);
        MapsActivity.makeToast("Авторизация пройдена, привет " + name);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putLong("id", id);
        editor.apply();
        startGeoUpdateService(context);
    }

    public static void logOut(Context context) {
        stopGeoUpdateService(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        id = 0L;
        name = "name";
    }

    public static void logIn(Context context, MapsActivity mapsActivity) {
        prefs = context.getSharedPreferences("HordePref", MODE_PRIVATE);
        long mySavedID = prefs.getLong("id", 0L);
        String mySavedName = prefs.getString("name", "name");

        if (mySavedID == 0L || mySavedName.equals("name") || mySavedName.equals("")) {
            InfoDialogFragment dialogFragment = new InfoDialogFragment(context, mapsActivity);
            dialogFragment.show(mapsActivity.getSupportFragmentManager(), "info_dialog");
        } else {
            id = mySavedID;
            name = mySavedName;
            startGeoUpdateService(context);
            MapsActivity.permissionForGeoUpdate = true;
            MapsActivity.makeToast("Авторизация пройдена, привет " + name);
        }
    }

    public static void startGeoUpdateService(Context context) {
        MapsActivity.permissionForGeoUpdate = true;
        MarkersHandler.isMarkersON = true;
        GeoUpdateService sender = GeoUpdateService.getInstance();
        sender.exchangeGPSData();//обновление списка координат сразу после запуска не дожидаясь алармменеджера
        Intent service = new Intent(context, GeoUpdateService.class);
        service.setAction("com.newlevel.ACTION_SEND_DATA");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        }
    }

    private static void stopGeoUpdateService(Context context) {
        MarkersHandler.markersOff();
        MapsActivity.permissionForGeoUpdate = false;
        Intent intent = new Intent(context, GeoUpdateService.class);
        if (MyServiceUtils.alarmMgr != null)
            MyServiceUtils.alarmMgr.cancel(MyServiceUtils.pendingIntent);
        context.stopService(intent);
    }
}