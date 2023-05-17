package ru.newlevel.hordemap;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Messenger {

    @SuppressLint("StaticFieldLeak")
    private static Messenger instance = null;
    private Handler handler;
    public static List<Message> allMessages = new ArrayList<>();
    private final static String MESSAGE_PATH = "messages";
    private final static String MESSAGE_FILE_FOLDER = "MessengerFiles";
    private final HashSet<Long> loadedMessageIds = new HashSet<>(); // для хранения идентификаторов уже загруженных сообщений

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1010;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1011;
    public static final int REQUEST_CODE_SELECT_FILE = 101;
    public static final int REQUEST_CODE_CAMERA = 11;

    private Context context;
    private TextView progressText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MessagesAdapter adapter;
    private ImageButton messengerButton;
    private EditText textMessage;
    private Dialog dialog;
    private Uri photoUri;
    private ImageButton newMessageButton;

    public static Messenger getInstance() {
        if (instance == null) {
            instance = new Messenger();
        }
        return instance;
    }

    public ImageButton getMessengerButton() {
        return messengerButton;
    }

    void createMessenger(Context context) {
        this.context = context;
        handler = new Handler();
        createMessengerButton();
        createDialog();
        createNewMessageAnnounces(dialog);
        createAndSetupRecyclerView();

        messengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MapsActivity.getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                }
                getMessagesFromDatabase(true);

                createProgressBar(dialog);
                createScrollDownButton(dialog);
                createEditTextMessage(dialog);
                createSendMessageButton(dialog);
                createUploadFileButton(dialog);
                createOpenCameraButton(dialog);
                createCloseMessengerButton(dialog);

                Runnable updateMessagesEveryMin = new Runnable() {
                    @Override
                    public void run() {
                        getMessagesFromDatabase(false);
                        boolean isAtEnd = !recyclerView.canScrollVertically(1) && recyclerView.computeVerticalScrollOffset() > 0;
                        if (isAtEnd) {
                            newMessageButton.setVisibility(View.GONE);
                        }
                        handler.postDelayed(this, 1000);
                    }
                };

                // слушатель размера экрана для прокрутки элементов при открытии клавиатуры
                final View activityRootView = dialog.findViewById(R.id.activityRoot);
                activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    Rect r = new Rect();
                    activityRootView.getWindowVisibleDisplayFrame(r);

                    int screenHeight = activityRootView.getRootView().getHeight();
                    int keypadHeight = r.bottom - screenHeight;
                    // если высота клавиатуры больше 15% от экрана, считаем клавиатуру открытой
                    if (keypadHeight > screenHeight * 0.15) {
                        if (adapter.getItemCount() > 0)
                            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                });

                handler.post(updateMessagesEveryMin);
                dialog.setOnDismissListener(dialog1 -> {
                    handler.removeCallbacks(updateMessagesEveryMin);
                    messengerButton.setBackgroundResource(R.drawable.nomassage);
                });
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

    synchronized void uploadFileToDatabase(Uri fileUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String fileName = getFileNameFromUri(fileUri);
        long fileSize = getFileSizeFromUri(fileUri);

        StorageReference fileRef = storageRef.child(MESSAGE_FILE_FOLDER + User.getInstance().getRoomId() + "/" + fileName);
        UploadTask uploadTask = fileRef.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                progressBar.setProgress((int) progress, true);
            }
        });
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    sendNewMessageToDatabase(downloadUrl + "&&&" + fileName + "&&&" + fileSize);
                });
            }
        });
    }

    void checkDatabaseForNewMessages() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        Query lastMessageQuery = messagesRef.orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (MessagesAdapter.lastDisplayedMessage == null) {
                        messengerButton.setBackgroundResource(R.drawable.yesmassage);
                    } else {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Message lastMessage = messageSnapshot.getValue(Message.class);
                            assert lastMessage != null;
                            System.out.println("сравниваем lastMessage.getTimestamp() " + lastMessage.getTimestamp() + " MessagesAdapter.lastDisplayedMessage.getTimestamp() " + MessagesAdapter.lastDisplayedMessage.getTimestamp());
                            if (lastMessage.getTimestamp() != MessagesAdapter.lastDisplayedMessage.getTimestamp()) {
                                messengerButton.setBackgroundResource(R.drawable.yesmassage);
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

    private void getMessagesFromDatabase(boolean isNeedFool) {
        long maxTimestamp;
        if (isNeedFool || MessagesAdapter.lastDisplayedMessage == null)
            maxTimestamp = 100000000L;
        else
            maxTimestamp = MessagesAdapter.lastDisplayedMessage.getTimestamp();
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference(MESSAGE_PATH + User.getInstance().getRoomId());
        Query query = messagesRef.orderByChild("timestamp").startAfter(maxTimestamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("Получили data");
                List<Message> messages = new ArrayList<>();
                if (isNeedFool) {
                    allMessages.clear();
                    loadedMessageIds.clear();
                }
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    assert message != null;
                    Long messageId = message.getTimestamp();
                    System.out.println(messageId);
                    if (loadedMessageIds.contains(messageId)) {
                        continue;
                    }
                    loadedMessageIds.add(messageId);
                    messages.add(message);
                }
                if (allMessages.isEmpty() && !messages.isEmpty()) {
                    allMessages.addAll(messages);
                    adapter.setAllMessages(allMessages);
                } else if (!messages.isEmpty()) {
                    adapter.setLatestMessages(messages);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
                            messageSnapshot.getRef().child("/timestamp").setValue(System.currentTimeMillis());
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

    synchronized void sendNewMessageToDatabase(String message) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String geoDataPath = MESSAGE_PATH + User.getInstance().getRoomId() + "/" + System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(geoDataPath + "/userName", User.getInstance().getUserName());
        updates.put(geoDataPath + "/message", message);
        updates.put(geoDataPath + "/timestamp", System.currentTimeMillis());
        database.updateChildren(updates);
    }

    public static void onSendFileButtonClick(@NonNull Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                uploadFileToDatabase(fileUri);
            }
        }
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            if (data != null) {
                photoUri = data.getData();
            }
            Messenger.getInstance().uploadFileToDatabase(photoUri);
        }

    }

    @SuppressLint("IntentReset")
    private void openPhotoLoaderIntents(@NonNull Context context) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = createTempImageFile(context);
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

    @Nullable
    private File createTempImageFile(@NonNull Context context) {
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

    synchronized void downloadFileFromDatabase(String url, String fileName) {
        if (ContextCompat.checkSelfPermission(MapsActivity.getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        } else {
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
    }

    private void createMessengerButton() {
        messengerButton = ((Activity) context).findViewById(R.id.massage);
        messengerButton.setBackgroundResource(R.drawable.nomassage);
        messengerButton.setClickable(false);
    }

    private void createDialog() {
        dialog = new Dialog(context, R.style.AlertDialogNoMargins);
        dialog.setContentView(R.layout.activity_messages);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void createAndSetupRecyclerView() {
        recyclerView = dialog.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new MessagesAdapter(recyclerView, newMessageButton);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(true);
    }

    private void createScrollDownButton(@NonNull Dialog dialog) {
        ImageButton scrollDownButton = dialog.findViewById(R.id.go_down);    // кнопка скрола вниз
        scrollDownButton.setOnClickListener(v14 -> recyclerView.scrollToPosition(adapter.getItemCount() - 1));
        scrollDownButton.setBackgroundResource(R.drawable.down_button);
    }

    private void createEditTextMessage(@NonNull Dialog dialog) {
        textMessage = dialog.findViewById(R.id.editTextMessage);
        textMessage.setInputType(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
        textMessage.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String text = String.valueOf(textMessage.getText());
                if (text.length() > 0)
                    checkAndSendTextMessageToDatabase(text);
                getMessagesFromDatabase(false);
                textMessage.setText("");
                textMessage.requestFocus();
                return true;
            }
            return false;
        });  // отпрака фото по нажатию энтер на клавиатуре
    }

    private void createSendMessageButton(@NonNull Dialog dialog) {
        ImageButton sendMessageButton = dialog.findViewById(R.id.buttonSend);   // Отправка сообщения
        sendMessageButton.setBackgroundResource(R.drawable.send_message);
        sendMessageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        sendMessageButton.setOnClickListener(v1 -> {
            String text = String.valueOf(textMessage.getText());
            if (text.length() > 0)
                checkAndSendTextMessageToDatabase(text);
            getMessagesFromDatabase(false);
            textMessage.setText("");
            textMessage.requestFocus();
        });
    }

    private void createUploadFileButton(@NonNull Dialog dialog) {
        ImageButton uploadFileButton = dialog.findViewById(R.id.buttonSendFile);   // Отправка файла
        uploadFileButton.setBackgroundResource(R.drawable.send_file);
        uploadFileButton.setOnClickListener(v1 -> {
            onSendFileButtonClick(((Activity) context));
            textMessage.requestFocus();
        });

    }

    private void createOpenCameraButton(@NonNull Dialog dialog) {
        ImageButton openCameraButton = dialog.findViewById(R.id.buttonPhoto);   // Открыть камеру
        openCameraButton.setBackgroundResource(R.drawable.button_photo);
        openCameraButton.setOnClickListener(v1 -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
            } else {
                openPhotoLoaderIntents(context);
            }
        });
    }

    private void createCloseMessengerButton(@NonNull Dialog dialog) {
        ImageButton closeMessengerButton = dialog.findViewById(R.id.close_massager);  // Кнопка закрыть
        closeMessengerButton.setOnClickListener(v12 -> dialog.dismiss());
        closeMessengerButton.setBackgroundResource(R.drawable.close_button);
    }

    private void createProgressBar(@NonNull Dialog dialog) {
        progressText = dialog.findViewById(R.id.progressText);
        progressText.setVisibility(View.INVISIBLE);
        progressBar = dialog.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void createNewMessageAnnounces(@NonNull Dialog dialog) {
        newMessageButton = dialog.findViewById(R.id.new_message);
        newMessageButton.setBackgroundResource(R.drawable.new_message_arrived);
        newMessageButton.setOnClickListener(v14 -> {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            newMessageButton.setVisibility(View.GONE);
        });
        newMessageButton.setBackgroundResource(R.drawable.new_message_arrived);
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
