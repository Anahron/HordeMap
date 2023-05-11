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
        MyServiceUtils.stopGeoUpdateService(context);
    }

    static void onLoginSuccess(String phoneNumber, String answer, Context context) {
        name = answer;
        id = Long.parseLong(phoneNumber);
        MapsActivity.makeToast("Авторизация пройдена, привет " + name);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putLong("id", id);
        editor.apply();
        MyServiceUtils.startGeoUpdateService(context);
    }

    public static void logOut(Context context) {
        MyServiceUtils.stopGeoUpdateService(context);
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
            MyServiceUtils.startGeoUpdateService(context);
            MapsActivity.makeToast("Авторизация пройдена, привет " + name);
        }
    }
}