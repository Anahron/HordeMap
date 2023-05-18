package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessengerRepository {
    private final StorageReference storageRef;
    private final String MESSAGE_PATH = "messages";
    private final static String MESSAGE_FILE_FOLDER = "MessengerFiles";
    private long lastReceivedTimestamp = 0;
    private Query query;
    private ValueEventListener valueEventListener;

    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();

    public MessengerRepository() {
        storageRef = FirebaseStorage.getInstance().getReference();
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

    public void getMessagesSinceTimestamp(long timestamp, Callback<List<Message>> callback) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        if (valueEventListener != null) {
            query.removeEventListener(valueEventListener); // Удаляем предыдущий слушатель
        }
        query = messagesRef.orderByChild("timestamp").startAt(timestamp - 1);
        query.addValueEventListener(valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null && message.getTimestamp() > lastReceivedTimestamp) {
                        messages.add(message);
                        lastReceivedTimestamp = message.getTimestamp(); // Обновляем метку времени последнего полученного сообщения
                    }
                }
                if (!messages.isEmpty())
                    callback.onSuccess(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
    public void removeEventListener(){
        query.removeEventListener(valueEventListener);
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
                        if (lastMessage != null && lastMessage.getUserName().equals(User.getInstance().getUserName()) && !lastMessage.getMessage().startsWith("http")) {
                            String newMessage = lastMessage.getMessage() + "\n> " + message;
                            messageSnapshot.getRef().child("/message").setValue(newMessage);
                            messageSnapshot.getRef().child("/timestamp").setValue(System.currentTimeMillis()+1);
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
                    sendMessage(downloadUrl + "&&&" + fileName + "&&&" + fileSize);
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
        String geoDataPath = MESSAGE_PATH + User.getInstance().getRoomId() + "/" + System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/userName", User.getInstance().getUserName());
        updates.put(geoDataPath + "/message", message);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
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