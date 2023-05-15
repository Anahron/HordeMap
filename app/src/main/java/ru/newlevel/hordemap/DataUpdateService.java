package ru.newlevel.hordemap;

import static android.app.Activity.RESULT_OK;
import static ru.newlevel.hordemap.MapsActivity.adapter;
import static ru.newlevel.hordemap.MapsActivity.progressBar;
import static ru.newlevel.hordemap.MapsActivity.progressText;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private final static String MASSAGE_PATH = "massages";
    private final static String MASSAGE_FILE_FOLDER = "MessengerFiles";

    private static final int REQUEST_CODE_SELECT_FILE = 101;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1010;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private static Location location;
    public static List<Messages> allMessages = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final Location[] lastLocation = {null};

    public static synchronized DataUpdateService getInstance() {
        if (instance == null) {
            instance = new DataUpdateService();
        }
        return instance;
    }

    protected void downloadFile(String url, String fileName) {
        if (ContextCompat.checkSelfPermission(MapsActivity.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        } else {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            System.out.println("Полученный файлнейм: " + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager = (DownloadManager) MapsActivity.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                long downloadId = downloadManager.enqueue(request);
                observeDownloadProgress(downloadManager, downloadId);
            }
        }
    }

    private void observeDownloadProgress(DownloadManager downloadManager, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                        onDownloadComplete();
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false;
                        onDownloadFailed();
                    } else {
                        @SuppressLint("Range") int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        @SuppressLint("Range") int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        float percent = (bytesDownloaded * 100.0f) / bytesTotal;
                        updateProgress(percent);
                    }
                }
                cursor.close();
            }
        }).start();
    }

    private void updateProgress(float percent) {
        handler.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress((int) percent);
            progressText.setVisibility(View.VISIBLE);
        });
    }

    private void onDownloadComplete() {
        handler.post(() -> {
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            Toast.makeText(MapsActivity.getContext(), "Файл успешно загружен", Toast.LENGTH_SHORT).show();
        });
    }

    private void onDownloadFailed() {
        handler.post(() -> {
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            Toast.makeText(MapsActivity.getContext(), "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendGeoData(String userId, String userName, double latitude, double longitude) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = GEO_DATA_PATH + "/" + userId;
        // Создаем объект с обновлениями
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/latitude", latitude);
        updates.put(geoDataPath + "/longitude", longitude);
        updates.put(geoDataPath + "/userName", userName);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        System.out.println("Отправка данных : " + updates);
        // Применяем обновления к базе данных
        database.updateChildren(updates);
        checkLastMessage();
    }

    static void sendGeoMarker(String userName, double latitude, double longitude, int selectedItem, String title) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = GEO_MARKERS_PATH + "/" + System.currentTimeMillis();
        // Создаем объект с обновлениями
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/latitude", latitude);
        updates.put(geoDataPath + "/longitude", longitude);
        updates.put(geoDataPath + "/userName", userName);
        updates.put(geoDataPath + "/title", title);
        updates.put(geoDataPath + "/item", selectedItem);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        System.out.println("Отправка данных : " + updates);
        // Применяем обновления к базе данных
        database.updateChildren(updates);
    }

    public static void onSendFileButtonClick(Activity activity) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Выберите нужный тип файлов, например, image/* для изображений
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                sendFile(fileUri); // Вызов вашего метода sendMassage с путем к файлу
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            cursor = MapsActivity.getContext().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                fileName = cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }

    private long getFileSize(Uri fileUri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.MediaColumns.SIZE};
            cursor = MapsActivity.getContext().getContentResolver().query(fileUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                return cursor.getLong(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    private void sendFile(Uri fileUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String fileName = getFileNameFromUri(fileUri);
        Long fileSize = getFileSize(fileUri);
        // Создайте ссылку на файл в Firebase Storage
        StorageReference fileRef = storageRef.child(MASSAGE_FILE_FOLDER + "/" + fileName);

        // Загрузите файл в Firebase Storage
        UploadTask uploadTask = fileRef.putFile(fileUri);

        // Отслеживайте прогресс загрузки (необязательно)
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setVisibility(View.VISIBLE);
                MapsActivity.progressText.setVisibility(View.VISIBLE);
                MapsActivity.progressBar.setProgress((int) progress, true);
            }
            System.out.println("Прогресс загрузки: " + progress + "%");
        });

        // Обработайте завершение загрузки
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                MapsActivity.progressText.setVisibility(View.GONE);
                System.out.println("Загрузка завершена успешно");
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadUrl = uri.toString();
                        createNewMessage(downloadUrl + "&&&" + fileName + "&&&" + fileSize);
                    }
                });
            } else {
                System.out.println("Ошибка загрузки файла: " + task.getException().getMessage());
                // Обработайте ошибку загрузки файла
            }
        });


    }

    public static void checkLastMessage() {
        System.out.println("ПРоверяем последние сообщения");
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH);
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (MessagesAdapter.lastDisplayedMessage == null) {
                        MapsActivity.imageButton.setBackgroundResource(R.drawable.yesmassage);
                        return;
                    } else {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Messages lastMessage = messageSnapshot.getValue(Messages.class);
                            assert lastMessage != null;
                            if (lastMessage.getTimestamp() != MessagesAdapter.lastDisplayedMessage.getTimestamp()) {
                                MapsActivity.imageButton.setBackgroundResource(R.drawable.yesmassage);
                                return;
                            }
                        }
                    }
                }
                System.out.println("Штамп совпал??");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    public static void deleteMarker(Marker marker) {
        DatabaseReference databaseMarkers = FirebaseDatabase.getInstance().getReference().child(GEO_MARKERS_PATH);
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
                    System.out.println("Null в DataSnapshot");
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

    static void getAllGeoData() {
        long timeNow = System.currentTimeMillis();
        float[] alpha = {0};
        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child(GEO_DATA_PATH);
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
                        System.out.println("Положили в hash " + data);
                        hashMap.put(userId, data);
                    }
                    if (!hashMap.isEmpty()) {
                        MarkersHandler.createMarkers(hashMap);
                    } else {
                        Log.d("Horde map", "Данные пусты");
                    }
                } catch (Exception e) {
                    System.out.println("Null в DataSnapshot");
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Обработка ошибки
                System.out.println("Ошибка: " + databaseError.getMessage());
            }
        });

        DatabaseReference databaseMarkers = FirebaseDatabase.getInstance().getReference().child(GEO_MARKERS_PATH);
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
                        MarkersHandler.createMapMarkers(hashMap);
                    } else {
                        Log.d("Horde map", "Данные пусты");
                    }
                } catch (Exception e) {
                    System.out.println("Null в DataSnapshot получения маркеров");
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

    private static void updateLastMessageText(String messageId, String newMessage) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH);
        Map<String, Object> update = new HashMap<>();
        update.put(messageId + "/massage", newMessage);
        update.put(messageId + "/timestamp", System.currentTimeMillis());
        database.updateChildren(update);
    }

    private static void createNewMessage(String message) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = MASSAGE_PATH + "/" + System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/userName", User.getInstance().getUserName());
        updates.put(geoDataPath + "/massage", message);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        database.updateChildren(updates);
    }

    public static void getMessagesFromDatabase(boolean isNeedFool) {
        System.out.println("Запрос getMessagesFromDatabase");
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH);
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Messages> messages = new ArrayList<>();
                if (isNeedFool) allMessages.clear();
                if (allMessages.isEmpty()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Messages message = snapshot.getValue(Messages.class);
                        allMessages.add(message);
                    }
                    adapter.setMessages(allMessages);
                } else {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Messages message = snapshot.getValue(Messages.class);
                        if (allMessages.contains(message)) continue;
                        messages.add(message);
                    }
                    adapter.setLatestMessages(messages);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Обработка ошибки чтения
            }
        });
    }

    static void sendMassage(String massage) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH);
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        Messages lastMessage = messageSnapshot.getValue(Messages.class);
                        if (lastMessage != null && lastMessage.getUserName().equals(User.getInstance().getUserName()) && !lastMessage.getMassage().startsWith("http")) {
                            String newMassage = lastMessage.getMassage() + "\n> " + massage;
                            updateLastMessageText(messageSnapshot.getKey(), newMassage);
                            return;
                        }
                    }
                }
                createNewMessage(massage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
        MyServiceUtils.checkAndStartForeground(this);
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
        // Проверяем если растояние меньше 8 метров межу последней точкой и полученой - не добавляем
        // до 2 = 11
        // до 3 = 14
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
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        sendGeoData(User.getInstance().getUserId(), User.getInstance().getUserName(), latitude, longitude);
        getAllGeoData();
        //    exchangeGPSData();
        return START_STICKY;
    }
}