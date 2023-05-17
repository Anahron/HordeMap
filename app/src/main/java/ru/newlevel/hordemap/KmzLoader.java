package ru.newlevel.hordemap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.data.kml.KmlLayer;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class KmzLoader {
    private static final int REQUEST_CODE_PICK_KMZ_FILE = 100;
    private final Context mContext;
    private final GoogleMap mMap;
    public static KmlLayer savedKmlLayer = null;

    public KmzLoader(Context mContext, GoogleMap mMap) {
        this.mContext = mContext;
        this.mMap = mMap;
    }

    void openFilePicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.google-earth.kmz");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(Intent.createChooser(intent, "Выберите KMZ файл"), REQUEST_CODE_PICK_KMZ_FILE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_KMZ_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri kmzFileUri = data.getData();
                InputStream inputStream = null;
                try {
                    inputStream = MapsActivity.getContext().getContentResolver().openInputStream(kmzFileUri);
                    KmlLayer kmlLayer = new KmlLayer(mMap, inputStream, mContext);
                    savedKmlLayer = kmlLayer;
                    kmlLayer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static File unpackKmz(Uri kmzFile, File filedir) {
        try {
            System.out.println(kmzFile + "это путь к kmzFile");
            InputStream inputStream = MapsActivity.getContext().getContentResolver().openInputStream(kmzFile);
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

            ZipEntry zipEntry;
            File kmlfile = new File(filedir, "doc.kml");
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                System.out.println(zipEntry.getName() + " zipEntry.getName()");
                if (fileName.startsWith("files/")) {
                    fileName = fileName.substring("files/".length());
                    System.out.println(fileName + "  fileName = fileName.substring(files/.length()");
                    ZipEntry newEntry;
                    while ((newEntry = zipInputStream.getNextEntry()) != null) {
                        System.out.println(newEntry.getName() + " имя entry");
                        String newFileName = newEntry.getName();
                        File newFile = new File(filedir, newFileName);
                        File newDir = new File(filedir, fileName);
                        newDir.mkdir();
                        System.out.println(newDir + " newDir");
                        FileOutputStream outputStream = new FileOutputStream(newFile);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.close();
                        zipInputStream.closeEntry();
                        System.out.println("Файл распакован " + newFile);
                        if (newFileName.endsWith("kmz")) {
                            kmlfile = newFile;
                        }
                    }
                } else {
                    File outputFile = new File(filedir.getAbsolutePath(), fileName);
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                    System.out.println(outputFile + " - outputFile = new File(filedir, fileName)");
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    System.out.println("Файл распакован " + outputFile);
                    outputStream.close();
                    zipInputStream.closeEntry();
                    if (outputFile.getName().endsWith("kmz")) {
                        kmlfile = outputFile;
                    }
                }
            }
            zipInputStream.close();
            return rebuildKML(kmlfile);
        } catch (IOException e) {
            System.out.println("Ошибка в методе unpackKmz");
            e.printStackTrace();
            return null;
        }
    }

    private static File rebuildKML(File inputFile) {
        File outputFile = null;
        System.out.println("зашли в ребилд кмз");
        System.out.println("проверяем на наличие инпут файла " + inputFile.exists());
        try {
            //На основе прошлого файла получаем родительский каталог и объявляем новый файл
            outputFile = new File(inputFile.getParent(), "docnew.kml");
            //удаляем если существует(например старая версия)
            if (outputFile.delete()) {
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("Failed to delete file.");
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
            //читаем построчно и меняем пути
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("files/")) {
                    line = line.replace("files/", inputFile.getParent() + "/files/");
                }
                bufferedWriter.write(line + "\n");
            }
            bufferedReader.close();
            bufferedWriter.close();
            System.out.println(outputFile.exists() + "файл существует?");
            System.out.println(outputFile.getAbsolutePath() + " путь к новому файлу");
            System.out.println(outputFile.length() + " размер нового файла");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }
}


