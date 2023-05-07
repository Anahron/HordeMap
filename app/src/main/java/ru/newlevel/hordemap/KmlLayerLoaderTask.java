package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.data.kml.KmlLayer;
import org.xmlpull.v1.XmlPullParserException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class KmlLayerLoaderTask extends AsyncTask<Void, Void, KmlLayer> {

    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private final GoogleMap mMap;
    public static File kmlSavedFile;


    @SuppressWarnings("deprecation")
    public KmlLayerLoaderTask(Context context, GoogleMap map) {
        mContext = context;
        mMap = map;
    }

    @Override
    protected KmlLayer doInBackground(Void... params) {
        try {
            File kmlfile = KMZhandler.DownloadKMZ(mContext, mContext.getFilesDir());
            kmlSavedFile = kmlfile;
            InputStream in = new FileInputStream(kmlfile);
            KmlLayer kmlLayer = new KmlLayer(mMap, in, mContext);
            in.close();
            return kmlLayer;
        } catch (IOException | XmlPullParserException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(KmlLayer kmlLayer) {
        if (kmlLayer != null) {
            kmlLayer.addLayerToMap();
        }
    }
}

