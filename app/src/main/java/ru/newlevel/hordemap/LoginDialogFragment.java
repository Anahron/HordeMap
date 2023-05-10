package ru.newlevel.hordemap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class LoginDialogFragment extends DialogFragment {
    private final Context mContext;
    private static final String SEND_MASSAGE = "ОТПРАВИТЬ";
    private static final String AUTHORIZATION = "Авторизация";
    private static final String AUTHORIZATION_MASSAGE = " Введите номер телефона \nформата '891312341212' \nили идентификатор";

    public LoginDialogFragment(Context context) {
        mContext = context;
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
            final String phoneNumber = input.getText().toString().trim();
            final String[] answerFromServer = {""};
            Thread thread = new Thread(() -> answerFromServer[0] = GeoUpdateService.requestInfoFromServer(phoneNumber));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (answerFromServer[0].equals("404") || answerFromServer[0].equals("")) {
                LoginRequest.onLoginFailure(mContext);
            } else {
                LoginRequest.onLoginSuccess(phoneNumber, answerFromServer[0], mContext);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.setCancelable(false);

        return builder.create();
    }
}
