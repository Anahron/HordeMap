package ru.newlevel.hordemap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;

public class LoginRequest {
    private static SharedPreferences prefs;

    static void onLoginFailure(Context context) {
        MapsActivity.makeToast("Авторизация НЕ пройдена, обмен гео данными запрещен");
        MyServiceUtils.stopGeoUpdateService(context);
        MapsActivity.imageButton.setClickable(false);
    }

    static void onLoginSuccess(Context context) {
        MapsActivity.makeToast("Авторизация пройдена, привет " + User.getInstance().getUserName());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", User.getInstance().getUserName());
        editor.putString("id", User.getInstance().getUserId());
        editor.apply();
        MyServiceUtils.startGeoUpdateService(context);
        MapsActivity.imageButton.setClickable(true);
    }

    public static void logOut(Context context) {
        MyServiceUtils.stopGeoUpdateService(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        User.getInstance().setUserName("name");
        User.getInstance().setUserId("0");
        MapsActivity.imageButton.setClickable(false);
    }

    public static void logIn(Context context, MapsActivity mapsActivity) {
        prefs = context.getSharedPreferences("HordePref", MODE_PRIVATE);
        String mySavedID = prefs.getString("id", "0");
        String mySavedName = prefs.getString("name", "name");
        FirebaseApp.initializeApp(context);
        if (mySavedID.equals("0") || mySavedName.equals("name") || mySavedName.equals("")) {
            InfoDialogFragment dialogFragment = new InfoDialogFragment(context, mapsActivity);
            dialogFragment.show(mapsActivity.getSupportFragmentManager(), "info_dialog");
        } else {
            User.getInstance().setUserName(mySavedName);
            User.getInstance().setUserId(mySavedID);
            MyServiceUtils.startGeoUpdateService(context);
            MapsActivity.imageButton.setClickable(true);
            MapsActivity.makeToast("Авторизация пройдена, привет " + mySavedName);
        }
    }
}