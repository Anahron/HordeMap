package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataUpdateService.locationHistory;
import static ru.newlevel.hordemap.DataUpdateService.sendTimesList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.FirebaseApp;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static GoogleMap gMap;
    public static Boolean permissionForGeoUpdate = false;
    private boolean IsNeedToSave = true;
    private Polyline routePolyline;
    private PopupWindow popupWindow;
    private TextView distanceTextView;
    private View viewPopupMenu2;
    private View viewPopupMenu1;
    private ImageButton messengerButton;
    @SuppressLint("StaticFieldLeak")
    public static TextView AzimuthTextView;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static HordeMapViewModel viewModel;
    private KmzLoader kmzLoader;
    private Polyline polyline;
    private Dialog dialog;
    private int mapType;
    private long timeOfTurnOnPause;
    public static int MARKER_SIZE_USERS = 60;
    public static int MARKER_SIZE_CUSTOMS = 50;
    public static int TIME_TO_SEND_DATA = 30000;
    public static boolean isInactive = false;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1003;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION = 1004;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 1005;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1007;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1008;
    private static final int REQUEST_CODE_FOREGROUND_SERVICE = 1012;
    private static final int MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 1013;
    private static final int MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS = 1006;

    public static Context getContext() {
        return context;
    }

    public static HordeMapViewModel getViewModel() {
        return viewModel;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100)
            kmzLoader.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Messenger.REQUEST_CODE_SELECT_FILE || requestCode == Messenger.REQUEST_CODE_CAMERA)
            Messenger.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    protected void onDestroy() {
        MyServiceUtils.stopGeoUpdateService();
        if (IsNeedToSave && locationHistory.size() > 0)
            PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 1))));
        super.onDestroy();
    }

    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @SuppressLint("BatteryLife")
    protected void setPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            if (gMap != null)
                enableMyLocationAndClicksListener();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, REQUEST_CODE_FOREGROUND_SERVICE);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission_group.SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.SENSORS}, MY_PERMISSIONS_REQUEST_SENSOR);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, MY_PERMISSIONS_REQUEST_WAKE_LOCK);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION);
            }
        }

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void getPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("HordeMapPref", Context.MODE_PRIVATE);
        try {
            mapType = prefs.getInt("mapType", 4);
            MARKER_SIZE_USERS = prefs.getInt("markerUserSize", 50);
            MARKER_SIZE_CUSTOMS = prefs.getInt("markerCustomSize", 50);
            TIME_TO_SEND_DATA = prefs.getInt("timeToUpdate", 30000);
            User.getInstance().setMarker(prefs.getInt("myMarker", 0));
        } catch (Exception e) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            getPrefs();
        }
    }

    @SuppressLint("SetTextI18n")
    private void openDialogCloseBySystem(int type) {
        SharedPreferences prefs = context.getSharedPreferences("HordeMapPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isCloseBySystem", false);
        editor.apply();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Предупреждение");
        TextView textMessage = new TextView(context);
        if (type == 1)
            textMessage.setText(" Сервис слишком редко получал обновления GPS в фоновом режиме, для корректной работы пожалуйста перейдите в настройки и отключите режим оптимизации/автоматическое управление для приложения");
        else
            textMessage.setText(" Сервис НЕ получал обновления GPS в фоновом режиме, для корректной работы пожалуйста перейдите в настройки и отключите режим оптимизации/автоматическое управление батареи для приложения");
        textMessage.setPadding(25, 25, 25, 25);
        builder.setView(textMessage);
        builder.setPositiveButton("Перейти в настройки", (dialog, which) -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            dialog.dismiss();
        });
        builder.setNegativeButton("закрыть", (dialog, which) -> dialog.dismiss());
        dialog.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInactive = false;
        context = this;

        FirebaseApp.initializeApp(this);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (viewModel == null)
            viewModel = new ViewModelProvider(this).get(HordeMapViewModel.class);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getPrefs();

        dialog = new Dialog(context, R.style.AlertDialogNoMargins);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createRightButtons();
        }
        Messenger.getInstance().createMessenger(context, viewModel, dialog, messengerButton);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        createToolbar();
        LoginRequest.logIn(context, this);

        viewModel.getCustomMarkersLiveData().observe(this, MarkersHandler::createCustomMapMarkers);
        viewModel.getUsersMarkersLiveData().observe(this, MarkersHandler::createAllUsersMarkers);
        viewModel.getIsHaveNewMessages().observe(this, isNewMessage -> {
            if (isNewMessage)
                messengerButton.setBackgroundResource(R.drawable.yesmassage);
        });
        if (KmzLoader.savedKmlLayer != null)
            KmzLoader.savedKmlLayer.addLayerToMap();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createRightButtons() {
        messengerButton = findViewById(R.id.message);
        messengerButton.setBackgroundResource(R.drawable.nomassage);
        messengerButton.setClickable(false);
        ImageButton mapTypeButton = findViewById(R.id.map_type);
        if (mapType == 4)
            mapTypeButton.setBackgroundResource(R.drawable.map_type_normal);
        else
            mapTypeButton.setBackgroundResource(R.drawable.map_type_hybrid);
        mapTypeButton.setOnClickListener(d -> {
            setPermission();
            SharedPreferences prefs = context.getSharedPreferences("HordeMapPref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            int mapType = gMap.getMapType();
            switch (mapType) {
                case GoogleMap.MAP_TYPE_NORMAL:
                    gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    mapTypeButton.setBackgroundResource(R.drawable.map_type_normal);
                    editor.putInt("mapType", 4);
                    editor.apply();
                    break;
                case GoogleMap.MAP_TYPE_HYBRID:
                    gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    mapTypeButton.setBackgroundResource(R.drawable.map_type_hybrid);
                    editor.putInt("mapType", 1);
                    editor.apply();
                    break;
            }
        });
    }

    public static void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("InflateParams")
    private Button createMenuButtons2(int heightPx) {
        Button menuButton2 = new Button(this);
        menuButton2.setBackgroundResource(R.drawable.menu);
        menuButton2.setLayoutParams(new ViewGroup.LayoutParams(heightPx * 100 / 60, heightPx * 9 / 10));
        menuButton2.setText("PATHS");
        menuButton2.setShadowLayer(5, 1, 1, Color.parseColor("#a89c6a"));
        menuButton2.setTextColor(Color.parseColor("#d4bd61"));
        menuButton2.setTextSize(15);
        menuButton2.setOnClickListener(v -> {
            viewPopupMenu2 = LayoutInflater.from(context).inflate(R.layout.pupup_menu2, null, false);
            popupWindow.setContentView(viewPopupMenu2);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);
            if (popupWindow.isShowing()) popupWindow.dismiss();
            else popupWindow.showAsDropDown(menuButton2);

            createMenuItem1clear();
            createMenuItem2showPath();
            createMenuItem3hidePath();
            createMenuItem4savePath();
            createMenuItem5LoadPath();
            createMenuItem6DeletePath();
        });
        return menuButton2;
    }

    private void createMenuItem1clear() {
        Button menuItem1clear = viewPopupMenu2.findViewById(R.id.menu2_item1);  // Очистка пути
        menuItem1clear.setBackgroundResource(R.drawable.menubutton);
        menuItem1clear.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem1clear.setOnClickListener(s -> {
            locationHistory.clear();
            Toast.makeText(context, "Текущий путь очищен", Toast.LENGTH_LONG).show();
            popupWindow.dismiss();
        });
    }

    private void createMenuItem2showPath() {
        Button menuItem2showPath = viewPopupMenu2.findViewById(R.id.menu2_item2);  //Показать путь
        menuItem2showPath.setBackgroundResource(R.drawable.menubutton);
        menuItem2showPath.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem2showPath.setOnClickListener(s -> {
            if (locationHistory.isEmpty())
                Toast.makeText(context, "Записаного пути нет.", Toast.LENGTH_LONG).show();
            else {
                if (polyline != null)
                    polyline.remove();
                MarkersHandler.markersOn();
                PolylineOptions polylineOptions = new PolylineOptions().addAll(locationHistory).jointType(JointType.ROUND).startCap(new SquareCap()).endCap(new RoundCap()).geodesic(true).color(Color.RED) // Задаем цвет линии
                        .width(10); // Задаем ширину линии
                polyline = gMap.addPolyline(polylineOptions);
            }
            popupWindow.dismiss();
        });
    }

    private void createMenuItem3hidePath() {
        Button menuItem3hidePath = viewPopupMenu2.findViewById(R.id.menu2_item3);  // Скрыть (на карте)
        menuItem3hidePath.setBackgroundResource(R.drawable.menubutton);
        menuItem3hidePath.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem3hidePath.setOnClickListener(s -> {
            if (polyline != null)
                polyline.remove();
            popupWindow.dismiss();
        });
    }

    private void createMenuItem4savePath() {
        Button menuItem4savePath = viewPopupMenu2.findViewById(R.id.menu2_item4); //Сохранение пути в файловую систему
        menuItem4savePath.setBackgroundResource(R.drawable.menubutton);
        menuItem4savePath.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem4savePath.setOnClickListener(s -> {
            if (!locationHistory.isEmpty()) {
                PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(locationHistory)));
                Toast.makeText(context, "Текущий путь сохранен и очищен", Toast.LENGTH_LONG).show();
                locationHistory.clear();
            } else {
                Toast.makeText(context, "Нет данных для записи", Toast.LENGTH_LONG).show();
            }
            IsNeedToSave = false;
            popupWindow.dismiss();
        });
    }

    private void createMenuItem5LoadPath() {
        Button menuItem5LoadPath = viewPopupMenu2.findViewById(R.id.menu2_item5); // Загрузка путей из файловой системы
        menuItem5LoadPath.setBackgroundResource(R.drawable.menubutton);
        menuItem5LoadPath.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem5LoadPath.setOnClickListener(s -> {
            Hashtable<String, List<LatLng>> hashtable = PolylineSaver.getKeys();
            System.out.println("в меню итем 3 " + hashtable);
            if (hashtable.isEmpty()) {
                Toast.makeText(context, "Записанного пути нет :(", Toast.LENGTH_LONG).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Выберите сохраненный файл");
                builder.setItems(hashtable.keySet().toArray(new String[0]), (dialog, which) -> {
                    String selectedItem = hashtable.keySet().toArray(new String[0])[which];
                    Toast.makeText(context, "Выбран элемент: " + selectedItem, Toast.LENGTH_SHORT).show();
                    List<LatLng> polylines = hashtable.get(selectedItem);
                    assert polylines != null;
                    if (polylines.isEmpty()) {
                        Toast.makeText(context, "Записанного пути нет :(", Toast.LENGTH_LONG).show();
                    } else {
                        PolylineOptions polylineOptions2 = new PolylineOptions()   // тест 1
                                .addAll(polylines).jointType(JointType.ROUND).startCap(new RoundCap()).endCap(new SquareCap()).geodesic(true).color(Color.BLACK).width(10);
                        polyline = gMap.addPolyline(polylineOptions2);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            popupWindow.dismiss();
        });
    }

    private void createMenuItem6DeletePath() {
        Button menuItem6DetelePath = viewPopupMenu2.findViewById(R.id.menu2_item6);  // Удаление записанных путей
        menuItem6DetelePath.setBackgroundResource(R.drawable.menubutton);
        menuItem6DetelePath.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem6DetelePath.setOnClickListener(s -> {
            Toast.makeText(context, "Все записанные пути удалены.", Toast.LENGTH_LONG).show();
            PolylineSaver.deleteAll(context);
            popupWindow.dismiss();
        });
    }

    @SuppressLint("InflateParams")
    private Button createMenuButtons(int heightPx) {
        Button menuButton = new Button(this);
        menuButton.setBackgroundResource(R.drawable.menu);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(heightPx * 100 / 60, heightPx * 9 / 10);
        layoutParams2.setMarginEnd(convertDpToPx(7));

        menuButton.setLayoutParams(layoutParams2);
        menuButton.setText("MENU");
        menuButton.setShadowLayer(5, 1, 1, Color.parseColor("#a89c6a"));
        menuButton.setTextColor(Color.parseColor("#d4bd61"));
        menuButton.setTextSize(15);
        menuButton.setOnClickListener(v -> {
            viewPopupMenu1 = LayoutInflater.from(context).inflate(R.layout.pupup_menu, null, false);
            popupWindow.setContentView(viewPopupMenu1);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);

            if (popupWindow.isShowing()) popupWindow.dismiss();
            else popupWindow.showAsDropDown(menuButton);

            createMenuItemInfoDialog();
            createMenuItemLoadMapKMZ();
            createMenuItemMarkerOptions();
            createMenuItemShowDistance();
            createMenuItemLogOut();
            createMenuItemCloseApp();

            popupWindow.showAtLocation(menuButton, Gravity.TOP | Gravity.START, 0, 0);
        });
        return menuButton;
    }

    private void createMenuItemInfoDialog() {
        Button menuItem0 = viewPopupMenu1.findViewById(R.id.menu_item0);
        menuItem0.setBackgroundResource(R.drawable.menubutton);
        menuItem0.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem0.setOnClickListener(s -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.dialog_user_info, null);
            TextView textUserName = view.findViewById(R.id.text_user_name);
            textUserName.setText(User.getInstance().getUserName());
            TextView textRoomNumber = view.findViewById(R.id.text_room_number);
            textRoomNumber.setText(User.getInstance().getRoomId());
            TextView textDeviceId = view.findViewById(R.id.text_device_id);
            textDeviceId.setText(User.getInstance().getDeviceId());
            builder.setView(view);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
            popupWindow.dismiss();
        });
    }

    private void createMenuItemLoadMapKMZ() {
        Button menuItem1 = viewPopupMenu1.findViewById(R.id.menu_item1);  // Загрузка карты полигона
        menuItem1.setBackgroundResource(R.drawable.menubutton);
        menuItem1.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem1.setOnClickListener(s -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
            } else {
                kmzLoader = new KmzLoader(context, gMap);
                kmzLoader.openFilePicker(MapsActivity.this);
            }
            popupWindow.dismiss();
        });
    }

    @SuppressLint({"SetTextI18n", "SuspiciousIndentation"})
    private void createMenuItemMarkerOptions() {
        Button menuItem2 = viewPopupMenu1.findViewById(R.id.menu_item2);
        menuItem2.setBackgroundResource(R.drawable.menubutton);
        menuItem2.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem2.setOnClickListener(s -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_marker_options, null);
            // юзер маркера
            SeekBar seekBarUsersMarker = view.findViewById(R.id.seekBar2);
            seekBarUsersMarker.setProgress(MARKER_SIZE_USERS);
            // кастом маркера
            SeekBar seekBarCustomMarkers = view.findViewById(R.id.seekBar3);
            seekBarCustomMarkers.setProgress(MARKER_SIZE_CUSTOMS);

            // время обновления
            SeekBar seekBarTime = view.findViewById(R.id.seekBar4);
            seekBarTime.setProgress(TIME_TO_SEND_DATA / 1000);
            TextView textTime = view.findViewById(R.id.textTimeToSendData);
            textTime.setText(TIME_TO_SEND_DATA / 1000 + " сек.");
            // примеры маркеров
            ImageView imageUserMarker = view.findViewById(R.id.imageUserMarker);
            imageUserMarker.setImageResource(R.drawable.pngwing);
            ImageView imageCustomMarker = view.findViewById(R.id.imageCustomMarker);
            imageCustomMarker.setImageResource(R.drawable.swords_icon);
            // установка текущего размера маркеров
            ViewGroup.LayoutParams paramsCustomMarker = imageCustomMarker.getLayoutParams();
            paramsCustomMarker.width = MARKER_SIZE_CUSTOMS;
            paramsCustomMarker.height = MARKER_SIZE_CUSTOMS;
            imageCustomMarker.setLayoutParams(paramsCustomMarker);

            ViewGroup.LayoutParams paramsUserMarker = imageUserMarker.getLayoutParams();
            paramsUserMarker.width = MARKER_SIZE_USERS;
            paramsUserMarker.height = MARKER_SIZE_USERS;
            imageUserMarker.setLayoutParams(paramsUserMarker);
            // слушатели сикбаров
            seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textTime.setText(Math.max(seekBarTime.getProgress(), 10) + " сек.");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBarCustomMarkers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    paramsCustomMarker.width = Math.max(progress, 20);
                    paramsCustomMarker.height = Math.max(progress, 20);
                    imageCustomMarker.setLayoutParams(paramsCustomMarker);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBarUsersMarker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    paramsUserMarker.width = Math.max(progress, 20);
                    paramsUserMarker.height = Math.max(progress, 20);
                    imageUserMarker.setLayoutParams(paramsUserMarker);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            // выбор пользовательского маркера
            RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
            RadioButton radioButton0 = view.findViewById(R.id.radioButton0);
            RadioButton radioButton1 = view.findViewById(R.id.radioButton1);
            RadioButton radioButton2 = view.findViewById(R.id.radioButton2);
            RadioButton radioButton3 = view.findViewById(R.id.radioButton3);
            RadioButton radioButton4 = view.findViewById(R.id.radioButton4);
            AtomicInteger checkedButton = new AtomicInteger(0);
            switch (User.getInstance().getMarker()) {
                case 1:
                    radioButton1.setAlpha(1f);
                    checkedButton.set(1);
                    break;
                case 2:
                    radioButton2.setAlpha(1f);
                    checkedButton.set(2);
                    break;
                case 3:
                    radioButton3.setAlpha(1f);
                    checkedButton.set(3);
                    break;
                case 4:
                    radioButton4.setAlpha(1f);
                    checkedButton.set(4);
                    break;
                default:
                    radioButton0.setAlpha(1f);
                    checkedButton.set(0);
            }

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                // Сначала делаем все кнопки полупрозрачными
                radioButton0.setAlpha(0.3f);
                radioButton1.setAlpha(0.3f);
                radioButton2.setAlpha(0.3f);
                radioButton3.setAlpha(0.3f);
                radioButton4.setAlpha(0.3f);
                // Затем делаем выбранную кнопку полностью видимой
                RadioButton checkedRadioButton = view.findViewById(checkedId);
                checkedRadioButton.setAlpha(1.0f);
                checkedButton.set(Integer.parseInt(checkedRadioButton.getTag().toString()));
            });

            builder.setPositiveButton("Применить", (dialog, which) -> {
                int seekBarUsersMarkerValue = Math.max(seekBarUsersMarker.getProgress(), 20);
                int seekBarCustomMarkersValue = Math.max(seekBarCustomMarkers.getProgress(), 20);
                int seekBarTimeValue = Math.max(seekBarTime.getProgress(), 10);
                SharedPreferences prefs = context.getSharedPreferences("HordeMapPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("markerUserSize", seekBarUsersMarkerValue);
                editor.putInt("markerCustomSize", seekBarCustomMarkersValue);
                editor.putInt("timeToUpdate", seekBarTimeValue * 1000);
                editor.putInt("myMarker", checkedButton.intValue());
                editor.apply();
                User.getInstance().setMarker(checkedButton.intValue());
                TIME_TO_SEND_DATA = seekBarTimeValue * 1000;
                MARKER_SIZE_CUSTOMS = seekBarCustomMarkersValue;
                MARKER_SIZE_USERS = seekBarUsersMarkerValue;
                if (permissionForGeoUpdate)
                    DataUpdateService.getInstance().restartSendGeoTimer(TIME_TO_SEND_DATA);
                MarkersHandler.reCreateMarkers();
            });
            builder.setNegativeButton("Сбросить", (dialog, which) -> {
                SharedPreferences prefs = context.getSharedPreferences("HordeMapPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                getPrefs();
                if (permissionForGeoUpdate)
                    DataUpdateService.getInstance().restartSendGeoTimer(TIME_TO_SEND_DATA);
                MarkersHandler.reCreateMarkers();
            });
            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.show();
            popupWindow.dismiss();
        });
    }

    private void createMenuItemShowDistance() {
        Button menuItem3 = viewPopupMenu1.findViewById(R.id.menu_item3);
        menuItem3.setBackgroundResource(R.drawable.menubutton);
        menuItem3.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem3.setOnClickListener(s -> {
            if (locationHistory.isEmpty())
                Toast.makeText(context, "Записаного пути нет.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "Пройденная дистанция: " + ((int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22)))) + " метров.", Toast.LENGTH_LONG).show();
            popupWindow.dismiss();
        });
    }

    private void createMenuItemLogOut() {
        Button menuItem4 = viewPopupMenu1.findViewById(R.id.menu_item4);
        menuItem4.setBackgroundResource(R.drawable.menubutton);
        menuItem4.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem4.setOnClickListener(s -> {
            LoginRequest.logOut();
            LoginRequest.logIn(context, this);
            popupWindow.dismiss();
        });
    }

    private void createMenuItemCloseApp() {
        Button menuItem5 = viewPopupMenu1.findViewById(R.id.menu_item5);
        menuItem5.setBackgroundResource(R.drawable.menubutton);
        menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
        menuItem5.setOnClickListener(s -> {
            if (IsNeedToSave && locationHistory.size() > 0)
                PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
            MyServiceUtils.stopGeoUpdateService();
            System.exit(0);
            popupWindow.dismiss();
        });
    }

    private Button createMarkerSwitch(int heightPx) {
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch markerswitch = new Switch(this);
        markerswitch.setBackgroundResource(R.drawable.custom_switch);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((heightPx * 6) / 10, (heightPx * 6) / 10);
        layoutParams.setMarginStart(convertDpToPx(28));
        layoutParams.setMarginEnd(convertDpToPx(20));
        markerswitch.setLayoutParams(layoutParams);
        markerswitch.setChecked(true);
        markerswitch.setThumbDrawable(null);
        markerswitch.setText(null);
        markerswitch.setTextOff(null);
        markerswitch.setTextOn(null);
        markerswitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Обработка изменения состояния переключателя
            if (isChecked && permissionForGeoUpdate) {
                MarkersHandler.isMarkersON = true;
                MarkersHandler.markersOn();
            } else {
                MarkersHandler.isMarkersON = false;
                MarkersHandler.markersOff();
            }
        });
        return markerswitch;
    }

    private void createToolbar() {
        popupWindow = new PopupWindow(context);
        distanceTextView = findViewById(R.id.distance_text_view);
        distanceTextView.setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPadding(convertDpToPx(0), 0, convertDpToPx(0), convertDpToPx(5));

        // Получаем размер тулбара для выставления размера кнопок
        TypedValue tv = new TypedValue();
        Drawable logo = ContextCompat.getDrawable(context, R.drawable.toolbar);
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int heightPx = getResources().getDimensionPixelSize(tv.resourceId);
        toolbar.setBackground(logo);

        // Добавляем менюшки, свитч маркеров, передаем высоту тулбара
        toolbar.addView(createMenuButtons(heightPx));
        toolbar.addView(createMenuButtons2(heightPx));
        toolbar.addView(createMarkerSwitch(heightPx));

        //Добавляем тект направления
        AzimuthTextView = new TextView(this);
        AzimuthTextView.setTextColor(Color.parseColor("#FFe6ce6b"));
        AzimuthTextView.setTextSize(12F);
        Toolbar.LayoutParams layoutParams1 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_HORIZONTAL;
        toolbar.addView(AzimuthTextView, layoutParams1);

        //Компас вью
        CompassView compassView = findViewById(R.id.compass_view);
        compassView.setVisibility(View.INVISIBLE);
        compassView.compassOFF();
        AzimuthTextView.setOnClickListener(v -> {
            if (compassView.getVisibility() == View.VISIBLE) {
                compassView.setVisibility(View.INVISIBLE);
                compassView.compassOFF();
                AzimuthTextView.setTextSize(12F);
                AzimuthTextView.setText("COMPAS");
            } else {
                compassView.setVisibility(View.VISIBLE);
                compassView.compassON();
            }
        });
        AzimuthTextView.setTextSize(12F);
        AzimuthTextView.setText("COMPAS");
    }

    @SuppressLint("SetTextI18n")
    private void buildRoute(LatLng destination) {
        if (routePolyline != null) {
            routePolyline.remove();
        }
        List<LatLng> polylineCoordinates = Arrays.asList(new LatLng(gMap.getMyLocation().getLatitude(), gMap.getMyLocation().getLongitude()), destination);
        Bitmap bitmapcustomcap = BitmapFactory.decodeResource(context.getResources(), R.drawable.star);
        BitmapDescriptor bitmapcustomcapicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcustomcap, 60, 60, false));
        CustomCap customCap = new CustomCap(bitmapcustomcapicon);

        PolylineOptions polylineOptions = new PolylineOptions().addAll(polylineCoordinates).endCap(customCap).geodesic(true).color(Color.YELLOW).width(6);
        double distance = SphericalUtil.computeDistanceBetween(new LatLng(gMap.getMyLocation().getLatitude(), gMap.getMyLocation().getLongitude()), destination);

        distanceTextView.setVisibility(View.VISIBLE);
        if ((int) distance > 1000)
            distanceTextView.setText((Math.round(distance / 10) / 100.0) + " км.");
        else distanceTextView.setText((int) distance + " м.");

        routePolyline = gMap.addPolyline(polylineOptions);

        // Слежение за изменением местоположения пользователя
        gMap.setOnMyLocationChangeListener(location -> {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Обновление полилинии маршрута с учетом нового местоположения
            List<LatLng> updatedPolylineCoordinates = Arrays.asList(currentLatLng, destination);
            routePolyline.setPoints(updatedPolylineCoordinates);
            double distance1 = SphericalUtil.computeDistanceBetween(currentLatLng, destination);
            if ((int) distance1 > 1000)
                distanceTextView.setText((Math.round(distance1 / 10) / 100.0) + " км.");
            else distanceTextView.setText((int) distance1 + " м.");
        });
    }

    private void enableMyLocationAndClicksListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
            gMap.setOnMapLongClickListener(latLng -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Выберите действие").setItems(new CharSequence[]{"Показать расстояние до точки", "Построить маршрут", "Очистить маршрут", "Поставить маркер"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Показать расстояние до точки
                            float[] distance = new float[1];
                            Location.distanceBetween(DataUpdateService.getLatitude(), DataUpdateService.getLongitude(), latLng.latitude, latLng.longitude, distance);
                            Toast.makeText(context, "Расстояние до точки: " + (distance[0] > 1000 ? (Math.round(distance[0] / 10) / 100.0) : (int) distance[0]) + " м", Toast.LENGTH_LONG).show();
                            break;
                        case 1:
                            // Построить маршрут
                            buildRoute(latLng);
                            break;
                        case 2:
                            // Очистить маршрут
                            if (routePolyline != null) {
                                routePolyline.remove();
                            }
                            distanceTextView.setVisibility(View.INVISIBLE);
                            break;
                        case 3:
                            AlertDialog.Builder dialogMarkerBuilder = new AlertDialog.Builder(context);
                            dialogMarkerBuilder.setTitle(" Выберите иконку ");

                            // Загрузка макета для диалогового окна
                            LayoutInflater inflater = LayoutInflater.from(context);
                            View dialogView = inflater.inflate(R.layout.dialog_marker_info, null);
                            dialogMarkerBuilder.setView(dialogView);

                            // Получение элементов управления из макета диалогового окна
                            ImageView icon1 = dialogView.findViewById(R.id.icon1_focus);
                            ImageView icon2 = dialogView.findViewById(R.id.icon2_swords);
                            ImageView icon3 = dialogView.findViewById(R.id.icon3_flag_red);
                            ImageView icon4 = dialogView.findViewById(R.id.icon4_flag_yellow);
                            ImageView icon5 = dialogView.findViewById(R.id.icon5_flag_green);
                            ImageView icon6 = dialogView.findViewById(R.id.icon6_flag_blue);

                            EditText descriptionEditText = dialogView.findViewById(R.id.description_edit_text);
                            EditText numberPointText = dialogView.findViewById(R.id.description_edit_text_number);
                            String[] description = {"Маркер"};
                            int[] selectedIcon = {0};
                            icon1.setOnClickListener(v -> {
                                selectedIcon[0] = 0;
                                icon1.setAlpha(1F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                                numberPointText.setVisibility(View.VISIBLE);
                            });
                            icon2.setOnClickListener(v -> {
                                selectedIcon[0] = 1;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(1F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                                numberPointText.setVisibility(View.GONE);
                            });
                            icon3.setOnClickListener(v -> {
                                selectedIcon[0] = 2;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(1F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                                numberPointText.setVisibility(View.GONE);
                            });
                            icon4.setOnClickListener(v -> {
                                selectedIcon[0] = 3;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(1F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                                numberPointText.setVisibility(View.GONE);
                            });
                            icon5.setOnClickListener(v -> {
                                selectedIcon[0] = 4;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(1F);
                                icon6.setAlpha(0.3F);
                                numberPointText.setVisibility(View.GONE);
                            });
                            icon6.setOnClickListener(v -> {
                                selectedIcon[0] = 5;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(1F);
                                numberPointText.setVisibility(View.GONE);
                            });

                            // Установка кнопки "Отмена"
                            dialogMarkerBuilder.setNegativeButton("Отмена", (dialogInterface, which1) -> dialogInterface.dismiss());
                            // Установка кнопки "Поставить маркер"
                            dialogMarkerBuilder.setPositiveButton("Поставить маркер", (dialogInterface, which1) -> {
                                String selectedIconFinal = "10";
                                if (selectedIcon[0] == 0) {
                                    if (numberPointText.getText().toString().length() > 0)
                                        selectedIconFinal = "1" + numberPointText.getText();
                                    if (descriptionEditText.getText().toString().length() > 0)
                                        description[0] = String.valueOf(descriptionEditText.getText());
                                    viewModel.sendMarkerData(latLng.latitude, latLng.longitude, Integer.parseInt(selectedIconFinal), description[0]);
                                } else if (descriptionEditText.getText().toString().length() > 0)
                                    description[0] = String.valueOf(descriptionEditText.getText());
                                viewModel.sendMarkerData(latLng.latitude, latLng.longitude, selectedIcon[0], description[0]);
                                dialogInterface.dismiss();
                            });
                            // Создание диалогового окна
                            dialogMarkerBuilder.create().show();
                            break;
                    }
                });
                builder.create().show();
            });
            gMap.setOnMapClickListener(latLng -> {
                double[] distance = new double[1];
                distance[0] = 25;
                if (polyline != null) {
                    boolean closestPoint = PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, distance[0]);
                    if (closestPoint) {
                        Toast.makeText(getApplicationContext(), "Дистанция: " + (int) SphericalUtil.computeLength(polyline.getPoints()) + " метров", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        enableMyLocationAndClicksListener();

//         Смещаем карту ниже тулбара
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        gMap.setPadding(0, actionBarHeight, 0, 0);

        gMap.setMapType(mapType);

//         Камера на Красноярск
        LatLng location = new LatLng(56.0901, 93.2329);   //координаты красноярска
//         Камера на полигон
//        LatLng location = new LatLng(52.079417, 47.731866);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 8));

        gMap.getUiSettings().setMyLocationButtonEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);
        gMap.getUiSettings().setZoomControlsEnabled(true);
//         Загружаем стационарые метки полигона
//         MarkersHandler.importantMarkersCreate();                            // пока не нужны
//         Показываем только текст маркера, без перемещения к нему камеры
        gMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });
//         Создание диалога удаления маркера по долгому клику на инфо
        gMap.setOnInfoWindowLongClickListener(this::deleteMarkerShowDialog);
//         Скрываем диалог при коротком клике по нему
        gMap.setOnInfoWindowClickListener(Marker::hideInfoWindow);
    }

    private void deleteMarkerShowDialog(Marker marker) {
        if (marker.getTag() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Удаление маркера").setMessage("Вы уверены, что хотите удалить маркер?").setPositiveButton("Да", (dialog, which) -> {
                // Удаление маркера
                if (marker.getTag() != null) {
                    viewModel.deleteMarker(marker);
                    marker.remove();
                    MarkersHandler.markersOff();
                    MarkersHandler.markersOn();
                }
            }).setNegativeButton("Нет", (dialog, which) -> dialog.dismiss()).show();
        }
    }

    private void checkGeoUpdatesTimes() {
        if (sendTimesList.size() > 5) {
            long times = 0L;
            long tempTime = 0L;
            for (Long time : sendTimesList) {
                if (tempTime != 0L)
                    times += time - tempTime;
                tempTime = time;
            }
            makeToast("Среднее время обновлений в фоне " + times / sendTimesList.size() / 1000 + "сек. количество отправок на сервер: " + sendTimesList.size());
            if (times / sendTimesList.size() > TIME_TO_SEND_DATA * 2L)
                openDialogCloseBySystem(1);
        } else if (timeOfTurnOnPause > 0 && System.currentTimeMillis() - timeOfTurnOnPause > 130000 && sendTimesList.size() < 2)
            openDialogCloseBySystem(0);
        sendTimesList.clear();
    }

    @Override
    protected void onPause() {
        viewModel.stopLoadGeoData();
        viewModel.stopLoadMessages();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInactive = false;
        if (permissionForGeoUpdate) {
            DataUpdateService.getInstance().switchToActiveState();
            if (dialog.isShowing())
                viewModel.loadMessagesListener();
            viewModel.loadGeoDataListener();
        }
        checkGeoUpdatesTimes();
        timeOfTurnOnPause = 0;
    }

    @Override
    protected void onStop() {
        isInactive = true;
        if (permissionForGeoUpdate) {
            timeOfTurnOnPause = System.currentTimeMillis();
            DataUpdateService.getInstance().switchToInactiveState();
        }
        super.onStop();
    }

}