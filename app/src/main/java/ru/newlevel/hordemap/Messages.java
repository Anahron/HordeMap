package ru.newlevel.hordemap;

import java.util.Objects;

public class Messages {
    private String userName;
    private String massage;
    private long timestamp;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMassage() {
        return massage;
    }

    public void setMassage(String massage) {
        this.massage = massage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        // Сравнение на основе идентификатора или других уникальных полей
        return Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public int hashCode() {
        // Используйте хэш-код идентификатора или других уникальных полей
        return Objects.hash(timestamp);
    }
}
