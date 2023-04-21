package ru.newlevel.hordemap;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class KMZhandler {

    public static File DownloadKMZ(Context context, File filedir) throws MalformedURLException, InterruptedException {
        File newDir = new File(filedir, "krsk.kmz");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    System.out.println("Пытаемся скачать файл карты");
                    url = new URL(DataSender.requestInfoFromServer("mapurl"));
                    try (BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
                         FileOutputStream outputStream = new FileOutputStream(newDir);) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        System.out.println("Ошибка скачивания");
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.join();
        if (!newDir.exists()) {
            System.out.println("Не удалось скачать файл, загружаем из RAW");
            int resourceId = R.raw.krsk1;
            // Определяем путь и имя файла для сохранения
            try (InputStream inputStream = context.getResources().openRawResource(resourceId);
                 OutputStream outputStream = new FileOutputStream(newDir)) {
                // Читаем данные из InputStream и записываем их в выходной поток
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return unpackKmz(newDir, filedir);
    }

    public static File unpackKmz(File kmzFile, File filedir) {
        try {
            System.out.println("Распаковываем");
            File outputDir = filedir;
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
            return rebuildKML(kmlfile);
        } catch (IOException e) {
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
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
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

