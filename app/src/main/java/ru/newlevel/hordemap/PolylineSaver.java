package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolylineSaver {
    public static final String SHARED_PREFS_NAME = "HordePref"; // имя файла SharedPreferences

    // метод для сохранения списка в SharedPreferences
    public void saveListToSharedPreferences(Context context, List<LatLng> list, String list_id) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(list_id, json);
        editor.apply();
    }


    public List<LatLng> loadListFromSharedPreferences(Context context, String list_id) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME , Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = null;
        Type type = new TypeToken<List<LatLng>>(){}.getType();
        try {
            json = sharedPrefs.getString(list_id, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert type != null;
        return gson.fromJson(json, type);
    }

    public List<String> getKeys(Context context, String oldTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        Map<String, ?> allEntries = sharedPreferences.getAll();
        List<String> matchingKeys = new ArrayList<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Проверяем, что значение является датой в формате строки
            if (value instanceof String) {
                String dateString = (String) value;
                LocalDateTime storedDateTime = null;
                LocalDateTime oldDataTime = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    storedDateTime = LocalDateTime.parse(dateString);
                    oldDataTime = LocalDateTime.parse((dateString));
                }
                // Сравниваем дату из записи со временем LocalDateTime
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (storedDateTime.isEqual(oldDataTime)) {
                        matchingKeys.add(key);
                    }
                }
            }
        }

        // Выводим список найденных ключей
        for (String key : matchingKeys) {
            System.out.println(key);
        }
        return matchingKeys;
    }

    public List<LatLng> getPalyline(String list_id) {
        SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        String value = prefs.getString("my_key", null);
        Gson gson = new Gson();
        String json = null;
        Type listType = new TypeToken<List<LatLng>>(){}.getType();
        try {
            json = prefs.getString(list_id, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<LatLng> list = gson.fromJson(json, listType);

        return list;
    }

}
