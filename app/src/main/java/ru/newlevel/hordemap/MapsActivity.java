package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.getInstance;
import static ru.newlevel.hordemap.DataSender.sender;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_INTERNET_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private ActivityMapsBinding binding;
    public static GoogleMap mMap;
    public static Long id;
    public static String name;
    public static Boolean permission = false;
    private SharedPreferences prefs;

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        LoginRequest loginRequest = new LoginRequest();
        // Запрос прав если отсутствуют
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }
        MyLocationListener.startLocationListener();
        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        // Добавляем тулбар
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        //добавляем кнопки на туобар
        Switch toggleButton = new Switch(this);
        toggleButton.setChecked(true);
        toggleButton.setText("MARKER");
        toggleButton.setTextScaleX(0.8f);
        toolbar.addView(toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
        // Кнопка сброса логина
        Button resetLogin = new Button(this);
        resetLogin.setText("RESET");
        toolbar.addView(resetLogin);
        resetLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginRequest.logOut();
                loginRequest.logIn(context);
            }
        });
        // Кнопка загрузки оверлея
        Button button = new Button(this);
        button.setText("MAP");
        toolbar.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File kmlfile = null;
                try {
                    kmlfile = KMZhandler.DownloadKMZ(context, getFilesDir());
                    //Создали поток для ребилженого файла и наложили на экран.
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
            }
        });

        Button markersize = new Button(this);
        resetLogin.setWidth(10);
        resetLogin.setHeight(10);
        markersize.setWidth(10);
        markersize.setHeight(10);
        button.setWidth(10);
        button.setHeight(10);
        markersize.setText("SIZE");
        toolbar.addView(markersize);
        markersize.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Изменение размера маркеров");
                SeekBar seekBar = new SeekBar(context);
                builder.setView(seekBar);
                builder.setTitle("Изменение размера маркеров");
                // Установка диапазона значений
                seekBar.setMax(10);
                seekBar.setProgress(5);
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
                        System.out.println("РАЗМЕР В ЦИФРАХ" + value);
                        DataSender.markerSize = (value * 10) + 10;
                        System.out.println("РАЗМЕР В ПИКСЕЛЯ БУДЕТ: + " + value * 10);
                        DataSender.apDateMarkers();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();


            }
        });





























        loginRequest.logIn(context);
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
