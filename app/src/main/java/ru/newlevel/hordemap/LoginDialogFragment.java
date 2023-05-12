package ru.newlevel.hordemap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginDialogFragment extends DialogFragment {
    private final Context mContext;
    private final MapsActivity mMapsActivity;
    private static final String SEND_MASSAGE = "ОТПРАВИТЬ";
    private static final String AUTHORIZATION = "Авторизация";
    private static final String AUTHORIZATION_MASSAGE = " Введите номер телефона \nформата '891312341212' \nили идентификатор";

    public LoginDialogFragment(Context context, MapsActivity mapsActivity) {
        mContext = context;
        mMapsActivity = mapsActivity;
    }
    interface UserCallback {
        void onUserFound(boolean isFound);
    }

    private void getUserNameById(String userId, UserCallback callback) {
        System.out.println("Ищем документ с именем " + userId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            System.out.println("Вошли в \"users\"");
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    System.out.println("Получили документ " + document);
                    User user = User.getInstance();
                    user.setUserId(userId);
                    user.setUserName(document.getString("userName"));
                    callback.onUserFound(true);
                } else {
                    System.out.println("Юзер не найден");
                    callback.onUserFound(false);
                }
            } else {
                System.out.println("Ошибка");
                callback.onUserFound(false);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(AUTHORIZATION);
        builder.setMessage(AUTHORIZATION_MASSAGE);
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);
        builder.setPositiveButton(SEND_MASSAGE, (dialog, which) -> {
            mMapsActivity.setPermission();
            final String phoneNumber = input.getText().toString().trim();
            getUserNameById(phoneNumber, isFound -> {
                if (isFound) {
                    LoginRequest.onLoginSuccess(mContext);
                } else {
                    LoginRequest.onLoginFailure(mContext);
                }
            });
        });
        builder.setCancelable(false);

        return builder.create();
    }
}
