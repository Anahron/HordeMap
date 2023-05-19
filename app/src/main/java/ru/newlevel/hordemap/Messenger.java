package ru.newlevel.hordemap;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class Messenger {

    @SuppressLint("StaticFieldLeak")
    private static Messenger instance = null;
    private Handler handler;

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
    private HordeMapViewModel viewModel;

    public static Messenger getInstance() {
        if (instance == null) {
            instance = new Messenger();
        }
        return instance;
    }

    public ImageButton getMessengerButton() {
        return messengerButton;
    }

    void createMessenger(Context context, HordeMapViewModel viewModel, Dialog dialog) {
        this.dialog = dialog;
        this.context = context;
        this.viewModel = viewModel;
        handler = new Handler();
        createMessengerButton();
        createDialog();
        createNewMessageAnnounces(dialog);
        createAndSetupRecyclerView();

        messengerButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MapsActivity.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) MapsActivity.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }

            createProgressBar(dialog);
            createScrollDownButton(dialog);
            createEditTextMessage(dialog);
            createSendMessageButton(dialog);
            createUploadFileButton(dialog);
            createOpenCameraButton(dialog);
            createCloseMessengerButton(dialog);
            viewModel.loadMessagesListener();

            // Наблюдаем за изменениями в LiveData с сообщениями
            viewModel.getMessagesLiveData().observe((LifecycleOwner) context, messages -> {
                System.out.println(" отправляем типа изменения в  adapter.setMessages(messages);");
                if (messages.size() > 0)
                    adapter.setMessages(messages);
            });

            // Слушатель прогресса загрузки/отправки файла
            viewModel.getProgressLiveData().observe((LifecycleOwner) context, progress -> {
                System.out.println("Прогресс загрузки " + progress);
                progressBar.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(progress, true);
                } else
                    progressBar.setProgress(progress);

                progressText.setVisibility(View.VISIBLE);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                }
            });
            
            Runnable updateMessagesEveryMin = new Runnable() {
                @Override
                public void run() {
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
                viewModel.getMessagesLiveData().removeObservers((LifecycleOwner) context);
                viewModel.getProgressLiveData().removeObservers((LifecycleOwner) context);
                messengerButton.setBackgroundResource(R.drawable.nomassage);
                viewModel.stopLoadMessages();
            });
            dialog.show();
        });
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
                viewModel.uploadFile(fileUri);
            }
        }
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            if (data != null) {
                photoUri = data.getData();
            }
            viewModel.uploadFile(photoUri);
        }

    }

    @SuppressLint("IntentReset")
    private void openPhotoButtonClick(@NonNull Context context) {
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

    private void createMessengerButton() {
        messengerButton = ((Activity) context).findViewById(R.id.message);
        messengerButton.setBackgroundResource(R.drawable.nomassage);
        messengerButton.setClickable(false);
    }

    private void createDialog() {
        dialog.setContentView(R.layout.activity_messages);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void createAndSetupRecyclerView() {
        recyclerView = dialog.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new MessagesAdapter(recyclerView, newMessageButton);
        recyclerView.setAdapter(adapter);
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
                    viewModel.sendMessage(text);
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
                viewModel.sendMessage(text);
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
                openPhotoButtonClick(context);
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

}
