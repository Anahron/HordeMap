package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.sender;
import static ru.newlevel.hordemap.DataSender.locationHistory;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static GoogleMap mMap;
    public static Long id = 0L;
    public static String name;
    public static Boolean permission = false;
    private Polyline polyline;
    @SuppressLint("StaticFieldLeak")
    public static TextView textView1;
    private boolean IsNeedToSave = true;
    public static List<String> savedLogsOfGPSpath = new ArrayList<>();

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1002;
    private static final int MY_PERMISSIONS_REQUEST_SENSOR = 1003;

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

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride", "ResourceType", "SetTextI18n", "NonConstantResourceId", "BatteryLife"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        ru.newlevel.hordemap.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) // запрет поворота экрана

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission_group.SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.SENSORS}, MY_PERMISSIONS_REQUEST_SENSOR);
        }

        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }

        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Запрос логина
        if (name == null || name.equals("name"))
            LoginRequest.logIn(context);
        // Создаем меню
        createTollbar();
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
            if (popupWindow.isShowing())
                popupWindow.dismiss();
            else
                popupWindow.showAsDropDown(menubutton2);

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

                }
                popupWindow.dismiss();
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
                    builder.setItems(hashtable.keySet().toArray(new String[0]), (dialog, which) -> {
                        String selectedItem = hashtable.keySet().toArray(new String[0])[which];
                        Toast.makeText(context, "Выбран элемент: " + selectedItem, Toast.LENGTH_SHORT).show();
                        List<LatLng> polylines = hashtable.get(selectedItem);
                        if (polylines.isEmpty()) {
                            Toast.makeText(context, "Записанного пути нет :(", Toast.LENGTH_LONG).show();
                        } else {
                            PolylineOptions polylineOptions2 = new PolylineOptions()   // тест 1
                                    .addAll(polylines)
                                    .jointType(JointType.ROUND)
                                    .startCap(new RoundCap())
                                    .endCap(new SquareCap())
                                    .geodesic(true)
                                    .color(Color.BLACK)
                                    .width(10);
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
            popupWindow.setHeight(convertDpToPx(277));
            popupWindow.setFocusable(true);
            if (popupWindow.isShowing())
                popupWindow.dismiss();
            else
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
                LoginRequest.logIn(context);
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.START, 0, 0);

            Button menuItem5 = view.findViewById(R.id.menu_item5);  // Закрыть
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                if (IsNeedToSave && locationHistory.size() > 0)
                    PolylineSaver.savePathList(context, locationHistory, (int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(locationHistory, 22))));
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
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch markerswitch = new Switch(this);
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
        return markerswitch;
    }

    private void createTollbar() {
        // Добавляем тулбар
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPadding(convertDpToPx(0), 0, convertDpToPx(0), convertDpToPx(5));

        // Получаем размер тулбара для выставления размера кнопок
        TypedValue tv = new TypedValue();
        @SuppressLint("UseCompatLoadingForDrawables") Drawable logo = getResources().getDrawable(R.drawable.toolbar);
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

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        mMap.setPadding(0, actionBarHeight, 0, 0);
        // Камера на Красноярск
        LatLng location = new LatLng(56.0901, 93.2329);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 8));
        try {
            mMap.getUiSettings().setMapToolbarEnabled(false);
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
            System.out.println("включилась");
        } catch (
                SecurityException e) {
            Log.e("MapsActivity", "Error: " + e.getMessage());
            System.out.println("не включилась");
        }

        // Показываем только текст маркера, без перемещения к нему камеры
        mMap.setOnMarkerClickListener(marker ->

        {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        });

    }
}
