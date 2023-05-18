package ru.newlevel.hordemap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InfoDialogFragment extends DialogFragment {
    private final Context mContext;
    private final MapsActivity mMapsActivity;
    private static final String INFO = "Раскрытие информации.";
    private static final String INFO_MASSAGE = " Для корректной работы приложения требуется собирать Ваши данные о местоположении для работы функции обмена геоданными и построения маршрута пройденого пути, в том числе в фоновом режиме, даже если приложение закрыто и не используется. Мы не передаем данные о вашем местоположении третьим лицам и используем их только внутри нашего приложения, в том числе для передачи другим пользователям.";
    private static final String ACCEPT = "Я ПОНИМАЮ";
    private static final String DECLINE ="Отказываюсь";

    public InfoDialogFragment(Context context, MapsActivity mapsActivity) {
        mContext = context;
        mMapsActivity = mapsActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(INFO);
        builder.setMessage(INFO_MASSAGE);
        builder.setPositiveButton(ACCEPT, (dialog, which) -> {
            LoginDialogFragment dialogFragment = new LoginDialogFragment(mContext, mMapsActivity);
            dialogFragment.show(mMapsActivity.getSupportFragmentManager(), "login_dialog");
            mMapsActivity.setPermission();
        });
        builder.setNegativeButton(DECLINE, (dialog, which) -> mMapsActivity.finish());
        builder.setCancelable(false);
        return builder.create();
    }
}
