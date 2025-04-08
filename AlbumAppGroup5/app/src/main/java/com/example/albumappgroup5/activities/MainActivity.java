package com.example.albumappgroup5.activities;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.media.MediaScannerConnection;

import android.content.ContentValues;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.AlbumModel;
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.ImageModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements GalleryAdapter.OnImageClickListener, ImageActivityCallback, SimpleMessageCallback {
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private AlbumModel albumModel;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<ImageDetailsObject> imageList;
    private String currentPhotoPath;

    // fragments variable
    ActionButtonFragment buttonContainer;
    ImageOptionsFragment imageOptionsFragment;

    // database handler
    DatabaseHandler database;

    // Handling the result of the camera intent
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d("CAMERA", "Camera result OK");

                    if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                        Uri photoUri = Uri.parse(currentPhotoPath);

                        try (InputStream inputStream = getContentResolver().openInputStream(photoUri)) {
                            if (inputStream != null) {
                                Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show();

                                // Optional: Refresh gallery for immediate visibility
                                MediaScannerConnection.scanFile(
                                        this,
                                        new String[]{photoUri.getPath()},
                                        new String[]{"image/jpeg"},
                                        null
                                );
                            } else {
                                Toast.makeText(this, "Image capture failed. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        } catch (IOException e) {
                            Log.e("CAMERA", "Error verifying image file", e);
                            Toast.makeText(this, "Error accessing saved image.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to get photo path.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("CAMERA", "Camera operation failed, result code: " + result.getResultCode());
                    Toast.makeText(this, "Camera operation failed", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = DatabaseHandler.getInstance(this);

        // try to create database on first start
        SharedPreferences databasePreferences = getSharedPreferences(Global.PREFERENCE_DATABASE, MODE_PRIVATE);
        if (!databasePreferences.getBoolean("initialized", false)) {
            if (database.createDatabase()) {
                SharedPreferences.Editor editor = databasePreferences.edit();
                editor.putBoolean("initialized", true);
                editor.apply();
            }
        }

        albumModel = new ViewModelProvider(this).get(AlbumModel.class);

        recyclerView = findViewById(R.id.recyclerViewImages);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // fragments
        buttonContainer = ActionButtonFragment.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);
        fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
        fragmentTransaction.commit();

        imageOptionsFragment = ImageOptionsFragment.newInstance();

        // Swipe refresh
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("SWIPE_REFRESH", "Refreshing gallery...");

            // Reload gallery data
            loadImagesFromStorage();

            // Stop the refreshing animation
            swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(this, "Gallery refreshed!", Toast.LENGTH_SHORT).show();
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

        getSupportFragmentManager().setFragmentResultListener("album_closed", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("refresh_images", false)) {
                    reloadGalleryView();
                }
            }
        });

        checkAndRequestPermissions();

        applySettings(); // load settings from previous session (if available)

        // testing settings (use this sample when calling the settings activity)
//        Intent test = new Intent(this, AppSettings.class);
//        startActivityForResult(test, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.closeDatabase();
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) { // 0 : the settings activity
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
        findViewById(R.id.fragmentContainerBottom).setVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.popBackStack("BUTTON_CONTAINER", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        buttonContainer = ActionButtonFragment.newInstance();
        findViewById(R.id.fragmentContainerBottom).setVisibility(View.VISIBLE);
        fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);

        fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
        fragmentTransaction.commit();

        loadImagesFromStorage();
    }

    private void returnHome() {
        // Pop all fragments on the back stack.
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Make sure to show the main UI elements (RecyclerView and the bottom container).
        findViewById(R.id.recyclerViewImages).setVisibility(View.VISIBLE);

        // Create the button container fragment again.
        buttonContainer = ActionButtonFragment.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);
        fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
        fragmentTransaction.commit();

        // Optionally hide the fragment container if it overlaps.
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
    }

    private void imageSearch() {
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.commit();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, ImageSearchFragment.newInstance());
        transaction.addToBackStack(null);
        transaction.commit();

        // Show the fragment container
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    // Accessing the camera
    private void openCamera() {
        Log.d("CAMERA", "Opening camera");
        Toast.makeText(this, "Opening camera", Toast.LENGTH_SHORT).show();

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure a camera app is available
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the image file using MediaStore (Recommended for public storage)
        ContentValues values = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";

        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri photoURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (photoURI != null) {
            currentPhotoPath = photoURI.toString();  // Store URI as the path
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // Required for write access
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            Log.e("CAMERA", "Error creating image file via MediaStore");
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                loadImagesFromStorage();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                loadImagesFromStorage();
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

        HashSet<String> hashSet = new HashSet<>();

        Cursor cursor = getContentResolver().query(collection, projection, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));

                Date timeAddedDate = null;
                int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                // Use DATE_ADDED (in seconds) if available
                if (dateAddedIndex != -1) {
                    long dateAdded = cursor.getLong(dateAddedIndex);
                    if (dateAdded > 0) {
                        timeAddedDate = new Date(dateAdded * 1000L);
                    }
                }

                // Fallback: if DATE_ADDED is missing, use file's last modified date
                if (timeAddedDate == null && dataIndex != -1) {
                    String filePath = cursor.getString(dataIndex);
                    if (filePath != null) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            timeAddedDate = new Date(file.lastModified());
                        }
                    }
                }

                // If both methods fail, default to current time
                if (timeAddedDate == null) {
                    timeAddedDate = new Date();
                }
//                ImageDetailsObject tempImage = new ImageDetailsObject(imagePath, imageName, null, timeAddedDate, imagePath);
//                imageList.add(tempImage);
                String imageHash = getImageHash(imagePath);

                if (hashSet.contains(imageHash)) {
                    Log.d("Duplicates", "Duplicate found: " + imagePath);
                }
                else {
                    hashSet.add(imageHash);
                    ImageDetailsObject tempImage = new ImageDetailsObject(imagePath, imageName, null, timeAddedDate, imagePath);
                    imageList.add(tempImage);
                    try {
                        database.insertImage(tempImage);
                    }
                    catch (Exception e) {
                        Log.e("error", e.toString());
                    }
                }
            }
            cursor.close();
        }

        adapter = new GalleryAdapter(this, imageList, this);
        recyclerView.setAdapter(adapter);
    }

    // Click on image to go to details
    @Override
    public void onImageClick(int position) {
        ImageDetailsObject image = imageList.get(position); // Get the clicked image

        ImageLargeFragment imageLargeFragment = ImageLargeFragment.newInstance(
                image.getImageID(),
                "mainActivity"
        );
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageLargeFragment);
        transaction.addToBackStack("IMAGE_LARGE");
        transaction.commit();

        // Hide RecyclerView and buttons when showing the detail fragment
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainerBottom).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    // Long click to open options menu
    @Override
    public void onImageLongClick(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        String currentFragment;
        try {
            currentFragment = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
        }
        catch (NullPointerException e) {
            currentFragment = null;
        }

        if (Objects.equals(currentFragment, "IMAGE_OPTIONS")) {
            imageOptionsFragment.changeIndex(position);
            return;
        }
        if (!fragmentManager.popBackStackImmediate("IMAGE_OPTIONS", 0)) {
            fragmentTransaction.replace(R.id.fragmentContainerBottom, imageOptionsFragment);
            fragmentTransaction.addToBackStack("IMAGE_OPTIONS");
        }
        fragmentTransaction.commit();
        imageOptionsFragment.changeIndex(position);
    }

    // Go to albums section
    private void openAlbum() {
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.commit();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, AlbumCollectionFragment.newInstance(albumModel, imageList));
        transaction.addToBackStack(null);
        transaction.commit();

        // Show the fragment container
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    private void getCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void selectOption(int index, String option) { // message from option fragment
        switch (option) // implement later
        {
            case "details": // open image details fragment/activity
                Intent detailsActivity = new Intent(this, ImageDetailsActivity.class);
                detailsActivity.putExtra("imageID", imageList.get(index).getImageID());
                startActivity(detailsActivity);
                break;
            case "delete": // delete image
                Log.v("info", String.valueOf(index));
                // remove image (only in-app, not yet in internal storage)
                imageList.remove(index);
                adapter.notifyItemRemoved(index);
            case "cancel": // close and destroy the options fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (!fragmentManager.popBackStackImmediate("BUTTON_CONTAINER", 0)) {
                    fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);
                    fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
                }
                fragmentTransaction.commit();
                break;
            case "setWallpaper":
                setAsWallpaperWithGlide(imageList.get(index).getImageID());

                break;
            default:
                break;
        }
    }
    private void setAsWallpaperWithGlide(String imagePath) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                runOnUiThread(() -> Toast.makeText(this, "Ảnh không tồn tại", Toast.LENGTH_SHORT).show());
                return;
            }

            try {
                // Dùng Glide để load ảnh bitmap theo kích thước màn hình
                int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

                Bitmap bitmap = Glide.with(this)
                        .asBitmap()
                        .load(imageFile)
                        .submit(screenWidth, screenHeight) // load đúng size màn hình
                        .get(); // blocking get() trong background thread

                if (bitmap != null) {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                    wallpaperManager.setBitmap(bitmap);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Đã đặt ảnh làm hình nền", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi khi đặt hình nền", Toast.LENGTH_SHORT).show());
            }
        });
    }



    @Override
    public void receiveMessage(String message) {
        switch (message)
        {
            case "OPEN ALBUM":
                openAlbum();
                break;
            case "TAKE PHOTO":
                getCameraPermission();
                break;
            case "RETURN HOME":
                returnHome();
                break;
            case "SEARCH IMAGE":
                imageSearch();
                break;
        }
    }

    public String getImageHash(String imagePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(new File(imagePath));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            is.close();

            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
