package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HordeMapRepository {
    private final StorageReference storageRef;
    private final String MESSAGE_PATH = "messages";
    private final static String MESSAGE_FILE_FOLDER = "MessengerFiles";
    private final static String GEO_DATA_PATH = "geoData";
    private final static String GEO_MARKERS_PATH = "geoMarkers";
    private final int TIME_TO_DELETE_USER_MARKER = 20; // в минутах
    private long lastReceivedTimestamp = 0;
    private Query query;
    private ValueEventListener valueEventListener;
    private ValueEventListener markersEventListener;
    private ValueEventListener customMarkersEventListener;
    private final DatabaseReference database;

    private long lastSavedTimestamp = 0;

    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();

    public HordeMapRepository() {
        storageRef = FirebaseStorage.getInstance().getReference();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public void sendGeoDataToDatabase(double latitude, double longitude) {

        String geoDataPath = GEO_DATA_PATH + User.getInstance().getRoomId() + "/" + User.getInstance().getDeviceId();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = dateFormat.format(new Date(System.currentTimeMillis()));

        MyMarker myMarker = new MyMarker(User.getInstance().getUserName(), latitude, longitude, User.getInstance().getDeviceId(), System.currentTimeMillis(), User.getInstance().getMarker(), date);

        DatabaseReference geoDataRef = database.child(geoDataPath);
        geoDataRef.setValue(myMarker);
    }

    void checkDatabaseForNewMessages(Callback<Boolean> callback) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        Message lastMessage = messageSnapshot.getValue(Message.class);
                        assert lastMessage != null;
                        long messageTimestamp = lastMessage.getTimestamp();
                        if (lastSavedTimestamp == 0) {
                            lastSavedTimestamp = messageTimestamp;
                            return;
                        }
                        if (lastSavedTimestamp != messageTimestamp) {
                            callback.onSuccess(true);
                            lastSavedTimestamp = messageTimestamp;
                            return;
                        }
                    }
                }
                callback.onSuccess(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void sendGeoDataToDatabase(double latitude, double longitude, int selectedItem, String title) {
        long time = System.currentTimeMillis();
        String geoDataPath = GEO_MARKERS_PATH + User.getInstance().getRoomId() + "/" + time;

        MyMarker myMarker = new MyMarker(User.getInstance().getUserName(), latitude, longitude, User.getInstance().getDeviceId(), time, selectedItem, title);

        DatabaseReference geoDataRef = database.child(geoDataPath);
        geoDataRef.setValue(myMarker);

        System.out.println("Отправка данных : " + myMarker);
    }

    public void deleteMarkerFromDatabase(Marker marker) {
        String geoDataMarkerPath = GEO_MARKERS_PATH + User.getInstance().getRoomId();
        database.child(geoDataMarkerPath).child(String.valueOf(marker.getTag())).removeValue();
    }

    synchronized void downloadFileFromDatabase(String url, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) MapsActivity.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            long downloadId = downloadManager.enqueue(request);
            observeDownloadProgress(downloadManager, downloadId);
        }
        // Обновление медиахранилища после загрузки файла
        MediaScannerConnection.scanFile(MapsActivity.getContext(), new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName}, null, null);
    }

    LiveData<Integer> getProgressLiveData() {
        return progressLiveData;
    }

    void getAllUsersGeoData(Callback<List<MyMarker>> callback) {
        if (markersEventListener != null) {
            database.removeEventListener(markersEventListener); // Удаляем предыдущий слушатель
        }
        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child(GEO_DATA_PATH + User.getInstance().getRoomId());
        database.addValueEventListener(markersEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<MyMarker> markers = new ArrayList<>();
                long timeNow = System.currentTimeMillis();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        float alpha;
                        long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        long timeDiffMillis = timeNow - timestamp;
                        long timeDiffMinutes = timeDiffMillis / 60000;
                        // Удаляем маркера которым больше TIME_TO_DELETE_USER_MARKER минут
                        if (timeDiffMinutes >= TIME_TO_DELETE_USER_MARKER) {
                            snapshot.getRef().removeValue();
                            continue;
                        } else {
                            // Устанавливаем прозрачность маркера от 0 до 5 минут максимум до 50%
                            alpha = 1F - Math.min(timeDiffMinutes / 10F, 0.5F);
                        }
                        MyMarker myMarker = snapshot.getValue(MyMarker.class);

                        assert myMarker != null;
                        myMarker.setDeviceId(snapshot.getKey());
                        myMarker.setAlpha(alpha);
                        markers.add(myMarker);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                callback.onSuccess(markers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void getAllCustomMarkers(Callback<List<MyMarker>> callback) {
        if (customMarkersEventListener != null) {
            database.removeEventListener(customMarkersEventListener); // Удаляем предыдущий слушатель
        }
        DatabaseReference databaseMarkers = FirebaseDatabase.getInstance().getReference().child(GEO_MARKERS_PATH + User.getInstance().getRoomId());
        databaseMarkers.addValueEventListener(customMarkersEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<MyMarker> markers = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MyMarker myMarker = snapshot.getValue(MyMarker.class);
                    markers.add(myMarker);
                }
                callback.onSuccess(markers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getMessagesSinceTimestamp(long timestamp, Callback<List<Message>> callback) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        if (valueEventListener != null) {
            query.removeEventListener(valueEventListener); // Удаляем предыдущий слушатель
        }
        query = messagesRef.orderByChild("timestamp").startAfter(timestamp);
        query.addValueEventListener(valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null && message.getTimestamp() > lastReceivedTimestamp) {
                        messages.add(message);
                    }
                }
                if (!messages.isEmpty()) {
                    lastSavedTimestamp = messages.get(messages.size() - 1).getTimestamp(); // обновляем отдельную метку для индикации новых сообщений
                    lastReceivedTimestamp = lastSavedTimestamp; // Обновляем метку времени последнего полученного сообщения
                    callback.onSuccess(messages);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("DatabaseError error");
                callback.onError(error.getMessage());
            }
        });
    }

    public void removeMarkersEventListener() {
        if (customMarkersEventListener != null)
            database.removeEventListener(customMarkersEventListener);
        if (markersEventListener != null)
            database.removeEventListener(markersEventListener);
    }

    public void removeMessageEventListener() {
        if (valueEventListener != null) {
            query.removeEventListener(valueEventListener); // Удаляем предыдущий слушатель
        }
    }

    public void sendMessage(String message) {
        checkAndSendTextMessageToDatabase(message);
    }

    synchronized void checkAndSendTextMessageToDatabase(String message) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        Message lastMessage = messageSnapshot.getValue(Message.class);
                        if (lastMessage != null && lastMessage.getDeviceID().equals(User.getInstance().getDeviceId()) && !lastMessage.getMessage().startsWith("http")) {
                            String newMessage = lastMessage.getMessage() + "\n> " + message;
                            messageSnapshot.getRef().child("/message").setValue(newMessage);
                            messageSnapshot.getRef().child("/timestamp").setValue(System.currentTimeMillis() + 1);
                            return;
                        }
                    }
                }
                sendNewMessageToDatabase(message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    synchronized void uploadFileToDatabase(Uri fileUri) {
        String fileName = getFileNameFromUri(fileUri);
        long fileSize = getFileSizeFromUri(fileUri);

        StorageReference fileRef = storageRef.child(MESSAGE_FILE_FOLDER + User.getInstance().getRoomId() + "/" + fileName);
        UploadTask uploadTask = fileRef.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressLiveData.postValue((int) progress);
            }
        });
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressLiveData.postValue(100);
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    sendNewMessageToDatabase(downloadUrl + "&&&" + fileName + "&&&" + fileSize);
                });
            }
        });
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

    private long getFileSizeFromUri(Uri fileUri) {
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

    synchronized void sendNewMessageToDatabase(String message) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        long time = System.currentTimeMillis();
        String geoDataPath = MESSAGE_PATH + User.getInstance().getRoomId() + "/" + time;
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/userName", User.getInstance().getUserName());
        updates.put(geoDataPath + "/message", message);
        updates.put(geoDataPath + "/deviceID", User.getInstance().getDeviceId());
        updates.put(geoDataPath + "/timestamp", time);
        database.updateChildren(updates);
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String errorMessage);

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
                        progressLiveData.postValue(100);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false;
                        progressLiveData.postValue(100);
                    } else {
                        @SuppressLint("Range") int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        @SuppressLint("Range") int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        float percent = (bytesDownloaded * 100.0f) / bytesTotal;
                        progressLiveData.postValue((int) percent);
                    }
                }
                cursor.close();
            }
        }).start();
    }
}