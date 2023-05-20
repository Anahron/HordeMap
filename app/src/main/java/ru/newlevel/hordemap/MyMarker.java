package ru.newlevel.hordemap;

public class MyMarker {
    private String userName;
    private double latitude;
    private double longitude;
    private String deviceId;
    private long timestamp;
    private int item;
    private String title;
    private float alpha;

    public MyMarker() {
        // пустой конструктор требуется для Firebase
    }

    public MyMarker(String userName, double latitude, double longitude, String deviceId, long timestamp, int item, String title) {
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.item = item;
        this.title = title;
    }

    public float getAlpha() {
        if (alpha == 0)
            return 1.0F;
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDeviceId() {
        if (deviceId == null)
            return "";
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }

    public String getTitle() {
        if (title == null)
            return "";
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
