package ru.newlevel.hordemap;


import static ru.newlevel.hordemap.MapsActivity.id;
import static ru.newlevel.hordemap.MapsActivity.mMap;
import static ru.newlevel.hordemap.MapsActivity.name;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.Objects;

public class DataSender extends Service {

    public static String ipAdress = "horde.krasteplovizor.ru";  // сервер
  //  public static String ipAdress = "192.168.1.21";  //  локал
    public static int port = 49283; //49283 -сервер
  //  public static int port = 8080; // локал
    private static final ArrayList<Marker> markers = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Boolean isMarkersON = true;
    private static final int NOTIFICATION_ID = 1;
    @SuppressLint("StaticFieldLeak")
    public static DataSender sender = DataSender.getInstance();
    @SuppressLint("StaticFieldLeak")
    private static DataSender instance = null;
    public static int MARKER_SIZE = 60;
    private static HashMap<Long, String> savedmarkers = new HashMap<>();
    public static double latitude;
    public static double longitude;
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

    public DataSender() {
    }

    public static DataSender getInstance() {
        if (instance == null) {
            instance = new DataSender();
        }
        return instance;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        MyServiceUtils.startAlarmManager(context);
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
        super.onStartCommand(intent, flags, startId);
        checkAndStartForeground();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
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
        Log.d("Horde map", "onStartCommand вызвана " + this);
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkAndStartForeground() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();

        boolean notificationDisplayed = false;
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getId() == NOTIFICATION_ID) {
                notificationDisplayed = true;
                break;
            }
        }
        if (!notificationDisplayed) {
            Log.d("Horde map", "Запустили сервис startForeground");
            startForeground(NOTIFICATION_ID, MyServiceUtils.createNotification(context));
        } else {
            Log.d("Horde map", "Сервис startForeground уже запущен");
        }
    }

    @Override
    public void onDestroy() {
        Log.d("Horde map", "Вызван ондестрой в дата сендере");
        super.onDestroy();
    }

    public void myonDestroy() {
        stopSelf();
        onDestroy();
    }

    public static void offMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
    }

    public static void apDateMarkers() {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwing);
        Bitmap bitmapcom = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwingcomander);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE, MARKER_SIZE, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, MARKER_SIZE, MARKER_SIZE, false));
        if (!savedmarkers.isEmpty()) {
            for (Marker marker : markers) {
                marker.remove();
            }
            for (Long id : savedmarkers.keySet()) {
                if (!Objects.equals(MapsActivity.id, id) && isMarkersON) { //ЗАМЕНИТЬ 0 НА id Чтобы удалялись мои метки
                    String[] data = Objects.requireNonNull(savedmarkers.get(id)).split("/");
                    String hour = data[3].substring(11, 13);
                    int hourkrsk = Integer.parseInt(hour) + 7;
                    if (hourkrsk >= 24)
                        hourkrsk = hourkrsk - 24;
                    String minutes = data[3].substring(13, 16);
                    String rank = (Integer.parseInt(data[4]) == 1 ? "Сержант" : "Рядовой");
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                            .title(data[0])
                            .alpha(Float.parseFloat(data[5]))
                            .snippet(rank + " " + hourkrsk + minutes)
                            .icon(Integer.parseInt(data[4]) == 1 ? iconcom : icon));
                    markers.add(marker);
                }
            }
        }
    }

    public static void createMarkers(HashMap<Long, String> map) {
        Log.d("Horde map", "Удаляются старые и создаются новые маркеры");
        savedmarkers = map;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwing);
        Bitmap bitmapcom = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwingcomander);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE, MARKER_SIZE, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, MARKER_SIZE, MARKER_SIZE, false));
        ((Activity) context).runOnUiThread(() -> {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
            for (Long id : map.keySet()) {
                if (!Objects.equals(MapsActivity.id, id) && isMarkersON) {
                    String[] data = Objects.requireNonNull(map.get(id)).split("/");
                    String hour = data[3].substring(11, 13);
                    int hourkrsk = Integer.parseInt(hour) + 7;
                    if (hourkrsk >= 24)
                        hourkrsk = hourkrsk - 24;
                    String minutes = data[3].substring(13, 16);
                    String rank = (Integer.parseInt(data[4]) == 1 ? "Сержант" : "Рядовой");
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                            .title(data[0])
                            .alpha(Float.parseFloat(data[5]))
                            .snippet(rank + " " + hourkrsk + minutes)
                            .icon(Integer.parseInt(data[4]) == 1 ? iconcom : icon));
                    markers.add(marker);
                }
            }
        });
    }

    public void sendGPS() {
        try {
            Log.d("Horde map", "Вызван метод sendGPS, отсылаем данные и получаем ответ");
            // Макет запроса id:name:latitude:longitude
//            if (name == null || name.equals("name") || name.equals(""))
//                LoginRequest.logIn(context, mapsActivity);
            String post = id + "/" + name + "/" + latitude + "/" + longitude;
            Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(ipAdress, port), 4000);
            // Получаем входной и выходной потоки для обмена данными с сервером
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // Отправляем запрос серверу
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(post);
            writer.flush();
            Log.d("Horde map", "Запрос отправлен: " + post);
            // Читаем данные из входного потока
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.readLine();
            // Определяем тип данных, в которые нужно преобразовать JSON-строку
            Type type = new TypeToken<HashMap<Long, String>>() {
            }.getType();
            // Преобразуем JSON-строку в HashMap
            Gson gson = new Gson();
            try {
                if (json != null) {
                    HashMap<Long, String> hashMap = gson.fromJson(json, type);
                    Log.d("Horde map", "Запрос получен: " + json);
                    if (!hashMap.isEmpty())
                        createMarkers(hashMap);
                } else {
                    Log.d("Horde map", "Данные пусты");
                }
            } catch (JsonSyntaxException e) {
                Log.d("Horde map", "Данные ошибочны");
                Log.d("Horde map", json);
            }
            // Закрываем соединение с клиентом
            clientSocket.close();
            if (isConnectionLost) {
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Соединение установлено", Toast.LENGTH_SHORT).show());
                isConnectionLost = false;
            }

        } catch (Exception ex) {
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Соединение не установлено", Toast.LENGTH_LONG).show());
            isConnectionLost = true;
            ex.printStackTrace();
            Log.d("Horde map", "Соединение с сервером не установлено");
        }
    }

    public static String requestInfoFromServer(String request) {
        final String[] answer = {""};
        Thread thread = new Thread(() -> {
            try {
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(ipAdress, port), 4000);
                // Получаем входной и выходной потоки для обмена данными с сервером
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                // Отправляем запрос серверу
                PrintWriter writer = new PrintWriter(outputStream);
                writer.println(request);
                writer.flush();
                Log.d("Horde map", "Запрос отправлен: " + request);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                answer[0] = reader.readLine();
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