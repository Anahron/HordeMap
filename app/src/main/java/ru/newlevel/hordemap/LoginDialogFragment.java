package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginDialogFragment extends DialogFragment {
    private final Context mContext;
    private final MapsActivity mMapsActivity;
    private static final String SEND_MASSAGE = "ОТПРАВИТЬ";

    public LoginDialogFragment(Context context, MapsActivity mapsActivity) {
        mContext = context;
        mMapsActivity = mapsActivity;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_login, null);

        EditText roomNumber = dialogView.findViewById(R.id.editTextNumber);
        roomNumber.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText name = dialogView.findViewById(R.id.editTextPersonName);

        builder.setView(dialogView)
                .setPositiveButton(SEND_MASSAGE, (dialog, which) -> {
                    String deviceID = MyServiceUtils.getDeviceId(mContext);
                    mMapsActivity.setPermission();
                    if (name.getText().toString().trim().length() == 0) {
                        User.getInstance().setUserName("Аноним");
                    } else
                        User.getInstance().setUserName(name.getText().toString().trim());
                    if (roomNumber.getText().toString().trim().length() == 0)
                        User.getInstance().setRoomId("0");
                    else
                        User.getInstance().setRoomId(roomNumber.getText().toString().trim());
                    User.getInstance().setDeviceId(deviceID);
                    LoginRequest.onLoginSuccess(mContext);
                })
                .setCancelable(false);

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().getDecorView().setOnTouchListener((v, event) -> {
            // Обработка нажатия на фоновую область
            return true; // Вернуть true, чтобы предотвратить закрытие диалога
        });
        builder.setCancelable(false);
        // Установите размеры диалога
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        return dialog;
    }
}
