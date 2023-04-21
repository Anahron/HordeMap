package ru.newlevel.hordemap;

import static android.content.ContentValues.TAG;
import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.MapsActivity.mMap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.graphics.Canvas;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class DataSender extends Service {
    private static HashMap<Integer, String> gpsList = new HashMap();
    public static String ipAdress = "horde.krasteplovizor.ru";  // "horde.krasteplovizor.ru" - сервер" 192.168.1.21 - локал
    public static int port = 49283; //49383 -сервер
    private static ArrayList<Marker> markers = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Boolean isMarkersON = true;
    private static final int NOTIFICATION_ID = 1;
    private Handler handler;
    private Runnable runnable;
    @SuppressLint("StaticFieldLeak")
    public static DataSender sender = DataSender.getInstance();
    @SuppressLint("StaticFieldLeak")
    private static DataSender instance = null;

    public DataSender() {
    }

    public static DataSender getInstance() {
        if (instance == null) {
            instance = new DataSender();
        }
        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("Зашли в ONCREATE");
        startForeground(NOTIFICATION_ID, createNotification());
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("MyForegroundService", "Current time: " + new Date().toString());
                //  handler.postDelayed(this, 50000); //50000 норм
                System.out.println("В методе onCreate вызываем Аларм Менеджер");
                startAlarmManager();
            }
        };
        runnable.run();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand вызвана");
        startForeground(NOTIFICATION_ID, createNotification());
        startAlarmManager();
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("CHANNEL_ID", "channel_name", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Horde Map")
                .setContentText("Похищаем твои данные и передаем орде")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        ;
        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected static void startAlarmManager() {
        System.out.println("Запустился Аларм Менеджер");
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, MyWakefulReceiver.class);
        intent.setAction("com.newlevel.ACTION_SEND_DATA");
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 30);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, 0);
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, pendingIntent);
        System.out.println("Аларм менеджер отработал");
    }

    public static void offMarkers() {
        for (Marker marker : markers) {
            System.out.println(marker);
            marker.remove();
        }
    }

    public static void createMarkers(HashMap<Long, String> map) {
        System.out.println("Удаляются старые и создаются новые маркеры");
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwing);
        Bitmap bitmapcom = BitmapFactory.decodeResource(context.getResources(), R.drawable.pngwingcomander);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, 60, 60, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, 60, 60, false));
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : markers) {
                    marker.remove();
                }
                markers.clear();
                for (Long id : map.keySet()) {
                    if (MapsActivity.id != 0 && isMarkersON) { //ЗАМЕНИТЬ 0 НА id Чтобы удалялись мои метки
                        String[] data = Objects.requireNonNull(map.get(id)).split("/");
                        String hour = data[3].substring(11, 13);
                        int hourkrsk = Integer.parseInt(hour) + 7;
                        if (hourkrsk >= 24)
                            hourkrsk = hourkrsk-24;
                        String minutes = data[3].substring(13, 16);
                        String rank = (Integer.parseInt(data[4]) == 1? "Сержант" : "Рядовой");
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                                .title(data[0])
                                .alpha(Float.parseFloat(data[5]))
                                .snippet(rank + " " + hourkrsk + minutes)
                                .icon(Integer.parseInt(data[4]) == 1? iconcom : icon));
                        markers.add(marker);
                    }
                }
            }
        });
    }

    public void sendGPS() {
        try {
            System.out.println("Вызван метод RUN, отсылаем данные и получаем ответ");
            // Формируем запрос. Макет запроса id:name:latitude:longitude
            String post = MapsActivity.id + "/" + MapsActivity.name + "/" + MyLocationListener.getLastKnownLocation();
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
            System.out.println("Запрос отправлен: " + post);

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
                    System.out.println("Запрос получен: " + hashMap.toString());
                    createMarkers(hashMap);
                } else {
                    System.out.println("Данные пусты");
                }
            } catch (JsonSyntaxException e) {
                System.out.println("Данные ошибочны");
                System.out.println(json);
            }
            // Закрываем соединение с клиентом
            clientSocket.close();

        } catch (Exception ex) {
//            ((Activity) context).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(context, "Соединение не установлено, проверьте подключение к интернету", Toast.LENGTH_LONG).show();
//                }
//            });
            ex.printStackTrace();
            System.out.println("Соединение с сервером не установлено");
        }
    }

    public static String requestInfoFromServer(String request) {
        final String[] answer = {""};
        Thread thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    // Создаем сокет на порту 8080
                    Socket clientSocket = new Socket();
                    clientSocket.connect(new InetSocketAddress(ipAdress, port), 5000);
                    // Получаем входной и выходной потоки для обмена данными с сервером
                    InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream();

                    // Отправляем запрос серверу
                    PrintWriter writer = new PrintWriter(outputStream);
                    writer.println(request);
                    writer.flush();
                    System.out.println("Запрос отправлен: " + request);

                    // Читаем данные из входного потока
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    answer[0] = reader.readLine();
                    clientSocket.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Запустился метод MyWakefulReceiver, получает координаты и вызывает RUN в новом потоке");
            Intent service = new Intent(context, DataSender.class);
            service.setAction("com.newlevel.ACTION_SEND_DATA");
            startWakefulService(context, service);
            setResultCode(Activity.RESULT_OK);
            // Получение последних доступных координат
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startAlarmManager();
            }
            Thread thread = new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    sender.sendGPS();
                }
            });
            thread.start();
            // Завершение работы сервиса
            completeWakefulIntent(intent);
            this.abortBroadcast();
        }
    }
}