package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MapsActivity.MARKER_SIZE_CUSTOMS;
import static ru.newlevel.hordemap.MapsActivity.MARKER_SIZE_USERS;
import static ru.newlevel.hordemap.MapsActivity.gMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MarkersHandler {
    private static final int markerSize = 60;
    private static final List<Marker> importantMarkers = new ArrayList<>();
    private static final Bitmap swords1 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.citadelyellow);
    private static final Bitmap swords2 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.checkblue);
    private static final Bitmap swords3 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.checkyellow);
    private static final Bitmap swords4 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.citadelblue);
    private static final Bitmap swords5 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.boss);
    private static final Bitmap swords6 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.bomb);
    private static final Bitmap swords7 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.exit);
    private static final Bitmap swords8 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.swords8);
    private static final Bitmap swords9 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.swords9);
    private static final Bitmap blue_camp = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.blue_camp);
    private static final Bitmap yellow_camp = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.yellow_camp);
    private static final BitmapDescriptor swordsicon1 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords1, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon2 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords2, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon3 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords3, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon4 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords4, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon5 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords5, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon6 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords6, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon7 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords7, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon8 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords8, markerSize, markerSize, false));
    private static final BitmapDescriptor swordsicon9 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(swords9, markerSize, markerSize, false));
    private static final BitmapDescriptor blue_campicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(blue_camp, markerSize, markerSize, false));
    private static final BitmapDescriptor yellow_campicon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(yellow_camp, markerSize, markerSize, false));

    private static List<MyMarker> mySavedUsersMarkersList;
    private static List<MyMarker> mySavedCustomMarkersList;
    private static final ArrayList<Marker> customMarkers = new ArrayList<>();
    private static final ArrayList<Marker> userMarkers = new ArrayList<>();

    public static Boolean isMarkersON = true;
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    private static final TimeZone timeZone = TimeZone.getDefault();

    public static void importantMarkersCreate() {
        if (importantMarkers.size() == 0) {
            MarkerOptions marker1 = new MarkerOptions().position(new LatLng(52.079440, 47.732110)).icon(swordsicon1);  //база желтая
            importantMarkers.add(gMap.addMarker(marker1));
            MarkerOptions marker2 = new MarkerOptions().position(new LatLng(52.079159, 47.731756)).icon(swordsicon2);  // точка интереса синяя
            importantMarkers.add(gMap.addMarker(marker2));
            MarkerOptions marker3 = new MarkerOptions().position(new LatLng(52.079230, 47.730936)).icon(swordsicon3); //точка интереса желтая
            importantMarkers.add(gMap.addMarker(marker3));
            MarkerOptions marker4 = new MarkerOptions().position(new LatLng(52.079166, 47.730533)).icon(swordsicon4); //база синяя
            importantMarkers.add(gMap.addMarker(marker4));
            MarkerOptions marker5 = new MarkerOptions().position(new LatLng(52.079776, 47.731070)).icon(swordsicon5);  // валера
            importantMarkers.add(gMap.addMarker(marker5));
            MarkerOptions marker6 = new MarkerOptions().position(new LatLng(52.078645, 47.731751)).icon(swordsicon6);  // бомба
            importantMarkers.add(gMap.addMarker(marker6));
            MarkerOptions marker7 = new MarkerOptions().position(new LatLng(52.078817, 47.731735)).icon(swordsicon7);  // эвакуация
            importantMarkers.add(gMap.addMarker(marker7));
//        MarkerOptions marker8 = new MarkerOptions().position(new LatLng(55.6734572,85.1064004)).icon(swordsicon8);
//        importantMarkers.add(gMap.addMarker(marker8));
//        MarkerOptions marker9 = new MarkerOptions().position(new LatLng(55.6713818,85.104792)).icon(swordsicon9);
//        importantMarkers.add(gMap.addMarker(marker9));
            MarkerOptions markerM1 = new MarkerOptions().position(new LatLng(52.079139, 47.730093)).title("Мертвяк ЮГ").icon(blue_campicon);
            importantMarkers.add(gMap.addMarker(markerM1));
            MarkerOptions markerM2 = new MarkerOptions().position(new LatLng(52.079244, 47.732764)).title("Мертвяк").icon(yellow_campicon);
            importantMarkers.add(gMap.addMarker(markerM2));
        }
    }

    public static void setVisibleForImportantMarkers() {
        for (Marker marker : importantMarkers) {
            marker.setVisible(true);
        }
    }

    public static void reCreateMarkers() {
        if (mySavedCustomMarkersList != null)
            createCustomMapMarkers(mySavedCustomMarkersList);
        if (mySavedUsersMarkersList != null)
            createAllUsersMarkers(mySavedUsersMarkersList);
    }

    public static void createAllUsersMarkers(List<MyMarker> myMarkerList) {
        mySavedUsersMarkersList = myMarkerList;
        if (!isMarkersON)
            return;
        dateFormat.setTimeZone(timeZone);
        Bitmap bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
        ((Activity) MapsActivity.getContext()).runOnUiThread(() -> {
            for (Marker marker : userMarkers) {
                marker.remove();
            }
            userMarkers.clear();
            for (MyMarker myMarker : myMarkerList) {
                if (User.getInstance().getDeviceId().equals(myMarker.getDeviceId()))
                    continue;
                Marker marker = gMap.addMarker(new MarkerOptions()
                        .position(new LatLng(myMarker.getLatitude(), myMarker.getLongitude()))
                        .title(myMarker.getUserName())
                        .alpha(myMarker.getAlpha())
                        .snippet(dateFormat.format(new Date(myMarker.getTimestamp())))
                        .icon(icon));
                userMarkers.add(marker);
            }
        });
    }

    public static BitmapDescriptor getBitmapFromMyMarker(int item) {
        Bitmap bitmap;
        switch (item) {
            case 1:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.swords_icon);
                break;
            case 2:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.flag_red);
                break;
            case 3:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.flag_yellow);
                break;
            case 4:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.flag_green);
                break;
            case 5:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.flag_blue);
                break;
            default:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.focus);
                break;
        }
        return BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE_CUSTOMS, MARKER_SIZE_CUSTOMS, false));
    }

    public static void createCustomMapMarkers(List<MyMarker> myMarkerList) {
        mySavedCustomMarkersList = myMarkerList;
        ((Activity) MapsActivity.getContext()).runOnUiThread(() -> {
            for (Marker marker : customMarkers) {
                marker.remove();
            }
            customMarkers.clear();
            if (isMarkersON) {
                for (MyMarker myMarker : myMarkerList) {
                    dateFormat.setTimeZone(timeZone);
                    if (!myMarker.getTitle().equals("Маркер")) {
                        createTextMarker(myMarker);
                    }
                    Marker marker = gMap.addMarker(new MarkerOptions()
                            .position(new LatLng(myMarker.getLatitude(), myMarker.getLongitude()))
                            .title(myMarker.getTitle())
                            .alpha(myMarker.getAlpha())
                            .snippet(dateFormat.format(new Date(myMarker.getTimestamp())))
                            .icon(getBitmapFromMyMarker(myMarker.getItem())));
                    assert marker != null;
                    marker.setTag(myMarker.getTimestamp());
                    customMarkers.add(marker);
                }
            }
        });
    }

    private static void createTextMarker(@NonNull MyMarker myMarker) {
        String text = myMarker.getTitle().length() > 10 ? myMarker.getTitle().substring(0, 8) + "..." : myMarker.getTitle();
        Rect textBounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(25);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(8, 0, 0, Color.WHITE);
        paint.setAntiAlias(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.setBlendMode(BlendMode.SRC_OVER);
        }

        paint.getTextBounds(text, 0, text.length(), textBounds);
        int textWidth = textBounds.width();

        Bitmap bitmap1 = Bitmap.createBitmap(textBounds.width() + 15, 100, Bitmap.Config.ARGB_8888);
        Bitmap mutableBitmap = bitmap1.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawText(text, (mutableBitmap.getWidth() - textWidth) / 2F, 25, paint);

        Marker markerText = gMap.addMarker(new MarkerOptions()
                .position(new LatLng(myMarker.getLatitude(), myMarker.getLongitude()))
                .icon(BitmapDescriptorFactory.fromBitmap(mutableBitmap)));

        assert markerText != null;
        markerText.setAnchor(0.5F, 0f);
        customMarkers.add(markerText);
    }

    public static void markersOff() {
        for (Marker marker : customMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : userMarkers) {
            marker.setVisible(false);
        }
    }

    public static void markersOn() {
        for (Marker marker : customMarkers) {
            marker.setVisible(true);
        }
        for (Marker marker : userMarkers) {
            marker.setVisible(true);

        }
    }

}
