package ru.newlevel.hordemap;

import static android.app.Activity.RESULT_OK;
import static ru.newlevel.hordemap.MapsActivity.adapter;
import static ru.newlevel.hordemap.MapsActivity.MessengerButton;
import static ru.newlevel.hordemap.MapsActivity.photoUri;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Messenger {

    private static Messenger instance = null;
    public static RecyclerView recyclerView;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    public static TextView progressText;
    private Handler handler;
    private static final int REQUEST_CODE_CAMERA = 11;
    public static List<Message> allMessages = new ArrayList<>();
    private final static String MASSAGE_PATH = "massages";
    private final static String MASSAGE_FILE_FOLDER = "MessengerFiles";

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1010;
    private static final int REQUEST_CODE_SELECT_FILE = 101;

    public static synchronized Messenger getInstance() {
        if (instance == null) {
            instance = new Messenger();
        }
        return instance;
    }

    void createMassager(Context context) {
        MapsActivity.MessengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(context, R.style.AlertDialogNoMargins);
                MessengerButton.setBackgroundResource(R.drawable.nomassage);
                dialog.setContentView(R.layout.activity_messages);

                recyclerView = dialog.findViewById(R.id.recyclerViewMessages);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setAdapter(adapter);

                progressText = dialog.findViewById(R.id.progressText);
                progressText.setVisibility(View.INVISIBLE);

                progressBar = dialog.findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);

                getMessagesFromDatabase(true);

                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                ImageButton closeButton = dialog.findViewById(R.id.close_massager);  // Кнопка закрыть
                closeButton.setOnClickListener(v12 -> dialog.dismiss());
                closeButton.setBackgroundResource(R.drawable.close_button);

                ImageButton downButton = dialog.findViewById(R.id.go_down);    // кнопка вниз
                downButton.setOnClickListener(v14 -> recyclerView.scrollToPosition(adapter.getItemCount() - 1));
                downButton.setBackgroundResource(R.drawable.down_button);

                EditText textMassage = dialog.findViewById(R.id.editTextMessage);
                textMassage.setInputType(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
                //  textMassage.requestFocus();

                //    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                textMassage.setOnClickListener(v13 -> {
                    textMassage.requestFocus();
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);});

                textMassage.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        sendMassage(String.valueOf(textMassage.getText()));
                        getMessagesFromDatabase(false);
                        textMassage.setText("");
                        recyclerView.requestFocus();
                        recyclerView.postDelayed(() -> {
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                            textMassage.requestFocus();
                        }, 200); // Задержка для обеспечения фокуса на RecyclerView
                        return true;
                    }
                    return false;
                });

                ImageButton button = dialog.findViewById(R.id.buttonSend);   // Отправка
                button.setBackgroundResource(R.drawable.send_message);
                button.setOnClickListener(v1 -> {
                    sendMassage(String.valueOf(textMassage.getText()));
                    getMessagesFromDatabase(false);
                    textMassage.setText("");
                    recyclerView.requestFocus();
                    recyclerView.postDelayed(() -> {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        textMassage.requestFocus();
                    }, 200); // Задержка для обеспечения фокуса на RecyclerView
                });

                ImageButton downloadButton = dialog.findViewById(R.id.buttonSendFile);   // загрузка
                downloadButton.setBackgroundResource(R.drawable.send_file);
                downloadButton.setOnClickListener(v1 -> {
                    onSendFileButtonClick(((Activity) context));
                    recyclerView.requestFocus();
                    recyclerView.postDelayed(() -> {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        textMassage.requestFocus();
                    }, 200); // Задержка для обеспечения фокуса на RecyclerView
                });

                ImageButton buttonPhoto = dialog.findViewById(R.id.buttonPhoto);   // camera
                buttonPhoto.setBackgroundResource(R.drawable.button_photo);
                buttonPhoto.setOnClickListener(v1 -> openCamera(context));

                handler = new Handler();
                final int[] previousItemCount = {adapter.getItemCount()}; // Предыдущий размер списка
                Message[] lastMessage = {adapter.getItem(adapter.getItemCount() - 1)};
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        getMessagesFromDatabase(false);
                        int newItemCount = adapter.getItemCount(); // Текущий размер списка
                        try {
                            if (newItemCount > previousItemCount[0] || !adapter.getItem(newItemCount - 1).getMassage().equals(lastMessage[0].getMassage())) {
                                previousItemCount[0] = newItemCount;
                                lastMessage[0] = adapter.getItem(adapter.getItemCount() - 1);
                                recyclerView.scrollToPosition(newItemCount - 1); // Прокрутить список вниз, если не находится внизу
                            }
                        } catch (Exception e) {
                            System.out.println("список пуст");
                        }
                        handler.postDelayed(this, 1000);
                    }
                };
                handler.post(runnable);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                dialog.setOnDismissListener(dialog1 -> handler.removeCallbacks(runnable));
                dialog.show();
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

    protected void sendFile(Uri fileUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String fileName = getFileNameFromUri(fileUri);
        long fileSize = getFileSize(fileUri);
        // Создайте ссылку на файл в Firebase Storage
        StorageReference fileRef = storageRef.child(MASSAGE_FILE_FOLDER + User.getInstance().getRoomId() + "/" + fileName);

        // Загрузите файл в Firebase Storage
        UploadTask uploadTask = fileRef.putFile(fileUri);

        // Отслеживайте прогресс загрузки (необязательно)
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                progressBar.setProgress((int) progress, true);
            }
            System.out.println("Прогресс загрузки: " + progress + "%");
        });
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
                System.out.println("Загрузка завершена успешно");
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    createNewMessage(downloadUrl + "&&&" + fileName + "&&&" + fileSize);
                });
            } else {
                System.out.println("Ошибка загрузки файла");
                // Обработайте ошибку загрузки файла
            }
        });


    }

    void checkLastMessage() {
        System.out.println("Проверяем последние сообщения");
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (MessagesAdapter.lastDisplayedMessage == null) {
                        MapsActivity.MessengerButton.setBackgroundResource(R.drawable.yesmassage);
                        return;
                    } else {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Message lastMessage = messageSnapshot.getValue(Message.class);
                            assert lastMessage != null;
                            if (lastMessage.getTimestamp() != MessagesAdapter.lastDisplayedMessage.getTimestamp()) {
                                MapsActivity.MessengerButton.setBackgroundResource(R.drawable.yesmassage);
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

    private void updateLastMessageText(String messageId, String newMessage) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH + User.getInstance().getRoomId());
        Map<String, Object> update = new HashMap<>();
        update.put(messageId + "/massage", newMessage);
        update.put(messageId + "/timestamp", System.currentTimeMillis());
        database.updateChildren(update);
    }

    void sendMassage(String massage) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        Message lastMessage = messageSnapshot.getValue(Message.class);
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

    void createNewMessage(String message) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = MASSAGE_PATH + User.getInstance().getRoomId() + "/" + System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/userName", User.getInstance().getUserName());
        updates.put(geoDataPath + "/massage", message);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        database.updateChildren(updates);
    }

    public static void onSendFileButtonClick(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Выберите нужный тип файлов, например, image/* для изображений
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                sendFile(fileUri);
            }
        }
    }

    private void getMessagesFromDatabase(boolean isNeedFool) {
        System.out.println("Запрос getMessagesFromDatabase");
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MASSAGE_PATH + User.getInstance().getRoomId());
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                if (isNeedFool) allMessages.clear();
                if (allMessages.isEmpty()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        allMessages.add(message);
                    }
                    adapter.setMessages(allMessages);
                } else {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (allMessages.contains(message)) continue;
                        messages.add(message);
                    }
                    if (!messages.isEmpty())
                        adapter.setLatestMessages(messages);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Обработка ошибки чтения
            }
        });
    }

    @SuppressLint("IntentReset")
    private void openCamera(Context context) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = createImageFile(context);
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(context, "ru.newlevel.hordemap.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                // Создание интента для открытия галереи
                @SuppressLint("IntentReset") Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");

                // Создание интента для выбора из нескольких источников
                Intent chooserIntent = Intent.createChooser(takePictureIntent, "Select Source");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{galleryIntent});

                ((Activity) context).startActivityForResult(chooserIntent, REQUEST_CODE_CAMERA);
            }
        }
    }

    private File createImageFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void downloadFile(String url, String fileName) {
        if (ContextCompat.checkSelfPermission(MapsActivity.getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            // Обновление медиахранилища после загрузки файла
            MediaScannerConnection.scanFile(MapsActivity.getContext(), new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName}, null, null);
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

}
