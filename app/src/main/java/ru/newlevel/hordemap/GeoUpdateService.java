package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.SphericalUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeoUpdateService extends Service {

    public static String ipAdress = "horde.krasteplovizor.ru";  // сервер
    public static int port = 49283; // сервер
    //  public static String ipAdress = "192.168.1.21";  //  локал
    //  public static int port = 8080; // локал
    @SuppressLint("StaticFieldLeak")
    private static GeoUpdateService instance = null;
    private static double latitude;
    private static double longitude;
    public static List<LatLng> locationHistory = new ArrayList<>();
    private static boolean requestingLocationUpdates = false;
    private static boolean isConnectionLost;

    private final static int UPDATE_INTERVAL = 3000;
    private final static int FASTEST_INTERVAL = 2000;
    private final static int DISPLACEMENT = 3;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locatonCallback;
    private static Location location;

    private static final Location[] lastLocation = {null};

    public static synchronized GeoUpdateService getInstance() {
        if (instance == null) {
            instance = new GeoUpdateService();
        }
        return instance;
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
        MyServiceUtils.startAlarmManager(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand вызвана");
        super.onStartCommand(intent, flags, startId);
        MyServiceUtils.checkAndStartForeground(this);
        locatonCallback = new LocationCallback() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                location = locationResult.getLastLocation();
                try {
                    if (location != null) {
                        Log.d("Horde map", location.getLatitude() + " " + location.getLongitude() + "   " + location.getAccuracy() + "   получены координаты" + this);
                        if (location.getAccuracy() < 25) {
                            Log.d("Horde map", "Аккуратность < 25, проверяем на растояние");
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            if (lastLocation[0] == null) {
                                lastLocation[0] = location;
                            }
                            if (SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(lastLocation[0].getLatitude(), lastLocation[0].getLongitude())) > 8) {    // Проверяем если растояние меньше 8 метров межу последней точкой и полученой - не добавляем
                                Log.d("Horde map", "Растояние > 8 добавляем в locationHistory и в lastLocation");
                                locationHistory.add(0, new LatLng(latitude, longitude));
                                if (locationHistory.size() > 2) {
                                    LatLng firstLatLng = locationHistory.get(0);
                                    LatLng secondLatLng = locationHistory.get(1);
                                    LatLng thirdLatLng = locationHistory.get(2);
                                    double distance1 = SphericalUtil.computeDistanceBetween(firstLatLng, secondLatLng);   // до 2 = 11
                                    Log.d("Horde map", "Дистанция до прошлой: " + distance1);
                                    double distance2 = SphericalUtil.computeDistanceBetween(firstLatLng, thirdLatLng);    // до 3 = 14
                                    Log.d("Horde map", "Дистанция до позапрошлой: " + distance2);
                                    if (distance2 < distance1) {
                                        Log.d("Horde map", "координата 1 удалена");
                                        locationHistory.remove(1);
                                    }
                                    lastLocation[0] = location;
                                    Log.d("Horde map", "В coordinates добавлено " + latitude + " " + longitude);
                                } else {
                                    Log.d("Horde map", "Растояние < 8 метров, пропускаем");
                                }
                            } else if (lastLocation[0].getAccuracy() > location.getAccuracy()) {
                                lastLocation[0] = location;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        if (latitude == 0.0) {
            Log.d("Horde map", " Координаты 0.0 выключаем слушатель");
            requestingLocationUpdates = false;
            fusedLocationClient.removeLocationUpdates(locatonCallback);
        }

        if (!requestingLocationUpdates) {
            requestingLocationUpdates = true;
            Log.d("Horde map", " Запустили locationCallback");
            fusedLocationClient.requestLocationUpdates(locationRequest, locatonCallback, Looper.myLooper());
        }
        exchangeGPSData();
        return START_STICKY;
    }

    public void exchangeGPSData() {
        ExecutorService executor = Executors.newFixedThreadPool(4);;
        executor.execute(() -> {
            try {
                Log.d("Horde map", "Вызван метод exchangeGPSData");
                // Макет запроса id:name:latitude:longitude
                String post = LoginRequest.getId() + "/" + LoginRequest.getName() + "/" + latitude + "/" + longitude;

                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(ipAdress, port), 4000);
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();

                PrintWriter writer = new PrintWriter(outputStream);
                writer.println(post);
                writer.flush();
                Log.d("Horde map", "Запрос отправлен: " + post);

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String json = reader.readLine();

                Type type = new TypeToken<HashMap<Long, String>>() {
                }.getType();
                Gson gson = new Gson();
                try {
                    if (json != null) {
                        HashMap<Long, String> hashMap = gson.fromJson(json, type);
                        Log.d("Horde map", "Запрос получен: " + json);
                        if (!hashMap.isEmpty())
                            MarkersHandler.createMarkers(hashMap);
                    } else {
                        Log.d("Horde map", "Данные пусты");
                    }
                } catch (JsonSyntaxException e) {
                    Log.d("Horde map", "Данные ошибочны");
                    Log.d("Horde map", json);
                }
                clientSocket.close();
                if (isConnectionLost) {
                    isConnectionLost = false;
                }

            } catch (Exception ex) {
                isConnectionLost = true;
                ex.printStackTrace();
                Log.d("Horde map", "Соединение с сервером не установлено");
            } finally {
                executor.shutdown();
            }
        });
    }

    public static String requestInfoFromServer(String request) {
        final String[] answer = {""};
        Thread thread = new Thread(() -> {
            try {
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(ipAdress, port), 4000);
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream);
                writer.println(request);
                writer.flush();
                Log.d("Horde map", "Запрос отправлен: " + request);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                answer[0] = reader.readLine();
                if (answer[0] == null)
                    answer[0] = "";
                clientSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        thread.start();
        try {
            thread.join(); // Ожидаем завершения выполнения потока
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return answer[0];
    }
}