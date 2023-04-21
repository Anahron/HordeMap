package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;
import static ru.newlevel.hordemap.DataSender.getInstance;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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


        // Добавляем кнопки на тулбар
        // Кнопка выключения маркеров
        ToggleButton toggleButton = new ToggleButton(this);
        toggleButton.setText("ON/OFF");
        toolbar.addView(toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Обработка изменения состояния переключателя
                if (isChecked && permission) {
                    DataSender.isMarkersON = true;
                    DataSender sender = DataSender.getInstance();
                    //обновление списка координат сразу после запуска не дожидаясь алармменеджера
                    new Thread(sender).start();
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
        button.setText("Карта");
        toolbar.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //Копируем файл из папки raw в папку files (getFilesDir() задаем новое имя "krsk.kmz")
                    int resourceId = R.raw.krsk1;
                    InputStream inputStream = getResources().openRawResource(resourceId);
                    File kmzfile = new File(getFilesDir(), "krks.kmz");
                    try {
                        FileOutputStream outputStream = new FileOutputStream(kmzfile);
                        byte[] buffer = new byte[5024];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }
                        outputStream.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Анпаким krsk.kmz получая doc.kml и папку files с jpg возвращая файл kml
                    //Вызываем метод ребилда, меняя в файле doc пути с относительных на getFilesDir
                    File newfile = RebuildKML.rebuildKML(Objects.requireNonNull(unpackKmz(kmzfile)));
                    //Создали поток для ребилженого файла и наложили на экран.
                    InputStream in = new FileInputStream(newfile);
                    KmlLayer kmlLayer = new KmlLayer(mMap, in, getApplicationContext());
                    kmlLayer.addLayerToMap();
                    System.out.println("Кнопка отработала");
                    in.close();
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();

                }
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

    private File unpackKmz(File kmzFile) {
        try {
            File outputDir = getFilesDir();
            FileInputStream inputStream = new FileInputStream(kmzFile);
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry zipEntry;
            File kmlfile = new File(outputDir, "doc.kml");
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    File newDir = new File(outputDir, fileName);
                    newDir.mkdir();
                    ZipEntry newEntry;
                    while ((newEntry = zipInputStream.getNextEntry()) != null) {
                        String newFileName = newEntry.getName();
                        File newFile = new File(newDir, newFileName);
                        FileOutputStream outputStream = new FileOutputStream(newFile);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.close();
                        zipInputStream.closeEntry();
                        if (newFileName.endsWith("kmz")) {
                            kmlfile = newFile;
                        }
                    }
                } else {
                    File outputFile = new File(outputDir, fileName);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    outputStream.close();
                    zipInputStream.closeEntry();
                    if (outputFile.getName().endsWith("kmz")) {
                        kmlfile = outputFile;
                    }
                }
            }
            zipInputStream.close();
            return kmlfile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
