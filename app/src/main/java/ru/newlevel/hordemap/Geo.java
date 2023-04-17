package ru.newlevel.hordemap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Geo {
    private static ArrayList geoList;

    public static ArrayList download(int id) throws IOException {
        URL url = null; // замените на нужный URL
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("https://jino.cloud/s/M2pBzGF3XiGajPz/gps");

            urlConnection = (HttpURLConnection) url.openConnection();

            try {
                String geoPoint;
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
          //      geoList = bufferedReader.readLine();

                OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            urlConnection.disconnect();
        }
        return geoList;
    }
}
