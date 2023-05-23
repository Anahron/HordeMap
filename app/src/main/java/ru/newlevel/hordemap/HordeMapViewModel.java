package ru.newlevel.hordemap;

import static ru.newlevel.hordemap.MapsActivity.isInactive;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

public class HordeMapViewModel extends ViewModel {
    private final HordeMapRepository repository;
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<MyMarker>> usersMarkersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<MyMarker>> customMarkersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isHaveNewMessages = new MutableLiveData<>();

    public HordeMapViewModel() {
        isHaveNewMessages.setValue(false);
        repository = new HordeMapRepository();
    }

    public MutableLiveData<List<Message>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public LiveData<Integer> getProgressLiveData() {
        return repository.getProgressLiveData();
    }

    public MutableLiveData<List<MyMarker>> getUsersMarkersLiveData() {
        return usersMarkersLiveData;
    }

    public MutableLiveData<List<MyMarker>> getCustomMarkersLiveData() {
        return customMarkersLiveData;
    }

    public MutableLiveData<Boolean> getIsHaveNewMessages() {
        return isHaveNewMessages;
    }

    public void uploadFile(Uri fileUri) {
        repository.uploadFileToDatabase(fileUri);
    }

    public void downloadFile(String url, String fileName) {
        repository.downloadFileFromDatabase(url, fileName);
    }

    public void stopLoadMessages(){
        repository.removeMessageEventListener();
    }

    public void stopLoadGeoData(){
        repository.removeMarkersEventListener();
    }

    public synchronized void loadMessagesListener() {
        long maxTimeStamp = getLastDisplayedMessageTimestamp();
        repository.getMessagesSinceTimestamp(maxTimeStamp, new HordeMapRepository.Callback<>() {
            @Override
            public void onSuccess(List<Message> result) {
                messagesLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                // Обработка ошибки чтения данных
            }
        });
    }

    public synchronized void loadGeoDataListener() {
        repository.getAllUsersGeoData(new HordeMapRepository.Callback<>() {
            @Override
            public void onSuccess(List<MyMarker> result) {
                usersMarkersLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                // Обработка ошибки чтения данных
            }
        });
        repository.getAllCustomMarkers(new HordeMapRepository.Callback<>() {
            @Override
            public void onSuccess(List<MyMarker> result) {
                customMarkersLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                // Обработка ошибки чтения данных
            }
        });

    }

    public void sendMarkerData(double latitude, double longitude, int selectedItem, String title) {
        repository.sendGeoDataToDatabase(latitude,longitude,selectedItem,title);
    }

    public void sendMarkerData(double latitude, double longitude){
        repository.sendGeoDataToDatabase(latitude,longitude);
    }

    public void checkForNewMessages(){
        repository.checkDatabaseForNewMessages(new HordeMapRepository.Callback<>() {
            @Override
            public void onSuccess(Boolean result) {
                isHaveNewMessages.setValue(result);
                System.out.println("Есть новые сообщения? " + result);
                if (isInactive && result)
                    DataUpdateService.getInstance().showNewMessageNotification();
            }

            @Override
            public void onError(String errorMessage) {

            }
        });
    }

    public void sendMessage(String message) {
        repository.sendMessage(message);
    }
    public void deleteMarker(Marker marker){
        repository.deleteMarkerFromDatabase(marker);
    }

    private long getLastDisplayedMessageTimestamp() {
        // Вернуть таймштамп последнего отображенного сообщения
        if (MessagesAdapter.lastDisplayedMessage != null)
            return MessagesAdapter.lastDisplayedMessage.getTimestamp();
        return 0;
    }
}
