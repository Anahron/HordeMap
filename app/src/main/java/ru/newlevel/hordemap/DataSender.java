package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MapsActivity.mMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class DataSender implements Runnable {
    private HashMap<Integer, String> gpsList = new HashMap();
    private static Long id;
    private static String ipAdress = "192.168.1.21";
    ;
    private static int port = 8000;
    private static String name;
    private static double latitude;
    private static double longitude;
    private Gson gson = new Gson();
    private static ArrayList<Marker> markers = new ArrayList<>();
    public static Context context;
    public static String answer;

    public DataSender(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public void setIpAdress(String ipAdress) {
        this.ipAdress = ipAdress;
    }

    public String getIpAdress() {
        return ipAdress;
    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void createMarkers(HashMap<Long, String> map) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.hordecircle);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, 60, 60, false));
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : markers) {
                    marker.remove();
                }
                markers.clear();
                for (Long id : map.keySet()) {
                    if (MapsActivity.id != id) {
                        System.out.println(map.get(id));
                        String[] data = Objects.requireNonNull(map.get(id)).split("/");
                        String dateTimeString = data[3].substring(11, 16);
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                                .title(data[0])
                                .snippet(dateTimeString)
                                .icon(icon));
                        // .icon(BitmapDescriptorFactory.fromResource(R.drawable.orc2))); // Задание изображения маркера
                        markers.add(marker);
                    }
                }
            }
        });
    }

    public static void getLoginAccess (String phonenumber) {
        // Создаем сокет на порту 8080
        Socket clientSocket = new Socket();
        DataSender.answer = "404";
        try {
            clientSocket.connect(new InetSocketAddress(ipAdress, port), 10000);
        } catch (IOException e) {
            e.printStackTrace();
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Соединение не установлено", Toast.LENGTH_LONG).show();
                }
            });
        }
        // Получаем входной и выходной потоки для обмена данными с сервером
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream();
             PrintWriter writer = new PrintWriter(outputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // Отправляем запрос серверу
            writer.println(phonenumber);
            writer.flush();
            System.out.println("Запрос на авторизацию отправлен: " + phonenumber);
            // Читаем данные из входного потока
            DataSender.answer = reader.readLine();
            System.out.println(answer + " полученый ответ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Формируем запрос. Макет запроса id:name:latitude:longitude
            String post = id + "/" + name + "/" + latitude + "/" + longitude;
            System.out.println("Запрос серверу: " + post);

            // Создаем сокет на порту 8080
            Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(ipAdress, 8000), 10000);

            // Получаем входной и выходной потоки для обмена данными с сервером
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // Отправляем запрос серверу
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(post);
            writer.flush();
            System.out.println("Запрос отправлен: " + post);

            // Читаем данные из входного потока
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.readLine();

            // Определяем тип данных, в которые нужно преобразовать JSON-строку
            Type type = new TypeToken<HashMap<Long, String>>() {
            }.getType();
            // Преобразуем JSON-строку в HashMap
            HashMap<Long, String> hashMap = gson.fromJson(json, type);
            System.out.println("Запрос получен: " + hashMap.toString());
            // Закрываем соединение с клиентом
            clientSocket.close();
            createMarkers(hashMap);

        } catch (Exception ex) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Соединение не установлено, проверьте подключение к интернету", Toast.LENGTH_LONG).show();
                }
            });
            ex.printStackTrace();
        }
    }
}