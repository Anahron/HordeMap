package ru.newlevel.hordemap;

public class User {
    private static User instance = null;
    private String roomId;
    private String userName;
    private String deviceId;
    private int marker;

    private User() {
    }

    public static synchronized User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRoomId() {
        if (roomId == null || roomId.equals(""))
            return "0";
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserName() {
        if (userName == null)
            return "Аноним";
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getMarker() {
        return marker;
    }

    public void setMarker(int marker) {
        this.marker = marker;
    }
}