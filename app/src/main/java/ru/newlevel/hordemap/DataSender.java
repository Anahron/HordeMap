package ru.newlevel.hordemap;


import static ru.newlevel.hordemap.MapsActivity.mMap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DataSender extends Service {
    public static String ipAdress = "horde.krasteplovizor.ru";  // "horde.krasteplovizor.ru" - сервер" 192.168.1.21 - локал
    public static int port = 49283; //49283 -сервер 443
    private static final ArrayList<Marker> markers = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Boolean isMarkersON = true;
    private static final int NOTIFICATION_ID = 1;
    @SuppressLint("StaticFieldLeak")
    public static DataSender sender = DataSender.getInstance();
    @SuppressLint("StaticFieldLeak")
    private static DataSender instance = null;
    public static int markerSize = 60;
    private static HashMap<Long, String> savedmarkers = new HashMap<>();
    private static PendingIntent pendingIntent;
    public static double latitude;
    public static double longitude;
    public static List<LatLng> locationHistory = new ArrayList<>();
    private static FusedLocationProviderClient fusedLocationClient;
    private static LocationCallback locationCallback;
    private static LocationRequest locationRequest;
    private static boolean requestingLocationUpdates = false;
    private static Location location;
    private static AlarmManager alarmMgr;
    private static boolean isConnectionLost;


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
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        boolean notificationNotDisplayed = true;
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getId() == NOTIFICATION_ID) {
                notificationNotDisplayed = false;
                break;
            }
        }
        if (notificationNotDisplayed) {
            Log.d("Horde map", "Запустили сервис startForeground");
            startForeground(NOTIFICATION_ID, createNotification());
        } else {
            Log.d("Horde map", "Сервис startForeground уже запущен");
        }
        startAlarmManager();
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
        Log.d("Horde map", "onStartCommand вызвана " + this);
        startAlarmManager();
        return START_REDELIVER_INTENT; //пробуем
        //  return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel channel = new NotificationChannel("CHANNEL_1", "GPS", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_1")
                .setSmallIcon(R.mipmap.hordecircle_round)
                .setContentTitle("Horde Map")
                .setContentText("Приложение работает в фоновом режиме")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(500);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        Log.d("Horde map", "Вызван ондестрой в дата сендере");
        super.onDestroy();
    }

    public void myonDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (pendingIntent != null) {
                Log.d("Horde map", "Аларм менеджер Остановлен в методе onDestroy");
                alarmMgr.cancel(pendingIntent);
            }
            stopSelf();
            onDestroy();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected static void startAlarmManager() {
        Log.d("Horde map", "Запустился Аларм Менеджер " + getInstance());
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyWakefulReceiver.class);
        intent.setAction("com.newlevel.ACTION_SEND_DATA");
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20000, pendingIntent);
        Log.d("Horde map", "Аларм менеджер отработал " + getInstance());
    }


    public static void offMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
    }

    public static void apDateMarkers() {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwing);
        Bitmap bitmapcom = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwingcomander);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, markerSize, markerSize, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, markerSize, markerSize, false));
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
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, markerSize, markerSize, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, markerSize, markerSize, false));
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
            // Формируем запрос. Макет запроса id:name:latitude:longitude
            String post = MapsActivity.id + "/" + MapsActivity.name + "/" + latitude + "/" + longitude;
            // Создаем сокет на порту 8080
            Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(ipAdress, port), 10000);
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

        } catch (
                Exception ex) {
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
                clientSocket.connect(new InetSocketAddress(ipAdress, port), 5000);
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

    public static class MyWakefulReceiver extends WakefulBroadcastReceiver {

        public MyWakefulReceiver() {
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Horde map", "Запустился метод MyWakefulReceiver " + this + " -  получает координаты и вызывает sendData в новом потоке");
            Intent service = new Intent(context, DataSender.class);
            service.setAction("com.newlevel.ACTION_SEND_DATA");
            startWakefulService(context, service);
            setResultCode(Activity.RESULT_OK);
            // Настраиваем  fusedLocationClient и  locationRequest
            final Location[] lastLocation = {null};
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setSmallestDisplacement(3);

            locationCallback = new LocationCallback() {
                @SuppressLint("SuspiciousIndentation")
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    location = locationResult.getLastLocation();
                    try {
                        if (location != null) {
                            Log.d("Horde map", location.getLatitude() + " " + location.getLongitude() + "   " + location.getAccuracy() + "   получены координаты");
                            if (location.getAccuracy() < 25) {
                                Log.d("Horde map", "Аккуратность < 25, проверяем на растояние");
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                if (lastLocation[0] == null) {
                                    lastLocation[0] = location;
                                }
                                if (SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(lastLocation[0].getLatitude(), lastLocation[0].getLongitude())) > 10) {    // Проверяем если растояние меньше 8 метров межу последней точкой и полученой - не добавляем
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
                                    }
                                    lastLocation[0] = location;
                                    Log.d("Horde map", "В coordinates добавлено " + latitude + " " + longitude);
                                } else {
                                    Log.d("Horde map", "Растояние < 8 метров, пропускаем");
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            ;
            if (!requestingLocationUpdates) {
                requestingLocationUpdates = true;
                Log.d("Horde map", " Запустили locationCallback");
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }

            Thread thread = new Thread(() -> sender.sendGPS());
            thread.start();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                new Thread(DataSender::startAlarmManager);
                completeWakefulIntent(intent);
                this.abortBroadcast();
            }
        }
    }
}