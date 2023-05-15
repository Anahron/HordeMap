package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jsibbold.zoomage.ZoomageView;

public class FullScreenImageActivity extends Activity {
  private ZoomageView imageView;

    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_full_screen_image);
        imageView = findViewById(R.id.myZoomageView);

        String imageUrl = getIntent().getStringExtra("imageUrl");

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        GlideApp.with(this)
                .load(storageReference)
                .into(imageView);

    }

}