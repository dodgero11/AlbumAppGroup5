package com.example.albumappgroup5.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;

public class ImageDetailActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView textViewInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        imageView = findViewById(R.id.imageViewDetail);
        textViewInfo = findViewById(R.id.textViewImageInfo);

        String imagePath = getIntent().getStringExtra("imagePath");
        String imageName = getIntent().getStringExtra("imageName");
        long fileSize = getIntent().getLongExtra("fileSize", 0);
        String dateTaken = getIntent().getStringExtra("dateTaken");

        Glide.with(this).load(imagePath).into(imageView);
        String info = "Tên: " + imageName + "\nDung lượng: " + fileSize + " bytes\nNgày chụp: " + dateTaken;
        textViewInfo.setText(info);
    }
}
