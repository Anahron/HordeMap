package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataUpdateService.locationHistory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static GoogleMap gMap;
    public static Boolean permissionForGeoUpdate = false;

    private boolean IsNeedToSave = true;
    private Polyline routePolyline;
    private TextView distanceTextView;
    @SuppressLint("StaticFieldLeak")
    public static TextView AzimuthTextView;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private KmzLoader kmzLoader;
    private Polyline polyline;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1003;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION = 1004;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 1005;
    private static final int MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS = 1006;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1007;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1008;

    public static Context getContext() {
        return context;
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
        if (IsNeedToSave && locationHistory.size() > 0)
            PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 1))));
        super.onDestroy();
        finish();
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
            enableMyLocationAndClicksListener();
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        FirebaseApp.initializeApp(this);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        Messenger.getInstance().createMessenger(context);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        createToolbar();

        LoginRequest.logIn(context, this);
    }

    public static void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private Button createMenuButtons2(int heightPx) {
        Button menubutton2 = new Button(this);
        menubutton2.setBackgroundResource(R.drawable.menu);
        menubutton2.setLayoutParams(new ViewGroup.LayoutParams(heightPx * 100 / 60, heightPx * 9 / 10));
        menubutton2.setText("PATHS");
        menubutton2.setShadowLayer(5, 1, 1, Color.parseColor("#a89c6a"));
        menubutton2.setTextColor(Color.parseColor("#d4bd61"));
        menubutton2.setTextSize(15);
        menubutton2.setOnClickListener(v -> {
            PopupWindow popupWindow = new PopupWindow(context);
            @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.pupup_menu2, null, false);
            popupWindow.setContentView(view);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);
            if (popupWindow.isShowing()) popupWindow.dismiss();
            else popupWindow.showAsDropDown(menubutton2);

            Button menuItem1clear = view.findViewById(R.id.menu2_item1);  // Очистка пути
            menuItem1clear.setBackgroundResource(R.drawable.menubutton);
            menuItem1clear.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem1clear.setOnClickListener(s -> {
                locationHistory.clear();
                Toast.makeText(context, "Текущий путь очищен", Toast.LENGTH_LONG).show();
                popupWindow.dismiss();
            });

            Button menuItem2showPath = view.findViewById(R.id.menu2_item2);  //Показать путь
            menuItem2showPath.setBackgroundResource(R.drawable.menubutton);
            menuItem2showPath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem2showPath.setOnClickListener(s -> {
                if (locationHistory.isEmpty())
                    Toast.makeText(context, "Записаного пути нет.", Toast.LENGTH_LONG).show();
                else {
                    polyline.remove();
                    MarkersHandler.setVisibleForImportantMarkers();
                    MarkersHandler.markersOn();
                    PolylineOptions polylineOptions = new PolylineOptions().addAll(locationHistory).jointType(JointType.ROUND).startCap(new SquareCap()).endCap(new RoundCap()).geodesic(true).color(Color.RED) // Задаем цвет линии
                            .width(10); // Задаем ширину линии
                    polyline = gMap.addPolyline(polylineOptions);
                }
                popupWindow.dismiss();
            });

            Button menuItem3hidePath = view.findViewById(R.id.menu2_item3);  // Скрыть (на карте)
            menuItem3hidePath.setBackgroundResource(R.drawable.menubutton);
            menuItem3hidePath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem3hidePath.setOnClickListener(s -> {
                gMap.clear();
                MarkersHandler.setVisibleForImportantMarkers();
                MarkersHandler.markersOn();
                if (KmzLoader.savedKmlLayer != null)
                    KmzLoader.savedKmlLayer.addLayerToMap();
                popupWindow.dismiss();
            });

            Button menuItem4savepath = view.findViewById(R.id.menu2_item4); //Сохранение пути в файловую систему
            menuItem4savepath.setBackgroundResource(R.drawable.menubutton);
            menuItem4savepath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem4savepath.setOnClickListener(s -> {
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

            Button menuItem5Loadpath = view.findViewById(R.id.menu2_item5); // Загрузка путей из файловой системы
            menuItem5Loadpath.setBackgroundResource(R.drawable.menubutton);
            menuItem5Loadpath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5Loadpath.setOnClickListener(s -> {
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

            Button menuItem6detelepath = view.findViewById(R.id.menu2_item6);  // Удаление записанных путей
            menuItem6detelepath.setBackgroundResource(R.drawable.menubutton);
            menuItem6detelepath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem6detelepath.setOnClickListener(s -> {
                Toast.makeText(context, "Все записанные пути удалены.", Toast.LENGTH_LONG).show();
                PolylineSaver.deleteAll(context);
                popupWindow.dismiss();
            });
        });
        return menubutton2;
    }

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
            PopupWindow popupWindow = new PopupWindow(context);
            @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.pupup_menu, null, false);
            popupWindow.setContentView(view);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);
            if (popupWindow.isShowing()) popupWindow.dismiss();
            else popupWindow.showAsDropDown(menuButton);

            Button menuItem0 = view.findViewById(R.id.menu_item0);  // Смена типа карты
            menuItem0.setBackgroundResource(R.drawable.menubutton);
            menuItem0.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem0.setOnClickListener(s -> {
                int mapType = gMap.getMapType();
                switch (mapType) {
                    case GoogleMap.MAP_TYPE_NORMAL:
                        gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    case GoogleMap.MAP_TYPE_HYBRID:
                        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                }
                popupWindow.dismiss();
            });

            Button menuItem1 = view.findViewById(R.id.menu_item1);  // Загрузка карты полигона
            menuItem1.setBackgroundResource(R.drawable.menubutton);
            menuItem1.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem1.setOnClickListener(s -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Если разрешение не предоставлено, запросите его у пользователя
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_READ_EXTERNAL_STORAGE);
                } else {
                    kmzLoader = new KmzLoader(context, gMap);
                    kmzLoader.openFilePicker(MapsActivity.this);
                }

                popupWindow.dismiss();
            });

            Button menuItem2 = view.findViewById(R.id.menu_item2);  // Изменение размера маркеров
            menuItem2.setBackgroundResource(R.drawable.menubutton);
            menuItem2.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem2.setOnClickListener(s -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Размер маркеров");
                SeekBar seekBar = new SeekBar(context);
                builder.setView(seekBar);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    seekBar.setMin(10);
                }
                seekBar.setMax(120);
                seekBar.setProgress(60);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    int value = seekBar.getProgress();
                    MarkersHandler.MARKER_SIZE = value + 1;
                    MarkersHandler.markersOff();
                    MarkersHandler.markersOn();
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                popupWindow.dismiss();
            });

            Button menuItem3 = view.findViewById(R.id.menu_item3);   // Показать дистанцию
            menuItem3.setBackgroundResource(R.drawable.menubutton);
            menuItem3.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem3.setOnClickListener(s -> {
                if (locationHistory.isEmpty())
                    Toast.makeText(context, "Записаного пути нет.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(context, "Пройденная дистанция: " + ((int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22)))) + " метров.", Toast.LENGTH_LONG).show();
                popupWindow.dismiss();
            });

            Button menuItem4 = view.findViewById(R.id.menu_item4);  // Логаут
            menuItem4.setBackgroundResource(R.drawable.menubutton);
            menuItem4.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem4.setOnClickListener(s -> {
                LoginRequest.logOut(context);
                LoginRequest.logIn(context, this);
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menuButton, Gravity.TOP | Gravity.START, 0, 0);

            Button menuItem5 = view.findViewById(R.id.menu_item5);  // Закрыть
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                if (IsNeedToSave && locationHistory.size() > 0)
                    PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
                MyServiceUtils.stopGeoUpdateService(context);
                finish();
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menuButton, Gravity.TOP | Gravity.START, 0, 0);
        });
        return menuButton;
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
                System.out.println("Включение маркеров");
                MarkersHandler.isMarkersON = true;
                MarkersHandler.markersOn();
            } else {
                System.out.println("Выключение маркеров");
                MarkersHandler.isMarkersON = false;
                MarkersHandler.markersOff();
            }
        });
        return markerswitch;
    }

    private void createToolbar() {
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
                            dialogMarkerBuilder.setTitle(" Выберите иконку \n и введите название");

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
                            });
                            icon2.setOnClickListener(v -> {
                                selectedIcon[0] = 1;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(1F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                            });
                            icon3.setOnClickListener(v -> {
                                selectedIcon[0] = 2;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(1F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                            });
                            icon4.setOnClickListener(v -> {
                                selectedIcon[0] = 3;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(1F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(0.3F);
                            });
                            icon5.setOnClickListener(v -> {
                                selectedIcon[0] = 4;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(1F);
                                icon6.setAlpha(0.3F);
                            });
                            icon6.setOnClickListener(v -> {
                                selectedIcon[0] = 5;
                                icon1.setAlpha(0.3F);
                                icon2.setAlpha(0.3F);
                                icon3.setAlpha(0.3F);
                                icon4.setAlpha(0.3F);
                                icon5.setAlpha(0.3F);
                                icon6.setAlpha(1F);
                            });

                            // Установка кнопки "Отмена"
                            dialogMarkerBuilder.setNegativeButton("Отмена", (dialogInterface, which1) -> dialogInterface.dismiss());
                            // Установка кнопки "Поставить маркер"
                            dialogMarkerBuilder.setPositiveButton("Поставить маркер", (dialogInterface, which1) -> {
                                if (descriptionEditText.getText().toString().length() > 0)
                                    description[0] = String.valueOf(descriptionEditText.getText());
                                DataUpdateService.sendGeoMarkerToDatabase(User.getInstance().getUserName(), latLng.latitude, latLng.longitude, selectedIcon[0], description[0]);
                                DataUpdateService.getAllGeoDataFromDatabase();
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

        // Смещаем карту ниже тулбара
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        gMap.setPadding(0, actionBarHeight, 0, 0);
        gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Камера на Красноярск
        //  LatLng location = new LatLng(56.0901, 93.2329);   //координаты красноярска

        // Камера на полигон
        LatLng location = new LatLng(52.079417, 47.731866);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
        // Настраиваем карту
        gMap.getUiSettings().setMyLocationButtonEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        // Загружаем метки полигона
        MarkersHandler.importantMarkersCreate();
        // Показываем только текст маркера, без перемещения к нему камеры
        gMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        gMap.setOnInfoWindowLongClickListener(marker -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Удаление маркера").setMessage("Вы уверены, что хотите удалить маркер?").setPositiveButton("Да", (dialog, which) -> {
                // Удаление маркера
                DataUpdateService.deleteMarkerFromDatabase(marker);
                marker.remove();
            }).setNegativeButton("Нет", (dialog, which) -> {
                // Отмена удаления
                dialog.dismiss();
            }).show();
        });

        gMap.setOnInfoWindowClickListener(Marker::hideInfoWindow);

        if (KmzLoader.savedKmlLayer != null)
            KmzLoader.savedKmlLayer.addLayerToMap();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (KmzLoader.savedKmlLayer != null)
            KmzLoader.savedKmlLayer.addLayerToMap();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}