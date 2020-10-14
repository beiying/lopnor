package com.beiying.media;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.beiying.media.image.BigImageView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        try {
            InputStream inputStream = getAssets().open("long.png");
            ((BigImageView)(findViewById(R.id.big_view))).setImage(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
