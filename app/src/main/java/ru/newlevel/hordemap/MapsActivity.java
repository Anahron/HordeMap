package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.markerSize;
import static ru.newlevel.hordemap.DataSender.sender;
import static ru.newlevel.hordemap.MyLocationListener.locationHistory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    public static GoogleMap mMap;
    public static Long id = 0L;
    public static String name;
    public static Boolean permission = false;
    private Polyline polyline;
    private Sensor rotationVectorSensor;
    private float currentDegree = 0f;
    private TextView textView1;
    private boolean IsNeedToSave = true;
    public static List<String> savedLogsOfGPSpath = new ArrayList<>();
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticSensor;

    private float[] accelerometerReading = new float[3];
    private float[] magneticReading = new float[3];

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1003;

    protected void onDestroy() {
        System.out.println("Вызван в мэйне");
        if (IsNeedToSave == true && locationHistory.size() > 0)
            PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
        super.onDestroy();
    }

    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride", "ResourceType", "SetTextI18n", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ru.newlevel.hordemap.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) // запрет поворота экрана
        context = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission_group.SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.SENSORS}, MY_PERMISSIONS_REQUEST_SENSOR);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Запуск слушателя месторасположений
        MyLocationListener.getInstance().startLocationListener();
        // Инициализация SensorManager и Rotation Vector Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Запрос логина
        if (name == null || name.equals("name"))
            LoginRequest.logIn(context);
        // Создаем меню
        createButtons();
    }

    private void createButtons() {
        // Добавляем тулбар
        Toolbar toolbar = findViewById(R.id.toolbar);
        Drawable logo = getResources().getDrawable(R.drawable.toolbar);
        TypedValue tv = new TypedValue();
        toolbar.setPadding(convertDpToPx(0), 0, convertDpToPx(0), convertDpToPx(5));
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int heightPx = getResources().getDimensionPixelSize(tv.resourceId);
        toolbar.setBackground(logo);

        // Добавляем первое меню
        Button menubutton = new Button(this);
        Drawable myDrawable = getResources().getDrawable(R.drawable.menu);
        menubutton.setBackgroundResource(R.drawable.menu);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(heightPx * 100/60, heightPx * 9 / 10);
        layoutParams2.setMarginEnd(convertDpToPx(7));
        menubutton.setLayoutParams(layoutParams2);
        menubutton.setText("MENU");
        menubutton.setShadowLayer(5,1,1,Color.parseColor("#a89c6a"));
        menubutton.setTextColor(Color.parseColor("#d4bd61"));
        menubutton.setTextSize(15);
        toolbar.addView(menubutton);
        menubutton.setOnClickListener(v -> {
            PopupWindow popupWindow = new PopupWindow(context);
            View view = LayoutInflater.from(context).inflate(R.xml.pupup_menu, null, false);
            popupWindow.setContentView(view);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(277));
            popupWindow.setFocusable(true);
            popupWindow.showAsDropDown(menubutton);

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
                    DataSender.markerSize = value + 1;
                    DataSender.apDateMarkers();
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                popupWindow.dismiss();
            });

            Button menuItem3= view.findViewById(R.id.menu_item3);   // Показать дистанцию
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
                LoginRequest.logIn(context);
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.LEFT, 0, 0);

            Button menuItem5 = view.findViewById(R.id.menu_item5);  // Закрыть
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                if (IsNeedToSave == true && locationHistory.size() > 0)
                    PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
                DataSender.getInstance().myonDestroy();
                onDestroy();
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.LEFT, 0, 0);

        });

        // Добавляем второе меню
        Button menubutton2 = new Button(this);
        Drawable myDrawable2 = getResources().getDrawable(R.drawable.menu);
        menubutton2.setBackgroundResource(R.drawable.menu);
        menubutton2.setLayoutParams(new ViewGroup.LayoutParams(heightPx * 100/60, heightPx * 9 / 10));
        menubutton2.setText("PATHS");
        menubutton2.setShadowLayer(5,1,1,Color.parseColor("#a89c6a"));
        menubutton2.setTextColor(Color.parseColor("#d4bd61"));
        menubutton2.setTextSize(15);
        toolbar.addView(menubutton2);
        menubutton2.setOnClickListener(v -> {
            PopupWindow popupWindow = new PopupWindow(context);
            View view = LayoutInflater.from(context).inflate(R.xml.pupup_menu2, null, false);
            popupWindow.setContentView(view);
            popupWindow.setWidth(convertDpToPx(256));
            popupWindow.setHeight(convertDpToPx(323));
            popupWindow.setFocusable(true);
            popupWindow.showAsDropDown(menubutton);

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
                    DataSender.apDateMarkers();
                    PolylineOptions polylineOptions = new PolylineOptions()
                         //   .addAll(PolyUtil.simplify(locationHistory, 1))
                            .addAll(locationHistory)
                            .jointType(JointType.ROUND)
                            .startCap(new SquareCap())
                            .endCap(new RoundCap())
                            .geodesic(true)
                            .color(Color.RED) // Задаем цвет линии
                            .width(10); // Задаем ширину линии
                    // Добавляем Polyline на карту
                    polyline = mMap.addPolyline(polylineOptions);
                    polyline.setVisible(true);
                    popupWindow.dismiss();
                }
            });

            Button menuItem3hidePath = view.findViewById(R.id.menu2_item3);  // Скрыть (на карте)
            menuItem3hidePath.setBackgroundResource(R.drawable.menubutton);
            menuItem3hidePath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem3hidePath.setOnClickListener(s -> {
                mMap.clear();
                DataSender.apDateMarkers();
                popupWindow.dismiss();
            });

            Button menuItem4savepath = view.findViewById(R.id.menu2_item4); //Сохранение пути в файловую систему
            menuItem4savepath.setBackgroundResource(R.drawable.menubutton);
            menuItem4savepath.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem4savepath.setOnClickListener(s -> {
                if (!locationHistory.isEmpty()) {
                    PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
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
                    builder.setItems(hashtable.keySet().toArray(new String[0]), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedItem = hashtable.keySet().toArray(new String[0])[which];
                            Toast.makeText(context, "Выбран элемент: " + selectedItem, Toast.LENGTH_SHORT).show();
                            System.out.println(selectedItem);
                            List<LatLng> value;
                            for (Map.Entry<String, List<LatLng>> entry : hashtable.entrySet()) {
                                if (entry.getKey().equals(selectedItem)) {
                                    value = entry.getValue();
                                    System.out.println(entry.getKey());
                                    System.out.println(selectedItem);
                                }
                            }
                            List<LatLng> polylines = hashtable.get(selectedItem);
                            System.out.println("длина пути в ячейках " + polylines.size());
                            if (polylines.isEmpty()) {
                                Toast.makeText(context, "Записанного пути нет :(", Toast.LENGTH_LONG).show();
                            } else {
                                PolylineOptions polylineOptions2 = new PolylineOptions()   // тест 1
                                  //      .addAll(polylines)
                                 //       .addAll(PolyUtil.simplify(polylines, 1))
                                        .addAll(polylines)
                                        .jointType(JointType.ROUND)
                                        .startCap(new RoundCap())
                                        .endCap(new SquareCap())
                                        .geodesic(true)
                                        .color(Color.BLACK)
                                        .width(10);
                                polyline = mMap.addPolyline(polylineOptions2);
//                                PolylineOptions polylineOptions3 = new PolylineOptions()   // тест 1
//                                        .addAll(PolyUtil.simplify(PolyUtil.simplify(polylines, 1), 1))
//                                        .jointType(JointType.ROUND)
//                                        .startCap(new RoundCap())
//                                        .endCap(new SquareCap())
//                                        .geodesic(true)
//                                        .color(Color.GREEN)
//                                        .width(10);
//                                polyline = mMap.addPolyline(polylineOptions3);
                            }
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

        // Добавляем свитч маркеров
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch markerswitch = new Switch(this);
        markerswitch.setBackgroundResource(R.xml.custom_switch);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((heightPx * 6) / 10, (heightPx * 6) / 10);
        layoutParams.setMarginStart(convertDpToPx(28));
        layoutParams.setMarginEnd(convertDpToPx(20));
        markerswitch.setLayoutParams(layoutParams);
        markerswitch.setChecked(true);
        markerswitch.setThumbDrawable(null);
        markerswitch.setText(null);
        markerswitch.setTextOff(null);
        markerswitch.setTextOn(null);
        toolbar.addView(markerswitch);
        markerswitch.setOnCheckedChangeListener((buttonView, isChecked) ->

        {
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

        //Добавляем тект направления
        textView1 = new TextView(this);
        textView1.setText(String.valueOf(currentDegree));
        textView1.setTextColor(Color.parseColor("#FFe6ce6b"));
        textView1.setTextSize(25F);
        Toolbar.LayoutParams layoutParams1 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_HORIZONTAL;
        toolbar.addView(textView1, layoutParams1);

    }

    //
    @Override
    protected void onResume() {
        super.onResume();
        // Регистрация слушателя для Rotation Vector Sensor
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Отмена регистрации слушателя для Rotation Vector Sensor
     //   sensorManager.unregisterListener(this);
    }


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Сдвиг карты ниже тулбара
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        mMap.setPadding(0, actionBarHeight, 0, 0);

        // Камера на Красноярск
        LatLng location = new LatLng(56.0901, 93.2329);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 8));
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        try {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
                @Override
                public void onMyLocationClick(@NonNull Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Toast.makeText(MapsActivity.this, "Lat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                }
            });
            mMap.getUiSettings().setMapToolbarEnabled(false);

            // ТЕСТ со слушателем координат гугл
            List<LatLng> mMapCoordinates = new ArrayList<>();
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.hordecircle);
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, markerSize, markerSize, false));
            final Marker[] marker = new Marker[1];
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            System.out.println("включилась");
        } catch (SecurityException e) {
            Log.e("MapsActivity", "Error: " + e.getMessage());
            System.out.println("не включилась");
        }

        // Показываем только текст маркера, без перемещения к нему камеры
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticReading, 0, magneticReading.length);
        }

        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magneticReading);

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float azimuthInRadians = orientationAngles[0];
            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
            textView1.setText(azimuthInDegrees + " deg.");
        }
    }

//        // Получение данных от Rotation Vector Sensor
//        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { //Sensor.TYPE_ROTATION_VECTOR - все 3
//            float[] rotationMatrix = new float[9];
//            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
//            float[] orientation = new float[3];
//            SensorManager.getOrientation(rotationMatrix, orientation);
//            float azimuthInRadians = orientation[0];
//            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
//            azimuthInDegrees = (azimuthInDegrees + 360) % 360;
//            currentDegree = -azimuthInDegrees;
//            System.out.println(+azimuthInDegrees);
//            updateDirection(azimuthInDegrees);
//        }
//    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
