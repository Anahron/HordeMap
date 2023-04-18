package ru.newlevel.hordemap;

import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DataSender implements Runnable {
    private static ArrayList geoList;
    private String serverUrl;
    private HashMap<Integer, String> gpsList = new HashMap();
    ;
    private Integer id;
    private String name;
    private double latitude;
    private double longitude;
    private Gson gson = new Gson();

    public DataSender(int id, String name) {
        this.id = id;
        this.name = name;

    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void run() {
        try {
            // Формируем запрос. Макет запроса id:name:latitude:longitude
            String post = id + ":" + name + ":" + latitude + ":" + longitude;
            System.out.println("Запрос серверу: " + post);

            // Создаем сокет на порту 8080
            Socket clientSocket = new Socket("localhost", 8080);

            // Получаем входной и выходной потоки для обмена данными с сервером
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // Отправляем запрос серверу
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(post);
            writer.flush();

            // Читаем данные из входного потока
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.readLine();

            // Определяем тип данных, в которые нужно преобразовать JSON-строку
            Type type = new TypeToken<HashMap<Integer, String>>() {
            }.getType();
            // Преобразуем JSON-строку в HashMap
            HashMap<Integer, String> hashMap = gson.fromJson(json, type);
            // Закрываем соединение с клиентом
            clientSocket.close();

        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}