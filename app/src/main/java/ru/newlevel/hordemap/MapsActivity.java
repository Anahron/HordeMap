package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.latitude;
import static ru.newlevel.hordemap.DataSender.longitude;
import static ru.newlevel.hordemap.DataSender.sender;
import static ru.newlevel.hordemap.DataSender.locationHistory;
import static ru.newlevel.hordemap.KmlLayerLoaderTask.kmlSavedFile;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static GoogleMap mMap;
    public static Long id = 0L;
    public static String name;
    public static Boolean permission = false;
    private Polyline polyline;
    @SuppressLint("StaticFieldLeak")
    public static TextView textView1;
    private boolean IsNeedToSave = true;
    private Polyline routePolyline;
    private TextView distanceTextView;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1003;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_BACKGROUND_LOCATION = 1004;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 1005;
    private static final int MY_PERMISSIONS_REQUEST_SCHEDULE_EXACT_ALARMS = 1006;

    protected void onDestroy() {
        System.out.println("Вызван в мэйне");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
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
        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Запрет перезагрузки при повороте
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        createTollbar();
        LoginRequest.logIn(context, this);
        // Создаем меню
        distanceTextView = findViewById(R.id.distance_text_view);
        distanceTextView.setVisibility(View.GONE);
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
            View view = LayoutInflater.from(context).inflate(R.layout.pupup_menu2, null, false);
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
                // Создаем объект PolylineOptions
                if (locationHistory.isEmpty())
                    Toast.makeText(context, "Записаного пути нет.", Toast.LENGTH_LONG).show();
                else {
                    mMap.clear();
                    ImportantMarkers.importantMarkersCreate();
                    DataSender.apDateMarkers();
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(locationHistory).jointType(JointType.ROUND).startCap(new SquareCap()).endCap(new RoundCap()).geodesic(true).color(Color.RED) // Задаем цвет линии
                            .width(10); // Задаем ширину линии
                    // Добавляем Polyline на карту
                    polyline = mMap.addPolyline(polylineOptions);

                }
                popupWindow.dismiss();
            });

            Button menuItem3hidePath = view.findViewById(R.id.menu2_item3);  // Скрыть (на карте)
            menuItem3hidePath.setBackgroundResource(R.drawable.menubutton);
            menuItem3hidePath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem3hidePath.setOnClickListener(s -> {
                mMap.clear();
                ImportantMarkers.importantMarkersCreate();
                DataSender.apDateMarkers();
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
                            polyline = mMap.addPolyline(polylineOptions2);
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
        Button menubutton = new Button(this);
        menubutton.setBackgroundResource(R.drawable.menu);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(heightPx * 100 / 60, heightPx * 9 / 10);
        layoutParams2.setMarginEnd(convertDpToPx(7));
        menubutton.setLayoutParams(layoutParams2);
        menubutton.setText("MENU");
        menubutton.setShadowLayer(5, 1, 1, Color.parseColor("#a89c6a"));
        menubutton.setTextColor(Color.parseColor("#d4bd61"));
        menubutton.setTextSize(15);
        menubutton.setOnClickListener(v -> {
            PopupWindow popupWindow = new PopupWindow(context);
            View view = LayoutInflater.from(context).inflate(R.layout.pupup_menu, null, false);
            popupWindow.setContentView(view);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);
            if (popupWindow.isShowing()) popupWindow.dismiss();
            else popupWindow.showAsDropDown(menubutton);

            Button menuItem0 = view.findViewById(R.id.menu_item0);  // Смена типа карты
            menuItem0.setBackgroundResource(R.drawable.menubutton);
            menuItem0.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem0.setOnClickListener(s -> {
                int mapType = mMap.getMapType();
                // Переключите режим карты на следующий доступный режим
                switch (mapType) {
                    case GoogleMap.MAP_TYPE_NORMAL:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    case GoogleMap.MAP_TYPE_HYBRID:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                }
                popupWindow.dismiss();
            });

            Button menuItem1 = view.findViewById(R.id.menu_item1);  // Загрузка карты полигона
            menuItem1.setBackgroundResource(R.drawable.menubutton);
            menuItem1.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem1.setOnClickListener(s -> {
                new KmlLayerLoaderTask(this, mMap).execute();
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
                // Установка диапазона значений
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    seekBar.setMin(10);
                }
                seekBar.setMax(120);
                seekBar.setProgress(60);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    int value = seekBar.getProgress();
                    DataSender.MARKER_SIZE = value + 1;
                    DataSender.apDateMarkers();
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
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.START, 0, 0);

            Button menuItem5 = view.findViewById(R.id.menu_item5);  // Закрыть
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                if (IsNeedToSave && locationHistory.size() > 0)
                    PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
                finish();
                MyServiceUtils.destroyAlarmManager();
                DataSender.getInstance().myonDestroy();
                onDestroy();
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.START, 0, 0);

        });
        return menubutton;
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
            if (isChecked && permission) {
                System.out.println("Включение маркеров");
                DataSender.isMarkersON = true;
                DataSender.apDateMarkers();
                //обновление списка координат сразу после запуска не дожидаясь алармменеджера
                new Thread(() -> sender.sendGPS());
            } else {
                System.out.println("Выключение маркеров");
                DataSender.isMarkersON = false;
                DataSender.offMarkers();
            }
        });
        return markerswitch;
    }

    private void createTollbar() {
        // Добавляем тулбар
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
        textView1 = new TextView(this);
        textView1.setTextColor(Color.parseColor("#FFe6ce6b"));
        textView1.setTextSize(12F);
        Toolbar.LayoutParams layoutParams1 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_HORIZONTAL;
        toolbar.addView(textView1, layoutParams1);

        //Компас вью
        CompassView compassView = findViewById(R.id.compass_view);
        compassView.setVisibility(View.INVISIBLE);
        compassView.compasOFF();
        textView1.setOnClickListener(v -> {
            if (compassView.getVisibility() == View.VISIBLE) {
                compassView.setVisibility(View.INVISIBLE);
                compassView.compasOFF();
                textView1.setTextSize(12F);
                textView1.setText("COMPAS");
            } else {
                compassView.setVisibility(View.VISIBLE);
                compassView.compassON();
            }
        });
        textView1.setTextSize(12F);
        textView1.setText("COMPAS");
    }

    @SuppressLint("SetTextI18n")
    private void buildRoute(LatLng destination) {
        // Удаление предыдущей полилинии маршрута
        if (routePolyline != null) {
            routePolyline.remove();
        }
        // Получение списка координат для построения прямой линии
        List<LatLng> polylineCoordinates = Arrays.asList(new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude()), destination);
        // Получение полилинии для отображения маршрута на карте

        Bitmap bitmapcustomcap = BitmapFactory.decodeResource(context.getResources(), R.drawable.star);
        BitmapDescriptor bitmapcustomcapicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcustomcap, 60, 60, false));
        CustomCap customCap = new CustomCap(bitmapcustomcapicon);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(polylineCoordinates)
                .endCap(customCap)
                .geodesic(true)
                .color(Color.YELLOW)
                .width(6);
        double distance = SphericalUtil.computeDistanceBetween(new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude()), destination);
        distanceTextView.setVisibility(View.VISIBLE);
        if ((int) distance > 1000)
            distanceTextView.setText((Math.round(distance / 10) / 100.0) + " км.");
        else
            distanceTextView.setText((int) distance + " м.");
        // Добавление полилинии на карту
        routePolyline = mMap.addPolyline(polylineOptions);

        // Слежение за изменением местоположения пользователя
        mMap.setOnMyLocationChangeListener(location -> {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Обновление полилинии маршрута с учетом нового местоположения
            List<LatLng> updatedPolylineCoordinates = Arrays.asList(currentLatLng, destination);
            routePolyline.setPoints(updatedPolylineCoordinates);
            double distance1 = SphericalUtil.computeDistanceBetween(currentLatLng, destination);
            if ((int) distance1 > 1000)
                distanceTextView.setText((Math.round(distance1 / 10) / 100.0) + " км.");
            else
                distanceTextView.setText((int) distance1 + " м.");

        });
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(latLng -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("Выберите действие")
                    .setItems(new CharSequence[]{"Показать расстояние до точки", "Построить маршрут", "Очистить маршрут"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                // Показать расстояние до точки
                                float[] distance = new float[1];
                                Location.distanceBetween(latitude, longitude, latLng.latitude, latLng.longitude, distance);
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
                        }
                    });
            builder.create().show();
        });
        // Смещаем карту ниже тулбара
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        mMap.setPadding(0, actionBarHeight, 0, 0);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Камера на Красноярск
        //  LatLng location = new LatLng(56.0901, 93.2329);   //координаты красноярска
        LatLng location = new LatLng(55.6739849, 85.1152591);  // координаты полигона
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

        // Настраиваем карту

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setMyLocationEnabled(true);
            mMap.setOnMapClickListener(latLng -> {
                double[] distance = new double[1];
                distance[0] = 25;
                if (polyline != null) {
                    boolean closestPoint = PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, distance[0]);
                    if (closestPoint) {
                        Toast.makeText(getApplicationContext(), "Дистанция: " + (int) SphericalUtil.computeLength(polyline.getPoints()) + " метров", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

        // Загружаем метки полигона
        ImportantMarkers.importantMarkersCreate();
        // Показываем только текст маркера, без перемещения к нему камеры
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        });

        try {
            if (kmlSavedFile != null) {
                System.out.println("Проверили что kmlSavedFile != null");
                assert kmlSavedFile != null;
                if (kmlSavedFile.exists()) {
                    System.out.println("Проверили что kmlSavedFile.exists() и добавляем слой из кеша");
                    InputStream in = new FileInputStream(kmlSavedFile);
                    KmlLayer kmlLayer = new KmlLayer(mMap, in, context);
                    in.close();
                    kmlLayer.addLayerToMap();
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}