package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;


import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.List;

public class PolylineSaver {


    // метод для сохранения списка в SharedPreferences
    public static void savePathList(Context context, List<LatLng> list, int meters) {
        System.out.println("вызван метод сохранения пути");
        if (meters > 10) {
            String formattedDateTime = null;
            LocalDateTime localDateTime = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                localDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                formattedDateTime = localDateTime.format(formatter);
            }
            Type type = new TypeToken<List<LatLng>>() {
            }.getType();
            Gson gson = new Gson();
            String json = gson.toJson(list);
            try {
                //     FileOutputStream outputStream = new FileOutputStream(file, true);
                FileOutputStream outputStream = context.openFileOutput("gps.txt", Context.MODE_APPEND);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                outputStreamWriter.write(meters + " метров.\n");
                System.out.println(meters + " метров.");
                outputStreamWriter.write(formattedDateTime + "\n");
                System.out.println(formattedDateTime);
                outputStreamWriter.write(json + "\n");
                System.out.println(json + "\n");
                outputStreamWriter.close();
                outputStream.close();
                Toast.makeText(context, "Пусь сохранен успешно", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(context, "Путь не сохранен.", Toast.LENGTH_LONG).show();
        }
    }

    public static void deleteAll(Context context) {
        try {
            FileOutputStream outputStream = context.openFileOutput("gps.txt", Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write("");
            outputStreamWriter.close();
            outputStream.close();
            System.out.println("Типа успешно стерли файл");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Hashtable<String, List<LatLng>> getKeys() {
        System.out.println("Зашли в гетКейс");
        Hashtable<String, List<LatLng>> hash = new Hashtable<>();
        MapsActivity.savedLogsOfGPSpath.clear();
        try {
            System.out.println("Зашли в тру");
            FileInputStream inputStream = context.openFileInput("gps.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            Type type = new TypeToken<List<LatLng>>() {
            }.getType();
            Gson json = new Gson();
            int count = 0;
            String meters = "";
            String time = "";
            while ((line = reader.readLine()) != null) {
                System.out.println("Прочли линию:" + line);
                if (count == 0) {
                    System.out.println("Записали её в метры");
                    meters = line;
                    count++;
                } else if (count == 1) {
                    System.out.println("Записали её во время");
                    time = line;
                    count++;
                } else if (count == 2) {
                    System.out.println("Запиали в json");
                    json.fromJson(line, type);
                    System.out.println(json);
                    System.out.println(hash);
                    hash.put(meters + "" + time, json.fromJson(line, type));
                    System.out.println(hash);
                    count = 0;
                }
            }
            reader.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
