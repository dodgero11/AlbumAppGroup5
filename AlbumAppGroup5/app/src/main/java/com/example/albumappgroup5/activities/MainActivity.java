package com.example.albumappgroup5.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentTransaction;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.AlbumModel;
import com.example.albumappgroup5.models.ImageModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GalleryAdapter.OnImageClickListener, ImageActivityCallback {
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private AlbumModel albumModel;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<ImageModel> imageList;
    private Button btnOpenAlbum, btnOpenCamera;
    private String currentPhotoPath;

    // Taking a photo with the camera
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Image captured successfully!", Toast.LENGTH_SHORT).show();
                    loadImagesFromStorage();
                } else {
                    Toast.makeText(this, "Camera capture failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize model for ablums
        albumModel = new ViewModelProvider(this).get(AlbumModel.class);

        // Sample album list
        albumModel.addAlbum("Vacation");
        albumModel.addAlbum("Family");
        albumModel.addAlbum("Friends");
        albumModel.addAlbum("Work");
        albumModel.addAlbum("Memories");

        albumModel.getAlbumImages().put("Vacation", new ArrayList<>());
        albumModel.getAlbumImages().put("Family", new ArrayList<>());
        albumModel.getAlbumImages().put("Friends", new ArrayList<>());
        albumModel.getAlbumImages().put("Work", new ArrayList<>());
        albumModel.getAlbumImages().put("Memories", new ArrayList<>());

        recyclerView = findViewById(R.id.recyclerViewImages);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        btnOpenAlbum = findViewById(R.id.btnOpenAlbum);
        btnOpenAlbum.setOnClickListener(view -> openAlbum());

        btnOpenCamera = findViewById(R.id.btnCaptureImage);
        btnOpenCamera.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        });

        // Listen for the result when ImageDetailFragment is closed
        getSupportFragmentManager().setFragmentResultListener("image_detail_closed", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("refresh_images", false)) {
                    reloadGalleryView();
                }
            }
        });

        checkAndRequestPermissions();

        applySettings(); // load settings from previous session (if available)

        // testing settings
//        Intent test = new Intent(this, AppSettings.class);
//        startActivityForResult(test, 0);

    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            Log.v("info", "returned: " + resultCode);
            if (resultCode == RESULT_OK)
            {
                applySettings();
            }
        }
    }

    private void applySettings () // read preferences and apply changes
    {
        SharedPreferences preferences = getSharedPreferences(Global.SETTINGS, Activity.MODE_PRIVATE);
        if (preferences == null)
            return;

        if (preferences.getBoolean(Global.SETTINGS_NIGHT, false))
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Reload images when returning to MainActivity
    private void reloadGalleryView() {
        findViewById(R.id.recyclerViewImages).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_button_container).setVisibility(View.VISIBLE);
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);

        loadImagesFromStorage(); // Refresh images
    }


    // Accessing the camera
    private void openCamera() {
        Toast.makeText(this, "Opening camera", Toast.LENGTH_SHORT).show();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CAMERA", "Error creating image file", ex);
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    // Creating images from the photoshoot
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Request permission to get images from internal storage
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
        } else {
            loadImagesFromStorage();
        }
    }

    // Check for permission request's response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImagesFromStorage();
            } else {
                Toast.makeText(this, "Permission Denied! Go to Settings > Apps > YourApp > Permissions to enable.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Load images from internal storage
    private void loadImagesFromStorage() {
        imageList = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.DATA, // File path
                MediaStore.Images.Media.DISPLAY_NAME, // File name
                MediaStore.Images.Media.SIZE, // File size
                MediaStore.Images.Media.DATE_TAKEN // Date
        };

        Cursor cursor = getContentResolver().query(collection, projection, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));

                // Find photo's date taken or downloaded
                String dateTaken = null;
                int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                if (dateAddedIndex != -1) {
                    long dateAdded = cursor.getLong(dateAddedIndex);
                    if (dateAdded > 0) {
                        dateTaken = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dateAdded * 1000L));
                    }
                }

                // If DATE_ADDED is missing, fallback to file last modified date
                if ((dateTaken == null || dateTaken.equals("0")) && dataIndex != -1) {
                    String filePath = cursor.getString(dataIndex);
                    if (filePath != null) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            long lastModified = file.lastModified();
                            dateTaken = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(lastModified));
                        }
                    }
                }

                // Everything failed, resorts to placeholder
                if (dateTaken == null || dateTaken.equals("0")) {
                    dateTaken = "Unknown Date";
                }

                imageList.add(new ImageModel(imagePath, imageName, fileSize, dateTaken));
            }
            cursor.close();
        }

        adapter = new GalleryAdapter(this, imageList, this);
        recyclerView.setAdapter(adapter);
    }

    // Click on image to go to details
    @Override
    public void onImageClick(int position) {
        ImageModel image = imageList.get(position); // Get the clicked image

        ImageDetailFragment imageDetailFragment = ImageDetailFragment.newInstance(
                image.getImagePath(),
                image.getName(),
                image.getFileSize(),
                image.getDateTaken(),
                "mainActivity"
        );

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        // Hide RecyclerView and buttons when showing the detail fragment
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.bottom_button_container).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    // Long click to remove image (only in-app, not yet in internal storage)
    @Override
    public void onImageLongClick(int position) {
        imageList.remove(position);
        adapter.notifyItemRemoved(position);
    }

    // Go to albums section
    private void openAlbum() {
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.bottom_button_container).setVisibility(View.GONE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, AlbumCollectionFragment.newInstance(albumModel, imageList));
        transaction.addToBackStack(null);
        transaction.commit();

        // Show the fragment container
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    @Override
    public void selectOption(int index, String option) {
        switch (option) // implement later
        {
            case "details": // open image details fragment/activity
                break;
            case "delete": // delete image
                break;
            case "cancel": // close and destroy the options fragment
                break;
            default:
                break;
        }
    }
}
