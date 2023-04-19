package ru.newlevel.hordemap;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.newlevel.hordemap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_INTERNET_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private ActivityMapsBinding binding;
    private LocationManager locationManager;
    public static GoogleMap mMap;
    public static Long id;
    public static String name;
    private static double latitude;
    private static double longitude;
    private Context context;
    private Timer timer;
    private Boolean permission = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Запрос прав если отсутствуют
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }
        // Получаем фрагмент карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Добавляем тулбар
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu();

        // Добавляем кнопки на тулбар
        // Кнопка смены провайдера
        ToggleButton toggleButton = new ToggleButton(this);
        toggleButton.setText("ON/OFF");
        toolbar.addView(toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Обработка изменения состояния переключателя
                if (isChecked && permission) {
                    DataSender.isMarkersON = true;
                    sendingRequests();
                } else {
                    DataSender.isMarkersON = false;
                    DataSender.offMarkers();
                }
            }
        });
        Button changeProviderButton = new Button(this);
        changeProviderButton.setText("ON/OFF");
        toolbar.addView(changeProviderButton);
        changeProviderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                    File kmlfile = unpackKmz(kmzfile);
                    //Вызываем метод ребилда, меняя в файле doc пути с относительных на getFilesDir
                    File newfile = RebuildKML.rebuildKML(kmlfile);
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
        // Получаем менеджер местоположений, название провайдера
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                System.out.println(location.getProvider());
                Log.d("TAG", "latitude: " + latitude + ", longitude: " + longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                System.out.println("Провайдер изменил статус");
            }

            @Override
            public void onProviderEnabled(String provider) {
                System.out.println("Провайдер включен");
            }

            @Override
            public void onProviderDisabled(String provider) {
                System.out.println("Провайдер выключен");
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, locationListener);
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        mMap.setPadding(0, actionBarHeight, 0, 0);
        // Add a marker and move the camera
        LatLng location = new LatLng(56.0901, 93.2329);
        //  mMap.addMarker(new MarkerOptions().position(location).title("test marker").icon(BitmapDescriptorFactory.fromResource(R.drawable.orc2)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 3));
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        try {

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true); // Включить кнопку перехода к местоположению пользователя
            mMap.getUiSettings().setCompassEnabled(true); // Включить отображение компаса
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            System.out.println("включилась");
        } catch (SecurityException e) {
            Log.e("MapsActivity", "Error: " + e.getMessage());
            System.out.println("не включилась");
        }
        dialodForNumber();
    }

    public void sendingRequests() {
        if (timer != null)
            try {
                timer.cancel();
                System.out.println("Таймер остановлен");
            } catch (Exception e) {
                System.out.println("Таймер не был запущен");
            }
        timer = new Timer();
        DataSender sender = new DataSender(id, name);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Создаем объект отправителя данных
                System.out.println(latitude);
                System.out.println(longitude);
                sender.updateLocation(latitude, longitude);
                new Thread(sender).start();
            }
        }, 0, 30000); // 30000 - 30 сек
        System.out.println("Таймер запущен");
    }

    private void dialodForNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите номер телефона формата '8912312341212'");

        // добавляем компонент EditText в AlertDialog
        final EditText input = new EditText(this);
        builder.setView(input);

        // добавляем кнопки "Отмена" и "Ок" в AlertDialog
        builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DataSender.context = context;
                String phoneNumber = input.getText().toString().trim();
                System.out.println(phoneNumber);
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        DataSender.getLoginAccess(phoneNumber);
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (DataSender.answer.matches("^[a-zA-Zа-яА-Я]+$")) {
                    name = DataSender.answer;
                    id = Long.parseLong(phoneNumber);
                    Toast.makeText(context, "Авторизация пройдена, привет " + name, Toast.LENGTH_LONG).show();
                    permission = true;
                    sendingRequests();
                } else {
                    Toast.makeText(context, "Авторизация НЕ пройдена, обмен гео данными запрещен", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Авторизация НЕ пройдена, обмен гео данными запрещен", Toast.LENGTH_LONG).show();// закрытие диалога при нажатии на кнопку "Отмена"
                dialog.cancel();
            }
        });
        // запрет на закрытие диалога при нажатии на кнопку "Назад" на устройстве
        builder.setCancelable(false);
        // создаем и отображаем AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private File unpackKmz(File kmzFile) {
        System.out.println("вызван метод анпак");
        System.out.println(kmzFile.exists());
        System.out.println(kmzFile.length());
        File kmlfile = new File(kmzFile.getParent(), "doc.kml");
        try {
            FileInputStream inputStream = new FileInputStream(kmzFile);
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry zipEntry;
            System.out.println(zipInputStream.available());
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                System.out.println(fileName + " - имя файла");
                System.out.println(zipEntry.isDirectory() + " это директория?");

                // directories
                if (zipEntry.isDirectory()) {
                    //       File outputDir = new File(getFilesDir(), "files");
                    //       System.out.println(outputDir.toString() + "директория на запись на начало входа");
                    unpackZipEntry(zipInputStream, zipEntry, getFilesDir());
                    System.out.println(getFilesDir() + " это файловая директория которая подается во второй анпак");
                } else {
                    // Extract file
                    File outputFile = new File(getFilesDir(), fileName);
                    System.out.println(outputFile + " это имя файла на запись");
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int count;

                    while ((count = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    outputStream.close();
                    zipInputStream.closeEntry();
                    System.out.println(outputFile.exists() + " mz файл существует? " + outputFile.getAbsolutePath() + " путь к нему и " + outputFile.length() + " его размер");
                    if (outputFile.getName().endsWith("kmz")) kmlfile = outputFile;
                }
            }
            zipInputStream.close();
            System.out.println("анзип отработал");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kmlfile;
    }

    private void unpackZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, File outputDir) throws IOException {
        // Check if entry is a directory
        System.out.println(outputDir.toString() + " - output dir");
        System.out.println("зашли во второй анпак");
        if (zipEntry.isDirectory()) {
            // Create directory on filesystem
            File newDir = new File(outputDir, zipEntry.getName());
            newDir.mkdir();
            ZipEntry newEntry;
            while ((newEntry = zipInputStream.getNextEntry()) != null) {
                unpackZipEntry(zipInputStream, newEntry, outputDir);
            }
        } else {
            // Create output file and stream
            File outputFile = new File(outputDir, zipEntry.getName());
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            // Copy bytes from zip entry stream to output file stream
            byte[] buffer = new byte[1024];
            int count;
            while ((count = zipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            System.out.println(outputDir + "это директория для записи и " + outputFile + " это файл");
            System.out.println(outputFile.toString() + " " + outputFile.length());
            // Close output stream
            outputStream.close();
        }
    }

}
