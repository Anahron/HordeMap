package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MapsActivity.adapter;
import static ru.newlevel.hordemap.MapsActivity.MessengerButton;
import static ru.newlevel.hordemap.MapsActivity.photoUri;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Messenger {

    public static RecyclerView recyclerView;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    public static TextView progressText;
    private Handler handler;
    private static final int REQUEST_CODE_CAMERA = 11;

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

                DataUpdateService.getMessagesFromDatabase(true);

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
                        DataUpdateService.sendMassage(String.valueOf(textMassage.getText()));
                        DataUpdateService.getMessagesFromDatabase(false);
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
                    DataUpdateService.sendMassage(String.valueOf(textMassage.getText()));
                    DataUpdateService.getMessagesFromDatabase(false);
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
                    DataUpdateService.onSendFileButtonClick(((Activity) context));
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
                        DataUpdateService.getMessagesFromDatabase(false);
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
}
