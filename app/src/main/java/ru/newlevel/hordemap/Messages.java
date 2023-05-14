package ru.newlevel.hordemap;

import java.util.Objects;

public class Messages {
    private String userName;
    private String massage;
    private long timestamp;

    public String getUserName() {
        return userName;
    }

    public String getMassage() {
        return massage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Messages other = (Messages) obj;
        return Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }
}
