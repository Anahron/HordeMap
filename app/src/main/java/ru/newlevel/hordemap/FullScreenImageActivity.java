package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;
import java.io.File;


public class FullScreenImageActivity extends Activity {


    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_full_screen_image);
        ZoomageView imageView = findViewById(R.id.myZoomageView);

        String imagePath = getIntent().getStringExtra("imageUrl");

        File imageFile = new File(imagePath);

        Glide.with(this)
                .load(imageFile)
                .into(imageView);
    }

}