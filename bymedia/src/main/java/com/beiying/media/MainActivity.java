package com.beiying.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.beiying.ai.ImageAI;
import com.beiying.media.image.BigImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private ImageView mFaceImage;
    private Bitmap faceBitmap;
    private File mCascadeFile;
    private ImageAI mImageAI;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        try {
            InputStream inputStream = getAssets().open("long.png");
            ((BigImageView)(findViewById(R.id.big_view))).setImage(inputStream);

            mFaceImage = (ImageView) findViewById(R.id.face_image);
            faceBitmap = BitmapFactory.decodeStream(getAssets().open("timg.jpeg"));
            mFaceImage.setImageBitmap(faceBitmap);
            mImageAI = new ImageAI();
            copyCascadeFile();

            mImageAI.loadCascade(mCascadeFile.getAbsolutePath());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void faceDetection(View view) {
        if (mImageAI == null) mImageAI = new ImageAI();
        mImageAI.faceDetectionSaveInfo(faceBitmap);
        mFaceImage.setImageBitmap(faceBitmap);
    }

    private void copyCascadeFile() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "libpcascade_frontalface.xml");
            if (mCascadeFile.exists()) return;
            FileOutputStream fos = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            fos.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
}
