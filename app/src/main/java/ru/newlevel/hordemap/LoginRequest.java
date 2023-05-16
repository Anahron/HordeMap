package ru.newlevel.hordemap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;

public class LoginRequest {
    private static SharedPreferences prefs;

    static void onLoginSuccess(Context context) {
        MapsActivity.makeToast("Авторизация пройдена, привет " + User.getInstance().getUserName());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", User.getInstance().getUserName());
        editor.putString("roomID", User.getInstance().getRoomId());
        editor.putString("deviceID", User.getInstance().getDeviceId());
        editor.apply();
        MyServiceUtils.startGeoUpdateService(context);
        MapsActivity.MessengerButton.setClickable(true);
    }

    public static void logOut(Context context) {
        MyServiceUtils.stopGeoUpdateService(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        User.getInstance().setUserName("Аноним");
        User.getInstance().setRoomId("0");
        MapsActivity.MessengerButton.setClickable(false);
    }

    public static void logIn(Context context, MapsActivity mapsActivity) {
        prefs = context.getSharedPreferences("HordePref", MODE_PRIVATE);
        String mySavedID = prefs.getString("roomID", "0");
        String mySavedName = prefs.getString("name", "Аноним");
        String mySavedDeviceID = prefs.getString("deviceID", "0");
        FirebaseApp.initializeApp(context);
        if (mySavedName.equals("name") || mySavedName.equals("") || mySavedDeviceID.equals("0")) {
            InfoDialogFragment dialogFragment = new InfoDialogFragment(context, mapsActivity);
            dialogFragment.show(mapsActivity.getSupportFragmentManager(), "info_dialog");
        } else {
            User.getInstance().setUserName(mySavedName);
            User.getInstance().setRoomId(mySavedID);
            User.getInstance().setDeviceId(mySavedDeviceID);
            MyServiceUtils.startGeoUpdateService(context);
            MapsActivity.MessengerButton.setClickable(true);
            MapsActivity.makeToast("Авторизация пройдена, привет " + mySavedName);
        }
    }
}