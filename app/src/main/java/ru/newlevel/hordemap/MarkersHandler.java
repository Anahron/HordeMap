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

    private static final Bitmap bitmap0 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing);
    private static final Bitmap bitmap1 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing_yellow);
    private static final Bitmap bitmap2 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing_green);
    private static final Bitmap bitmap3 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing_blue);
    private static final Bitmap bitmap4 = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing_purple);
    private static BitmapDescriptor icon0 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap0, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
    private static BitmapDescriptor icon1 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap1, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
    private static BitmapDescriptor icon2 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap2, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
    private static BitmapDescriptor icon3 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap3, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
    private static BitmapDescriptor icon4 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap4, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));

    private static List<MyMarker> mySavedUsersMarkersList;
    private static List<MyMarker> mySavedCustomMarkersList;
    private static final ArrayList<Marker> customMarkers = new ArrayList<>();
    private static final ArrayList<Marker> userMarkers = new ArrayList<>();

    public static Boolean isMarkersON = true;
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    private static final TimeZone timeZone = TimeZone.getDefault();

    public static void reCreateMarkers() {
        icon0 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap1, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
        icon1 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap1, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
        icon2 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap2, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
        icon3 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap3, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
        icon4 = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap4, MARKER_SIZE_USERS, MARKER_SIZE_USERS, false));
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

        ((Activity) MapsActivity.getContext()).runOnUiThread(() -> {
            for (Marker marker : userMarkers) {
                marker.remove();
            }
            userMarkers.clear();
            for (MyMarker myMarker : myMarkerList) {
                if (User.getInstance().getDeviceId().equals(myMarker.getDeviceId()))
                    continue;
                BitmapDescriptor icon;
                switch (myMarker.getItem()) {
                    case 1:
                        icon = icon1;
                        break;
                    case 2:
                        icon = icon2;
                        break;
                    case 3:
                        icon = icon3;
                        break;
                    case 4:
                        icon = icon4;
                        break;
                    default:
                        icon = icon0;
                }
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
            case 11:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point1);
                break;
            case 12:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point2);
                break;
            case 13:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point3);
                break;
            case 14:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point4);
                break;
            case 15:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point5);
                break;
            case 16:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point6);
                break;
            case 17:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point7);
                break;
            case 18:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point8);
                break;
            case 19:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point9);
                break;
            default:
                bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.marker_point0);
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
                    if (!myMarker.getTitle().equals("Маркер") && myMarker.getItem() < 10) {
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
        String text = myMarker.getTitle().length() > 10 ? myMarker.getTitle().substring(0, 7) + "..." : myMarker.getTitle();
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
