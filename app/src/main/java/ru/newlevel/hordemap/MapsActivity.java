package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.sender;
import static ru.newlevel.hordemap.MyLocationListener.coordinates;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    public static GoogleMap mMap;
    public static Long id = 0L;
    public static String name;
    public static Boolean permission = false;
    private Polyline polyline;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    // private ImageView compassImage;
    private float currentDegree = 0f;
    private TextView textView1;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(context, DataSender.class); // замените MyService на имя своего сервиса
        stopService(intent);
    }

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride", "ResourceType", "SetTextI18n", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ru.newlevel.hordemap.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Инициализация Rotation Vector Sensor
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) // запрет поворота экрана
        MyLocationListener.startLocationListener(this);
        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        // Добавляем тулбар
        Toolbar toolbar = findViewById(R.id.toolbar);
        Drawable logo = getResources().getDrawable(R.drawable.toolbar);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int heightPx = getResources().getDimensionPixelSize(tv.resourceId);

        //   int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        // int widthPx = (int) (heightPx * aspectRatio); // aspectRatio - соотношение сторон вашего логотипа
        toolbar.setBackground(logo);
        LoginRequest loginRequest = new LoginRequest();
        if (name == null || name.equals("name"))
            loginRequest.logIn(context);
        //добавляем кнопки на тулбар
        Button menubutton = new Button(this);

        ImageView myImage = new ImageView(this);
        myImage.setLayoutParams(new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(tv.resourceId), getResources().getDimensionPixelSize(tv.resourceId)));
        myImage.setImageResource(R.drawable.menu); // установка изображения по идентификатору ресурса
        myImage.setScaleType(ImageView.ScaleType.FIT_CENTER); // установка типа масштабирования
        Drawable myDrawable = getResources().getDrawable(R.drawable.menu);
        myDrawable.setBounds(0, 20, 100, 100); // задаем размер 100x100 пикселей
        //  button.setCompoundDrawables(null, myDrawable, null, null);
        menubutton.setBackgroundResource(R.drawable.menu);

        // установка размеров кнопки
        menubutton.setLayoutParams(new ViewGroup.LayoutParams(240, 100)); // установка ширины и высоты кнопки
        toolbar.addView(menubutton);
        menubutton.setOnClickListener(v -> {
            //  View view = LayoutInflater.from(context).inflate(R.xml.popup_menu, null);
            PopupWindow popupWindow = new PopupWindow(context);

            View view = LayoutInflater.from(context).inflate(R.xml.pupup_menu, null, false);
            //    view.setLayoutParams(new LinearLayout.LayoutParams(300, 300));

            popupWindow.setContentView(view);
            //  popupWindow.setWindowLayoutMode(300, 300);
            popupWindow.setWidth(450); //474
            // popupWindow.setHeight(474);//474
            popupWindow.setFocusable(true);
            popupWindow.showAsDropDown(menubutton);
            Button menuItem1 = view.findViewById(R.id.menu_item1);
            menuItem1.setBackgroundResource(R.drawable.menubutton);
            menuItem1.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem1.setOnClickListener(s -> {
                new KmlLayerLoaderTask(this, mMap).execute();
                popupWindow.dismiss();
            });
            Button menuItem2 = view.findViewById(R.id.menu_item2);
            menuItem2.setBackgroundResource(R.drawable.menubutton);
            menuItem2.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem2.setOnClickListener(s -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Размера маркеров");
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
            Button menuItem3 = view.findViewById(R.id.menu_item3);
            menuItem3.setBackgroundResource(R.drawable.menubutton);
            menuItem3.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem3.setOnClickListener(s -> {
                // Создаем объект PolylineOptions
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(PolyUtil.simplify(coordinates, 10))
                        .color(Color.RED) // Задаем цвет линии
                        .width(10); // Задаем ширину линии
                // Добавляем Polyline на карту
                polyline = mMap.addPolyline(polylineOptions);
                popupWindow.dismiss();
            });
            Button menuItem4 = view.findViewById(R.id.menu_item4);
            menuItem4.setBackgroundResource(R.drawable.menubutton);
            menuItem4.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem4.setOnClickListener(s -> {
                if (polyline != null)
                    polyline.remove();
                popupWindow.dismiss();
            });
            Button menuItem5 = view.findViewById(R.id.menu_item5);
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                coordinates.clear();
                popupWindow.dismiss();
            });
            Button menuItem6 = view.findViewById(R.id.menu_item6);
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                Toast.makeText(context, "Пройденная дистанция: " + ((int) Math.round(SphericalUtil.computeLength(PolyUtil.simplify(coordinates, 10)))) + " метров.", Toast.LENGTH_LONG).show();
                popupWindow.dismiss();
            });
            Button menuItem7 = view.findViewById(R.id.menu_item7);
            menuItem5.setBackgroundResource(R.drawable.menubutton);
            menuItem5.setGravity(Gravity.CENTER_HORIZONTAL);
            menuItem5.setOnClickListener(s -> {
                loginRequest.logOut(context);
                loginRequest.logIn(context);
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(menubutton, Gravity.TOP | Gravity.LEFT, 0, 0);

        });
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch markerswitch = new Switch(this);
        markerswitch.setChecked(true);
        markerswitch.setText("  MARKERS");
        markerswitch.setTextColor(Color.WHITE);
        toolbar.addView(markerswitch);
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
        textView1 = new TextView(this);
        textView1.setText(String.valueOf(currentDegree));
        textView1.setTextColor(Color.WHITE);
        Toolbar.LayoutParams layoutParams1 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_HORIZONTAL;
        toolbar.addView(textView1, layoutParams1);

    }

    private void updateDirection(float degrees) {
        // Находим TextView для отображения текущего направления и обновляем его текст
        textView1.setText(String.format("%.0f°", degrees));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Регистрация слушателя для Rotation Vector Sensor
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Отмена регистрации слушателя для Rotation Vector Sensor
        sensorManager.unregisterListener(this);
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
            mMap.getUiSettings().setMapToolbarEnabled(true);
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
        // Получение данных от Rotation Vector Sensor
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
            azimuthInDegrees = (azimuthInDegrees + 360) % 360;

            // Поворот изображения компаса
            RotateAnimation rotateAnimation = new RotateAnimation(
                    currentDegree,
                    -azimuthInDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            rotateAnimation.setDuration(250);
            rotateAnimation.setFillAfter(true);
            //compassImage.startAnimation(rotateAnimation);
            currentDegree = -azimuthInDegrees;
            System.out.println("изменился угол азимута " + azimuthInDegrees);
            updateDirection(-azimuthInDegrees);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
