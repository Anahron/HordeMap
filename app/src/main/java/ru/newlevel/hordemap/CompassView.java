package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
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
    private final float[] orientationAngles = new float[3];
    public static float azimuthDegrees = 0;
    private final float[] rotationVectorReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private static final int NUM_SAMPLES = 3;
    private final float[] azimuthSamples = new float[NUM_SAMPLES];
    private int currentSampleIndex = 0;
    private int lastX;
    private int lastY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                int deltaX = x - lastX;
                int deltaY = y - lastY;
                setTranslationX(getTranslationX() + deltaX);
                setTranslationY(getTranslationY() + deltaY);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sensorManager.unregisterListener(this);
    }
    protected void compasOFF() {
        sensorManager.unregisterListener(this);
    }
    protected void compassON() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_STATUS_ACCURACY_HIGH |
                50000 );
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        @SuppressLint("DrawAllocation") Matrix matrix = new Matrix();
        matrix.postRotate(-azimuthDegrees, bitmapWidth / 2F, bitmapHeight / 2F);
        matrix.postTranslate(centerX - bitmapWidth / 2F, centerY - bitmapHeight / 2F);

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
            float averageAzimuthDegrees = 0;
            for (int i = 0; i < NUM_SAMPLES; i++) {
                averageAzimuthDegrees += azimuthSamples[i];
            }
            averageAzimuthDegrees /= NUM_SAMPLES;
            MapsActivity.textView1.setTextSize(22F);
            MapsActivity.textView1.setText(((int) averageAzimuthDegrees > 0 ? (int) averageAzimuthDegrees : (int) averageAzimuthDegrees + 360) + "\u00B0");
            CompassView.azimuthDegrees = averageAzimuthDegrees > 0.0F ? averageAzimuthDegrees : averageAzimuthDegrees + 360.0F;
            invalidate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
