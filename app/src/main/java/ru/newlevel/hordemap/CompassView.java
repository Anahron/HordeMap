package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.DataSender.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CompassView extends View implements SensorEventListener {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap;
    private SensorManager sensorManager;
    private float[] orientationAngles = new float[3];
    public static float azimuthDegrees = 0;
    private float[] rotationVectorReading = new float[3];
    private float[] rotationMatrix = new float[9];
    private static final int NUM_SAMPLES = 3;
    private float[] azimuthSamples = new float[NUM_SAMPLES];
    private int currentSampleIndex = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
                return super.onTouchEvent(event);

    }

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.compas);
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
    }


    protected void compasOn() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                100000 );
    }

    protected void ocompasOFF() {
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(-azimuthDegrees, bitmapWidth / 2, bitmapHeight / 2);
        matrix.postTranslate(centerX - bitmapWidth / 2, centerY - bitmapHeight / 2);

        canvas.drawBitmap(bitmap, matrix, paint);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, rotationVectorReading, 0, rotationVectorReading.length);

            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorReading);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float azimuthRadians = orientationAngles[0];
            float azimuthDegrees = (float) Math.toDegrees(azimuthRadians);

            // Добавляем текущее значение азимута в массив сэмплов
            azimuthSamples[currentSampleIndex] = azimuthDegrees;

            // Переходим к следующему индексу сэмпла
            currentSampleIndex = (currentSampleIndex + 1) % NUM_SAMPLES;

            // Вычисляем среднее значение из нескольких последних сэмплов
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MapsActivity.textView1.setTextSize(22F);
                    float averageAzimuthDegrees = 0;
                    for (int i = 0; i < NUM_SAMPLES; i++) {
                        averageAzimuthDegrees += azimuthSamples[i];
                    }
                    averageAzimuthDegrees /= NUM_SAMPLES;
                    MapsActivity.textView1.setText(((int) averageAzimuthDegrees > 0 ? (int) averageAzimuthDegrees : (int) averageAzimuthDegrees + 360) + "\u00B0");
                    CompassView.azimuthDegrees = averageAzimuthDegrees;
                    invalidate();
                }
            });



        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
