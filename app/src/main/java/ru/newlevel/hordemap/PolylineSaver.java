package ru.newlevel.hordemap;

import android.content.Context;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.List;

public class PolylineSaver {

    public static void savePathList(Context context, List<LatLng> list, int meters) {
        if (meters > 10) {
            String formattedDateTime = null;
            LocalDateTime localDateTime;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                localDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                formattedDateTime = localDateTime.format(formatter);
            }
            Gson gson = new Gson();
            String json = gson.toJson(list);
            try {
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
                Toast.makeText(context, "Путь сохранен успешно", Toast.LENGTH_LONG).show();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Hashtable<String, List<LatLng>> getKeys() {
        Hashtable<String, List<LatLng>> hash = new Hashtable<>();
        try {
            FileInputStream inputStream = MapsActivity.getContext().openFileInput("gps.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            Type type = new TypeToken<List<LatLng>>() {
            }.getType();
            Gson json = new Gson();
            int count = 0;
            String meters = "";
            String time = "";
            while ((line = reader.readLine()) != null) {
                if (count == 0) {
                    meters = line;
                    count++;
                } else if (count == 1) {
                    time = line;
                    count++;
                } else if (count == 2) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
