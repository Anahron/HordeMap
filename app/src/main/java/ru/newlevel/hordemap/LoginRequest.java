package ru.newlevel.hordemap;

import static android.content.Context.MODE_PRIVATE;
import static ru.newlevel.hordemap.GeoUpdateService.context;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LoginRequest {
    private static SharedPreferences prefs;
    private static String answer = "";
    private static Long id = 0L;
    private static String name;

    public static Long getId() {
        return id;
    }

    public static String getName() {
        return name;
    }

    static void checkLogIn(String phoneNumber) {
        Thread thread = new Thread(() -> getLoginAccessFromServer(phoneNumber));
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (answer.equals("404") || answer.equals("")) {
            onLoginFailure();
        } else {
            onLoginSuccess(phoneNumber);
        }
    }

    private static void onLoginFailure() {
        Toast.makeText(context, "Авторизация НЕ пройдена, обмен гео данными запрещен", Toast.LENGTH_LONG).show();
        stopGeoUpdateService(context);
    }

    private static void onLoginSuccess(String phoneNumber) {
        name = answer;
        id = Long.parseLong(phoneNumber);
        Toast.makeText(context, "Авторизация пройдена, привет " + name, Toast.LENGTH_LONG).show();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putLong("id", id);
        editor.apply();
        startGeoUpdateService();
    }

    public static void logOut(Context context) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        id = 0L;
        name = "name";
        stopGeoUpdateService(context);
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
            startGeoUpdateService();
            MapsActivity.permissionForGeoUpdate = true;
            Toast.makeText(context, "Авторизация пройдена, привет " + name, Toast.LENGTH_LONG).show();
        }

    }

    static void getLoginAccessFromServer(String phonenumber) {
        Socket clientSocket = new Socket();
        try {
            clientSocket.connect(new InetSocketAddress(GeoUpdateService.ipAdress, GeoUpdateService.port), 10000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream();
             PrintWriter writer = new PrintWriter(outputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            writer.println(phonenumber);
            writer.flush();
            String tempAnswer;
            if ((tempAnswer = reader.readLine()) != null)
                answer = tempAnswer;
        } catch (IOException e) {
            e.printStackTrace();
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Соединение не установлено", Toast.LENGTH_LONG).show());
        }
    }

    public static void startGeoUpdateService() {
        System.out.println("Запущено выполнение фонового сбора и отправки данных");
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