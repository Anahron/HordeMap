package ru.newlevel.hordemap;

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
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.maps.android.clustering.ClusterManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

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

    private static HashMap<String, String> savedmarkers = new HashMap<>();
    private static HashMap<String, String> savedMapMarkers = new HashMap<>();
    private static final ArrayList<Marker> mapMarkers = new ArrayList<>();
    private static final ArrayList<Marker> markers = new ArrayList<>();
    public static Boolean isMarkersON = true;
    public static int MARKER_SIZE = 60;

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

    public static void setVisible() {
        for (Marker marker : importantMarkers) {
            marker.setVisible(true);
        }
    }

    public static void createMarkers(HashMap<String, String> map) {
        System.out.println("В создание маркеров пришло: " + map);
        Log.d("Horde map", "Удаляются старые и создаются новые маркеры");
        savedmarkers = map;
        Bitmap bitmap = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwing);
        Bitmap bitmapcom = BitmapFactory.decodeResource(MapsActivity.getContext().getResources(), R.drawable.pngwingcomander);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE, MARKER_SIZE, false));
        BitmapDescriptor iconcom = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapcom, MARKER_SIZE, MARKER_SIZE, false));
        ((Activity) MapsActivity.getContext()).runOnUiThread(() -> {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
            for (String id : map.keySet()) {
                if (!User.getInstance().getRoomId().equals(id) && isMarkersON) {
                    System.out.println("Полученое значение по ключу для отрисовки маркера: " + map.get(id));
                    String[] data = Objects.requireNonNull(map.get(id)).split("/");
                    @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    TimeZone timeZone = TimeZone.getDefault();
                    dateFormat.setTimeZone(timeZone);
                    Marker marker = gMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                            .title(data[0])
                            .alpha(Float.parseFloat(data[4]))
                            .snippet(dateFormat.format(new Date(Long.parseLong(data[3]))))
                            .icon(icon));
                    markers.add(marker);
                }
            }
        });
    }

    public static void createMapMarkers(HashMap<String, String> map) {
        System.out.println("В создание map маркеров пришло: " + map);
        savedMapMarkers = map;
        ((Activity) MapsActivity.getContext()).runOnUiThread(() -> {
            for (Marker marker : mapMarkers) {
                marker.remove();
            }
            mapMarkers.clear();
            if (isMarkersON) {
                for (String id : map.keySet()) {
                    Bitmap bitmap;
                    String[] data = Objects.requireNonNull(map.get(id)).split("/");

                    switch (Integer.parseInt(data[5])) {
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
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_SIZE, MARKER_SIZE, false));

                    @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    TimeZone timeZone = TimeZone.getDefault();
                    dateFormat.setTimeZone(timeZone);
                    Marker marker = gMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                            .title(data[0])
                            .alpha(Float.parseFloat(data[4]))
                            .snippet(dateFormat.format(new Date(Long.parseLong(data[3]))))
                            .icon(icon));
                    markers.add(marker);

                    String text = data[0].length()>10 ? data[0].substring(0,8)+"..." : data[0];

                    Rect textBounds = new Rect();
                    Paint paint = new Paint();
                    paint.setTextSize(25);
                    paint.setColor(Color.BLACK);
                   paint.setFakeBoldText(true);
                   paint.setShadowLayer(3,1,1,Color.WHITE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        paint.setBlendMode(BlendMode.SRC_OVER);
                    }

                    paint.getTextBounds(text, 0, text.length(), textBounds);
                    int textWidth = textBounds.width();
                    int textHeight = textBounds.height();

                    Bitmap bitmap1 = Bitmap.createBitmap(textBounds.width()+5, 100, Bitmap.Config.ARGB_8888);
                    Bitmap mutableBitmap = bitmap1.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);

                    canvas.drawText(text, (mutableBitmap.getWidth() - textWidth) / 2, 25 , paint);

                    Marker marker1 = gMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(data[1]), Double.parseDouble(data[2])))
                            .icon(BitmapDescriptorFactory.fromBitmap(mutableBitmap)));
                    assert marker1 != null;
                    marker1.setAnchor(0.5F, 0f);
                    markers.add(marker1);

                }
            }
        });
    }

    public static void markersOff() {
        for (Marker marker : markers) {
            marker.remove();
        }
        for (Marker marker : mapMarkers) {
            marker.remove();
        }
    }

    public static void markersOn() {
        if (mapMarkers.isEmpty())
            createMapMarkers(savedMapMarkers);
        else
            for (Marker marker : mapMarkers) {
                marker.setVisible(true);
            }
        if (markers.isEmpty())
            createMarkers(savedmarkers);
        else
            for (Marker marker : markers) {
                marker.setVisible(true);
            }
    }

}
