package ru.newlevel.hordemap;

import static android.content.Context.MODE_PRIVATE;
import static ru.newlevel.hordemap.GeoUpdateService.context;
import static ru.newlevel.hordemap.MapsActivity.name;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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
    static String answer = "";

    private static void createLogInDialog(Context context) {
        logOut(context);
        final String[] phoneNumber = new String[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Авторизация");
        builder.setMessage("Введите номер телефона \nформата '891312341212' \nили идентификатор");
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        builder.setPositiveButton("ОТПРАВИТЬ", (dialog, which) -> {
            phoneNumber[0] = input.getText().toString().trim();
            System.out.println(phoneNumber[0]);
            Thread thread = new Thread(() -> getLoginAccessFromServer(phoneNumber[0]));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Toast.makeText(context, "Получен логин " + answer, Toast.LENGTH_SHORT).show();
            if (answer.equals("404") || answer.equals("")) {
                System.out.println("Авторизация не пройдена");
                Toast.makeText(context, "Авторизация НЕ пройдена, обмен гео данными запрещен", Toast.LENGTH_LONG).show();
                MapsActivity.permission = false;
                MarkersHandler.markersOff();
                Intent intent = new Intent(context, GeoUpdateService.class);
                context.stopService(intent);
            } else {
                System.out.println("Авторизация пройдена");
                MapsActivity.name = answer;
                MapsActivity.id = Long.parseLong(phoneNumber[0]);
                Toast.makeText(context, "Авторизация пройдена, привет " + MapsActivity.name, Toast.LENGTH_LONG).show();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("name", name);
                editor.putLong("id", MapsActivity.id);
                editor.apply();
                MapsActivity.permission = true;
                startGeoUpdateService();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void logOut(Context context) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        MapsActivity.id = 0L;
        name = "name";
        MapsActivity.permission = false;
        MarkersHandler.markersOff();
        Intent intent = new Intent(context, GeoUpdateService.class);
        context.stopService(intent);
    }

    public static void logIn(Context context, MapsActivity mapsActivity) {
        prefs = context.getSharedPreferences("HordePref", MODE_PRIVATE);
        long mySavedID = prefs.getLong("id", 0L);
        String mySavedName = prefs.getString("name", "name");

        if (mySavedID == 0L || mySavedName.equals("name") || mySavedName.equals("")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle("Раскрытие информации.");
            builder.setMessage("Для корректной работы приложения требуется собирать Ваши данные о местоположении для работы функции обмена геоданными и построения маршрута пройденого пути, в том числе в фоновом режиме, даже если приложение закрыто и не используется. Мы не передаем данные о вашем местоположении третьим лицам и используем их только внутри нашего приложения, в том числе для передачи другим пользователям.");

            builder.setPositiveButton("Я ПОНИМАЮ", (dialog, which) -> {
                createLogInDialog(context);
                mapsActivity.setPermission();
            });

            builder.setNegativeButton("Отказываюсь", (dialog, which) -> {
                mapsActivity.finish();
            });
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            MapsActivity.id = mySavedID;
            MapsActivity.name = mySavedName;
            startGeoUpdateService();
            MapsActivity.permission = true;
            Toast.makeText(context, "Авторизация пройдена, привет " + MapsActivity.name, Toast.LENGTH_LONG).show();
        }

    }

    private static void getLoginAccessFromServer(String phonenumber) {
        Socket clientSocket = new Socket();
        try {
            clientSocket.connect(new InetSocketAddress(GeoUpdateService.ipAdress, GeoUpdateService.port), 10000);
        } catch (IOException e) {
            e.printStackTrace();
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Соединение не установлено", Toast.LENGTH_LONG).show());
        }
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream();
             PrintWriter writer = new PrintWriter(outputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            writer.println(phonenumber);
            writer.flush();
            System.out.println("Запрос на авторизацию отправлен: " + phonenumber);
            String tempAnswer;
            if ((tempAnswer = reader.readLine()) != null)
                answer = tempAnswer;
            System.out.println(answer + " полученый ответ");
        } catch (IOException e) {
            e.printStackTrace();
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Соединение не установлено", Toast.LENGTH_LONG).show());
        }
    }

    public static void startGeoUpdateService() {
        System.out.println("Запущено выполнение фонового сбора и отправки данных");
        MarkersHandler.isMarkersON = true;
        GeoUpdateService sender = GeoUpdateService.getInstance();
        sender.exchangeGPSData();//обновление списка координат сразу после запуска не дожидаясь алармменеджера
        Intent service = new Intent(context, GeoUpdateService.class);
        service.setAction("com.newlevel.ACTION_SEND_DATA");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        }
    }
}