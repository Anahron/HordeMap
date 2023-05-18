package ru.newlevel.hordemap;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class MessengerViewModel extends ViewModel {
    private final MessengerRepository repository;
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();

    public MessengerViewModel() {
        repository = new MessengerRepository();
    }

    public MutableLiveData<List<Message>> getMessagesLiveData() {
        return messagesLiveData;
    }
    public LiveData<Integer> getProgressLiveData() {
        return repository.getProgressLiveData();
    }

    public void uploadFile(Uri fileUri) {
        repository.uploadFileToDatabase(fileUri);
    }

    public void downloadFile(String url, String fileName) {
        repository.downloadFileFromDatabase(url, fileName);
    }

    public void stopLoadMessages(){
        repository.removeEventListener();
    }

    public synchronized void loadMessages() {
        long maxTimeStamp = getLastDisplayedMessageTimestamp();
        repository.getMessagesSinceTimestamp(maxTimeStamp, new MessengerRepository.Callback<>() {
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

    public void sendMessage(String message) {
        repository.sendMessage(message);
    }

    private long getLastDisplayedMessageTimestamp() {
        // Вернуть таймштамп последнего отображенного сообщения
        if (MessagesAdapter.lastDisplayedMessage != null)
            return MessagesAdapter.lastDisplayedMessage.getTimestamp();
        return 0;
    }


}
