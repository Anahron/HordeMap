package ru.newlevel.hordemap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.data.kml.KmlLayer;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;



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
            MapsActivity.makeToast("Подождите, загрузка выполняется");
            if (data != null) {
                Uri kmzFileUri = data.getData();
                InputStream inputStream = null;
                try {
                    inputStream = MapsActivity.getContext().getContentResolver().openInputStream(kmzFileUri);
                    KmlLayer kmlLayer = new KmlLayer(mMap, inputStream, mContext);
                    savedKmlLayer = kmlLayer;
                    kmlLayer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    MapsActivity.makeToast("Загрузка неудачна");
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
}


