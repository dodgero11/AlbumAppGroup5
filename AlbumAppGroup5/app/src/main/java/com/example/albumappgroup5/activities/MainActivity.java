package com.example.albumappgroup5.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.view.View;
import android.media.MediaScannerConnection;

import android.content.ContentValues;
import android.widget.Toolbar;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements GalleryAdapter.OnImageClickListener, ImageActivityCallback, SimpleMessageCallback {
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private AlbumModel albumModel;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<ImageDetailsObject> imageList;
    private String currentPhotoPath;
    private String currentActiveFragment = null; // Will store fragment tag

    // fragments variable
    ActionButtonFragment buttonContainer;
    ImageOptionsFragment imageOptionsFragment;

    // database handler
    DatabaseHandler database;

    // Sorting variables
    private static final int SORT_DATE_DESC = 0; // Newest first (default)
    private static final int SORT_DATE_ASC = 1;  // Oldest first
    private static final int SORT_NAME_ASC = 2;  // A-Z
    private static final int SORT_NAME_DESC = 3; // Z-A
    private int currentSortOrder = SORT_DATE_DESC; // Default sort order

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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

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
        fragmentTransaction.hide(buttonContainer);
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
        });

        // Listen for the result when ImageDetailFragment is closed
        getSupportFragmentManager().setFragmentResultListener("image_detail_closed", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("refresh_images", false)) {
                    currentActiveFragment = null;
                    bottomNav.setSelectedItemId(R.id.nav_image);
                    reloadGalleryView();
                }
            }
        });

        getSupportFragmentManager().setFragmentResultListener("album_closed", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("refresh_images", false)) {
                    currentActiveFragment = null;
                    bottomNav.setSelectedItemId(R.id.nav_image);
                    reloadGalleryView();
                }
            }
        });

        // Listen for the result when ImageEditorFragment is closed
        getSupportFragmentManager().setFragmentResultListener("image_editor_closed", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("refresh_images", false)) {
                    currentActiveFragment = null;
                    // Set highlighted button to image hub
                    bottomNav.setSelectedItemId(R.id.nav_image);
                    reloadGalleryView();
                }
            }
        });

        // Listen for the result when ImageSearchFragment is closed
        getSupportFragmentManager().setFragmentResultListener("image_search_closed", this, new FragmentResultListener() {
                @Override
                public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                    if (result.getBoolean("refresh_images", false)) {
                        currentActiveFragment = null;
                        bottomNav.setSelectedItemId(R.id.nav_image);
                        reloadGalleryView();
                    }
            }
        });

                // toolbar button to open settings
        Toolbar homeToolbar = findViewById(R.id.homeToolbar);
        homeToolbar.setNavigationOnClickListener(v -> {
            Intent settingsActivity = new Intent(this, AppSettings.class);
            startActivityForResult(settingsActivity, 0);
        });

        // Add menu items to the toolbar
        homeToolbar.inflateMenu(R.menu.main_menu);

        // Handle menu item clicks
        homeToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_sort) {
                showSortingDialog();
                return true;
            }

            return false;
        });

        loadSortOrderPreference(); // Load saved sort preference

        checkAndRequestPermissions();

        applySettings(); // load settings from previous session (if available)

        // Set up a listener for navigation item selection
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId(); // obtain the selected item ID from your source

                if (itemId == R.id.nav_image ) {
                    returnHome();
                } else if (itemId == R.id.nav_album) {
                    openAlbum();
                } else if (itemId == R.id.nav_camera) {
                    getCameraPermission();
                } else if (itemId == R.id.nav_search){
                    imageSearch();
                } else {
                   return false;
                }
                return true;
            }
        });
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
        else if (requestCode == 1) { // 1 : the details activity
            if (resultCode == RESULT_OK)
            {
                loadImagesFromStorage();
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
        findViewById(R.id.homeToolbar).setVisibility(View.VISIBLE);
        fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);

        fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
        fragmentTransaction.hide(buttonContainer);
        fragmentTransaction.commit();

        loadImagesFromStorage();
    }

    private void returnHome() {
        // Pop all fragments on the back stack.
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Make sure to show the main UI elements (RecyclerView and the bottom container).
        findViewById(R.id.recyclerViewImages).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

        // Create the button container fragment again.
        buttonContainer = ActionButtonFragment.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);
        fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
        fragmentTransaction.hide(buttonContainer);
        fragmentTransaction.commit();

        // Optionally hide the fragment container if it overlaps.
        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
    }

    // Go to albums section
    private void openAlbum() {
        // Check if album is already open
        if ("album_fragment".equals(currentActiveFragment)) {
            return;
        }
        // Set current active fragment to album
        currentActiveFragment = "album_fragment";

        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, AlbumCollectionFragment.newInstance(albumModel, imageList));
        transaction.addToBackStack(null);
        transaction.commit();

        // Show the fragment container
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
    }

    private void imageSearch() {
        // Check if album is already open
        if ("image_search_fragment".equals(currentActiveFragment)) {
            return;
        }
        // Set current active fragment to album
        currentActiveFragment = "image_search_fragment";

        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

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

        // Load all images first without specific sorting from MediaStore
        HashSet<String> hashSet = new HashSet<>();

        Cursor cursor = getContentResolver().query(collection, projection, null, null, null);
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

                String imageHash = getImageHash(imagePath);

                if (hashSet.contains(imageHash)) {
                    Log.d("Duplicates", "Duplicate found: " + imagePath);
                }
                else {
                    hashSet.add(imageHash);

                    // Create image object
                    ImageDetailsObject tempImage = new ImageDetailsObject(imagePath, imageName, null, timeAddedDate, imagePath);

                    // Check if image password exists in database
                    String tempPassword = database.getImagePassword(tempImage.getImageID());

                    // If it exists, use password info from database
                    if (!tempPassword.equals("")) {
                        tempImage.setHasPassword(true);
                        tempImage.setPasswordProtected(true);
                        // Don't set the actual password - that stays in the database
                    } else {
                        // Insert as new image if it doesn't exist
                        try {
                            database.insertImage(tempImage);
                        } catch (Exception e) {
                            Log.e("error", e.toString());
                        }
                    }

                    imageList.add(tempImage);
                }
            }
            cursor.close();
        }

        // Now sort the imageList based on the selected sort order
        sortImageList();

        adapter = new GalleryAdapter(this, imageList, this);
        recyclerView.setAdapter(adapter);
    }

    // New method to sort the imageList
    private void sortImageList() {
        // Update all images' names to match database
        for (ImageDetailsObject image : imageList) {
            image.setImageName(database.getImageName(image.getImageID()));
        }

        switch (currentSortOrder) {
            case SORT_DATE_ASC:
                // Sort by date, oldest first
                Collections.sort(imageList, (img1, img2) -> {
                    if (img1.getTimeAdded() == null || img2.getTimeAdded() == null)
                        return 0;
                    return img1.getTimeAdded().compareTo(img2.getTimeAdded());
                });
                break;

            case SORT_DATE_DESC:
                // Sort by date, newest first
                Collections.sort(imageList, (img1, img2) -> {
                    if (img1.getTimeAdded() == null || img2.getTimeAdded() == null)
                        return 0;
                    return img2.getTimeAdded().compareTo(img1.getTimeAdded());
                });
                break;

            case SORT_NAME_ASC:
                // Sort by name, A-Z
                Collections.sort(imageList, (img1, img2) -> {
                    String name1 = img1.getImageName() != null ? img1.getImageName() : "";
                    String name2 = img2.getImageName() != null ? img2.getImageName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;

            case SORT_NAME_DESC:
                // Sort by name, Z-A
                Collections.sort(imageList, (img1, img2) -> {
                    String name1 = img1.getImageName() != null ? img1.getImageName() : "";
                    String name2 = img2.getImageName() != null ? img2.getImageName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                break;
        }
    }

    // Click on image to go to details
    @Override
    public void onImageClick(int position) {
        ImageDetailsObject image = imageList.get(position); // Get the clicked image

        // Check if image has password protection
        if (image.isPasswordProtected()) {
            passwordCheckAsync(position)
                    .thenAccept(ok -> {
                        runOnUiThread(() -> {
                            if (ok) {
                                openImageLargeFragment(image);
                            } else {
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        } else {
            // No password, show image directly
            openImageLargeFragment(image);
        }
    }

    // Helper method to open the image fragment
    private void openImageLargeFragment(ImageDetailsObject image) {
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
        findViewById(R.id.homeToolbar).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    // Long click to open options menu
    @Override
    public void onImageLongClick(int position) {
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
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
                ImageDetailsObject image = imageList.get(index); // Get the clicked image

                // Check if image has password protection
                if (image.isPasswordProtected()) {
                    passwordCheckAsync(index)
                            .thenAccept(ok -> {
                                runOnUiThread(() -> {
                                    if (ok) {
                                        Intent detailsActivity = new Intent(this, ImageDetailsActivity.class);
                                        detailsActivity.putExtra("imageID", imageList.get(index).getImageID());
                                        startActivityForResult(detailsActivity, 1);
                                    } else {
                                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                } else {
                    Intent detailsActivity = new Intent(this, ImageDetailsActivity.class);
                    detailsActivity.putExtra("imageID", imageList.get(index).getImageID());
                    startActivityForResult(detailsActivity, 1);
                }
                break;

            case "delete": // delete image
                if (Build.VERSION.SDK_INT >= 30) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        Uri uri = Uri.parse("package:com.example.albumappgroup5");

                        startActivityForResult(
                                new Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        uri
                                ), 2
                        );
                    }
                }

                File file = new File(imageList.get(index).getImageID());
                if (file.delete()) {
                    database.deleteImage(imageList.get(index).getImageID()); // delete all image info from db
                    imageList.remove(index);
                    adapter.notifyItemRemoved(index);
                    Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Failed to delete image.\nCheck if permission is granted.", Toast.LENGTH_SHORT).show();
                }

            case "cancel": // close and destroy the options fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (!fragmentManager.popBackStackImmediate("BUTTON_CONTAINER", 0)) {
                    fragmentTransaction.replace(R.id.fragmentContainerBottom, buttonContainer);
                    fragmentTransaction.addToBackStack("BUTTON_CONTAINER");
                }
                fragmentTransaction.hide(buttonContainer);
                fragmentTransaction.commit();
                findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
                break;
            case "setWallpaper":
                ImageDetailsObject setWallPaperImage = imageList.get(index); // Get the clicked image

                // Check if image has password protection
                if (setWallPaperImage.isPasswordProtected()) {
                    passwordCheckAsync(index)
                            .thenAccept(ok -> {
                                runOnUiThread(() -> {
                                    if (ok) {
                                        setAsWallpaperWithGlide(imageList.get(index).getImageID());
                                    } else {
                                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                } else {
                    setAsWallpaperWithGlide(imageList.get(index).getImageID());
                }
                break;
            case "edit":
                ImageDetailsObject editImage = imageList.get(index); // Get the clicked image

                // Check if image has password protection (similar to your other cases)
                if (editImage.isPasswordProtected()) {
                    passwordCheckAsync(index)
                            .thenAccept(ok -> {
                                runOnUiThread(() -> {
                                    if (ok) {
                                        openImageEditorFragment(editImage.getImageID());
                                    } else {
                                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                } else {
                    openImageEditorFragment(editImage.getImageID());
                    findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
                }
                break;
            default:
                break;
        }
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

    public CompletableFuture<Boolean> passwordCheckAsync(int index) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ImageDetailsObject image = imageList.get(index);

        // if no password, complete immediately
        if (!image.isPasswordProtected()) {
            future.complete(true);
            return future;
        }

        // otherwise show the dialog
        PasswordDialogFragment dlg = PasswordDialogFragment.newInstance(image.getImageID());
        dlg.setCancelable(false);
        dlg.show(getSupportFragmentManager(), "PasswordDialog");

        getSupportFragmentManager().setFragmentResultListener(
                "password_result", this,
                (requestKey, bundle) ->
                        future.complete(bundle.getBoolean("password_correct", false))
        );

        return future;
    }

    // Sorting
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            showSortingDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSortingDialog() {
        // Create a bottom sheet or alert dialog with sorting options
        String[] sortOptions = {"Newest First", "Oldest First", "Name (A-Z)", "Name (Z-A)"};

        new AlertDialog.Builder(this)
                .setTitle("Sort Images By")
                .setSingleChoiceItems(sortOptions, currentSortOrder, (dialog, which) -> {
                    currentSortOrder = which;
                    saveCurrentSortOrder(); // Save user preference
                    loadImagesFromStorage(); // Reload with new sort order
                    dialog.dismiss();
                })
                .show();
    }

    // Save sort preference
    private void saveCurrentSortOrder() {
        SharedPreferences preferences = getSharedPreferences(Global.SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("sort_order", currentSortOrder);
        editor.apply();
    }

    // Load sort preference
    private void loadSortOrderPreference() {
        SharedPreferences preferences = getSharedPreferences(Global.SETTINGS, MODE_PRIVATE);
        currentSortOrder = preferences.getInt("sort_order", SORT_DATE_DESC); // Default to newest first
    }

    // Edit Image Fragment
    private void openImageEditorFragment(String imageId) {
        ImageEditorFragment imageEditorFragment = ImageEditorFragment.newInstance(imageId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageEditorFragment);
        transaction.addToBackStack("IMAGE_EDITOR");
        transaction.commit();

        // Hide RecyclerView and buttons when showing the editor fragment
        findViewById(R.id.recyclerViewImages).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainerBottom).setVisibility(View.GONE);
        findViewById(R.id.homeToolbar).setVisibility(View.GONE);
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        // When the back button is pressed, make sure the Bottom Navigation is visible
        if (findViewById(R.id.bottom_navigation).getVisibility() == View.GONE) {
            findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
