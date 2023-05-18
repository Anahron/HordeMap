package ru.newlevel.hordemap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.database.DatabaseReference;


public class DataUpdateService extends Service {

    @SuppressLint("StaticFieldLeak")
    private static DataUpdateService instance = null;

    private static double latitude;
    private static double longitude;

    public static List<LatLng> locationHistory = new ArrayList<>();
    private static boolean requestingLocationUpdates = false;
    private final static int UPDATE_INTERVAL = 3000;
    private final static int FASTEST_INTERVAL = 2000;
    private final static int DISPLACEMENT = 3;

    private final static String GEO_DATA_PATH = "geoData";
    private final static String GEO_MARKERS_PATH = "geoMarkers";

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private static Location location;
    private static PowerManager.WakeLock wakeLock;
    private static final Location[] lastLocation = {null};

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 2001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 2002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 2003;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION = 2004;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 2005;
    private static final int MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS = 2006;
    private static final int REQUEST_CODE_FOREGROUND_SERVICE = 2012;

    public static synchronized DataUpdateService getInstance() {
        if (instance == null) {
            instance = new DataUpdateService();
        }
        return instance;
    }

    private void sendGeoDataToDatabase(String userId, String userName, double latitude, double longitude) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = GEO_DATA_PATH + User.getInstance().getRoomId() + "/" + userId;

        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/latitude", latitude);
        updates.put(geoDataPath + "/longitude", longitude);
        updates.put(geoDataPath + "/userName", userName);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        System.out.println("Отправка данных : " + updates);
        database.updateChildren(updates);
        checkDatabaseForNewMessages();
    }

    void checkDatabaseForNewMessages() {
        String MESSAGE_PATH = "messages";
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (MessagesAdapter.lastDisplayedMessage == null) {
                        Messenger.getInstance().getMessengerButton().setBackgroundResource(R.drawable.yesmassage);
                    } else {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Message lastMessage = messageSnapshot.getValue(Message.class);
                            assert lastMessage != null;
                            if (lastMessage.getTimestamp() != MessagesAdapter.lastDisplayedMessage.getTimestamp()) {
                                Messenger.getInstance().getMessengerButton().setBackgroundResource(R.drawable.yesmassage);
                                return;
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    static void sendGeoMarkerToDatabase(String userName, double latitude, double longitude, int selectedItem, String title) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = GEO_MARKERS_PATH + User.getInstance().getRoomId() + "/" + System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/latitude", latitude);
        updates.put(geoDataPath + "/longitude", longitude);
        updates.put(geoDataPath + "/userName", userName);
        updates.put(geoDataPath + "/title", title);
        updates.put(geoDataPath + "/item", selectedItem);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        System.out.println("Отправка данных : " + updates);
        database.updateChildren(updates);
    }

    public static void deleteMarkerFromDatabase(Marker marker) {
        DatabaseReference databaseMarkers = FirebaseDatabase.getInstance().getReference().child(GEO_MARKERS_PATH + User.getInstance().getRoomId());
        databaseMarkers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        double latitude = snapshot.child("latitude").getValue(Double.class);
                        double longitude = snapshot.child("longitude").getValue(Double.class);
                        if (latitude == marker.getPosition().latitude && longitude == marker.getPosition().longitude) {
                            snapshot.getRef().removeValue();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    static void getAllGeoDataFromDatabase() {
        long timeNow = System.currentTimeMillis();
        float[] alpha = {0};
        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child(GEO_DATA_PATH + User.getInstance().getRoomId());
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    HashMap<String, String> hashMap = new HashMap<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String userId = snapshot.getKey();
                        String userName = snapshot.child("userName").getValue(String.class);
                        double latitude = snapshot.child("latitude").getValue(Double.class);
                        double longitude = snapshot.child("longitude").getValue(Double.class);
                        long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        long timeDiffMillis = timeNow - timestamp;
                        long timeDiffMinutes = timeDiffMillis / 60000;
                        if (timeDiffMinutes >= 10 || latitude == 0.0) {
                            snapshot.getRef().removeValue();
                            continue;
                        } else {
                            alpha[0] = 1 - (timeDiffMinutes / 10F);
                            if (alpha[0] < 0.5) alpha[0] = 0.5F;
                        }
                        String data = userName + "/" + latitude + "/" + longitude + "/" + timestamp + "/" + alpha[0];
                        hashMap.put(userId, data);
                    }
                    if (!hashMap.isEmpty()) {
                        MarkersHandler.createAllUsersMarkers(hashMap);
                    } else {
                        Log.d("Horde map", "Данные пусты");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        DatabaseReference databaseMarkers = FirebaseDatabase.getInstance().getReference().child(GEO_MARKERS_PATH + User.getInstance().getRoomId());
        databaseMarkers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    HashMap<String, String> hashMap = new HashMap<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        double latitude = snapshot.child("latitude").getValue(Double.class);
                        double longitude = snapshot.child("longitude").getValue(Double.class);
                        String title = snapshot.child("title").getValue(String.class);
                        long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        int item = snapshot.child("item").getValue(Integer.class);
                        long timeDiffMillis = timeNow - timestamp;
                        long timeDiffMinutes = timeDiffMillis / 60000;
                        if (timeDiffMinutes >= 1440 || latitude == 0.0) {
                            snapshot.getRef().removeValue();
                            continue;
                        } else {
                            alpha[0] = 1;
                        }
                        String data = title + "/" + latitude + "/" + longitude + "/" + timestamp + "/" + alpha[0] + "/" + item;
                        hashMap.put(snapshot.getKey(), data);
                    }
                    if (!hashMap.isEmpty()) {
                        MarkersHandler.createCustomMapMarkers(hashMap);
                    } else {
                        Log.d("Horde map", "Данные пусты");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Обработка ошибки
                System.out.println("Ошибка: " + databaseError.getMessage());
            }
        });
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }
    @SuppressLint("BatteryLife")
    protected void setPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Запросите разрешения
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            int requestCode = MY_PERMISSIONS_REQUEST_LOCATION;
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), permissions, requestCode);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.FOREGROUND_SERVICE}, REQUEST_CODE_FOREGROUND_SERVICE);
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission_group.SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission_group.SENSORS}, MY_PERMISSIONS_REQUEST_SENSOR);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.WAKE_LOCK}, MY_PERMISSIONS_REQUEST_WAKE_LOCK);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{android.Manifest.permission.SCHEDULE_EXACT_ALARM}, MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS);
            }
        }
    }
    @SuppressLint({"InvalidWakeLockTag"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        System.out.println("onCreate вызвана");
        super.onCreate();
        setPermission();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
        MyServiceUtils.checkAndStartForeground(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand вызвана");
        super.onStartCommand(intent, flags, startId);
        LocationCallback locationCallback = new LocationCallback() {
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
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (!requestingLocationUpdates) {
            requestingLocationUpdates = true;
            Log.d("Horde map", " Запустили locationCallback");
            // Разрешения на доступ к местоположению уже предоставлены, выполняем необходимые действия для получения местоположения
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);}
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        }
        sendGeoDataToDatabase(User.getInstance().getDeviceId(), User.getInstance().getUserName(), latitude, longitude);
        getAllGeoDataFromDatabase();
        return START_STICKY;
    }
}