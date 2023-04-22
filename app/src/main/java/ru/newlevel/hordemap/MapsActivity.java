package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.sender;
import static ru.newlevel.hordemap.MyLocationListener.coordinates;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import java.util.List;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_INTERNET_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private ActivityMapsBinding binding;
    public static GoogleMap mMap;
    public static Long id;
    public static String name;
    public static Boolean permission = false;
    private Polyline polyline;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(context, DataSender.class); // замените MyService на имя своего сервиса
        stopService(intent);
    }
    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // Запрос прав если отсутствуют
        MyLocationListener.startLocationListener();
        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        // Добавляем тулбар
        Toolbar toolbar = findViewById(R.id.toolbar);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.logIn(context);
        //добавляем кнопки на тулбар
        Button menubutton = new Button(this);
        menubutton.setText("MENU");
        toolbar.addView(menubutton);
        menubutton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, menubutton);
                popupMenu.getMenuInflater().inflate(R.xml.popup_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_item1:
                                File kmlfile = null;
                                try {
                                    kmlfile = KMZhandler.DownloadKMZ(context, getFilesDir());
                                    InputStream in = new FileInputStream(kmlfile);
                                    KmlLayer kmlLayer = new KmlLayer(mMap, in, getApplicationContext());
                                    kmlLayer.addLayerToMap();
                                    System.out.println("Кнопка отработала");
                                    in.close();
                                } catch (XmlPullParserException | IOException ex) {
                                    System.out.println("Ошибка загрузки или распаковки карты");
                                    Toast.makeText(context, "Ошибка загрузки, проверьте соединение с интернетом", Toast.LENGTH_LONG).show();
                                    ex.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            case R.id.menu_item2:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setTitle("Изменение размера маркеров");
                                SeekBar seekBar = new SeekBar(context);
                                builder.setView(seekBar);
                                builder.setTitle("Изменение размера маркеров");
                                // Установка диапазона значений
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    seekBar.setMin(10);
                                }
                                seekBar.setMax(120);
                                seekBar.setProgress(60);
                                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            seekBar.setTooltipText(String.valueOf(progress));
                                        }
                                    }
                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {

                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {

                                    }
                                });
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int value = seekBar.getProgress();
                                        DataSender.markerSize = value + 1;
                                        DataSender.apDateMarkers();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                return true;
                            case R.id.menu_item3:
                                float[] results = new float[1];
                                // Создаем объект PolylineOptions
                                PolylineOptions polylineOptions = new PolylineOptions()
                                        .addAll(coordinates)
                                        .color(Color.RED) // Задаем цвет линии
                                        .width(10); // Задаем ширину линии
                                // Добавляем Polyline на карту
                                polyline = mMap.addPolyline(polylineOptions);
                                return true;
                            case R.id.menu_item4:
                                if (polyline!=null)
                                    polyline.remove();
                                return true;
                            case R.id.menu_item5:
                                coordinates.clear();
                                return true;
                            case R.id.menu_item6:
                                List<LatLng> simplifiedCoordinates  = PolyUtil.simplify(coordinates, 10);
                            Toast.makeText(context, "Пройденная дистанция: " +  String.valueOf((int) Math.round(SphericalUtil.computeLength(simplifiedCoordinates))) + " метров.", Toast.LENGTH_LONG).show();
                                return true;
                            case R.id.menu_item7:
                                loginRequest.logOut(context);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
            }
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch markerswitch = new Switch(this);
        markerswitch.setChecked(true);
        markerswitch.setText("  MARKERS");
        markerswitch.setTextColor(Color.WHITE);
        ColorStateList thumbColorStateList = ColorStateList.valueOf(getResources().getColor(R.color.black));
        markerswitch.setThumbTintList(thumbColorStateList);
        markerswitch.getThumbDrawable().setColorFilter(getResources().getColor(R.color.teal_700), PorterDuff.Mode.MULTIPLY);
        markerswitch.getTrackDrawable().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
        toolbar.addView(markerswitch);
        markerswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Обработка изменения состояния переключателя
                if (isChecked && permission) {
                    DataSender.isMarkersON = true;
                    DataSender.apDateMarkers();
                    //обновление списка координат сразу после запуска не дожидаясь алармменеджера
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sender.sendGPS();
                        }
                    });
                } else {
                    DataSender.isMarkersON = false;
                    DataSender.offMarkers();
                }
            }
        });
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


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Сдвиг карты ниже тулбара
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        mMap.setPadding(0, actionBarHeight, 0, 0);
        // Add a marker and move the camera
        LatLng location = new LatLng(56.0901, 93.2329);
        //  mMap.addMarker(new MarkerOptions().position(location).title("test marker").icon(BitmapDescriptorFactory.fromResource(R.drawable.orc2)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 5));
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true); // Включить кнопку перехода к местоположению пользователя
            mMap.getUiSettings().setCompassEnabled(true); // Включить отображение компаса
            //  mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            System.out.println("включилась");
        } catch (SecurityException e) {
            Log.e("MapsActivity", "Error: " + e.getMessage());
            System.out.println("не включилась");
        }
        // Показываем только текст маркера, без перемещения к нему камеры
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @SuppressLint("PotentialBehaviorOverride")
            @Override
            public boolean onMarkerClick(Marker marker) {
                // показываем текст маркера
                System.out.println(marker.isInfoWindowShown());
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                    System.out.println("Скрыть инфо");
                } else {
                    marker.showInfoWindow();
                    System.out.println("Показать инфо");
                }
                return true;
            }
        });

    }

}
