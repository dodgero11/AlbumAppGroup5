package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentTransaction;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.AlbumAdapter;
import com.example.albumappgroup5.models.AlbumModel;
import com.example.albumappgroup5.models.AlbumObject;
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AlbumCollectionFragment extends Fragment implements AlbumAdapter.OnAlbumClickListener {

    static private List<ImageDetailsObject> allImages;
    static private AlbumModel albumModel;
    private AlbumAdapter adapter; // Using global adapter
    private FloatingActionButton btnAddAlbum;
    private DatabaseHandler database;
    boolean backToMain = true;

    public static AlbumCollectionFragment newInstance(AlbumModel album, List<ImageDetailsObject> images) {
        AlbumCollectionFragment fragment = new AlbumCollectionFragment();
        Bundle args = new Bundle();
        albumModel = album;
        allImages = images;
        fragment.setArguments(args);
        return fragment;
    }

    private void loadAlbumThumbnailsFromPrefs() {
        SharedPreferences sharedPref = getContext().getSharedPreferences("AlbumPreferences", Context.MODE_PRIVATE);
        // Duyệt qua danh sách album có trong AlbumModel
        for (AlbumObject album : albumModel.getAlbumList()) {
            String thumbnail = sharedPref.getString("thumbnail_" + album.getAlbumName(), null);
            if (thumbnail != null) {
                albumModel.setAlbumThumbnail(album.getAlbumName(), thumbnail);
            }
        }
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_collection, container, false);
        database = DatabaseHandler.getInstance(getContext());

        // Set up RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get albums from database
        getAlbumsFromDatabase();

        Log.d("AlbumCollectionFragment", "Albums: " + albumModel);
        // Initialize adapter
        adapter = new AlbumAdapter(albumModel, this);

        adapter.setAlbumModel(albumModel);
        recyclerView.setAdapter(adapter);

        btnAddAlbum = view.findViewById(R.id.btnAddAlbum);
        btnAddAlbum.setOnClickListener(v -> showAddAlbumDialog());

        return view;
    }

    // Temporary dialog to enter new album's name
    private void showAddAlbumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create New Album");

        // Create EditText input field
        final EditText input = new EditText(getContext());
        input.setHint("Enter album name");
        builder.setView(input);

        // Set up buttons
        builder.setPositiveButton("Create", (dialog, which) -> {
            String newAlbumName = input.getText().toString().trim();

            if (newAlbumName.isEmpty()) {
                Toast.makeText(getContext(), "Album name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
            if (database.insertAlbum(newAlbumName)) {
                AlbumObject album = new AlbumObject(database.getAlbumID(newAlbumName), newAlbumName);
                createAlbum(album);
            } else {
                Toast.makeText(getContext(), "Album name already exists!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Create new album
    private void createAlbum(AlbumObject album) {
        getAlbumsFromDatabase();
        adapter.notifyDataSetChanged();
    }

    private void getAlbumsFromDatabase() {
        try {
            albumModel.clearAlbumList();
            List<AlbumObject> tempAlbumList = database.getAlbums();
            for (AlbumObject album : tempAlbumList) {
                // Add images from database to albums
                albumModel.addImageToAlbum(album.getAlbumName(), database.getAlbumDetailedImages(album.getAlbumID()));
                albumModel.addAlbum(album);
            }
            loadAlbumThumbnailsFromPrefs();
        } catch (Exception e) {
            Log.e("error", e.toString());
        }
    }

    @Override
    public void onAlbumClick(String albumName) {
        // Check if user is going back to main
        backToMain = false;

        passwordCheckAsync(albumName)
                .thenAccept(ok -> {
                    requireActivity().runOnUiThread(() -> {
                        if (ok) {
                            openAlbum(albumName);
                            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this.getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    public void openAlbum(String albumName) {
        List<ImageDetailsObject> images = albumModel.getAlbumImages().getOrDefault(albumName, new ArrayList<>()); // Get images for album

        // Pass both album name and image list
        AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(albumName, images, allImages);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, albumDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onAlbumLongClick(String albumName) {

        // Get the album object
        AlbumObject album = albumModel.getAlbumByName(albumName);

        passwordCheckAsync(albumName)
                .thenAccept(ok -> {
                    requireActivity().runOnUiThread(() -> {
                        if (ok) {
                            openDialogs(album);
                            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this.getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    public void openDialogs (AlbumObject album) {
        // Create options array
        final String[] options = {"Change Password", "Delete Album"};

        // Show options dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Album Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Change Password option
                        showChangePasswordDialog(album);
                    } else if (which == 1) {
                        // Delete Album option
                        confirmDeleteAlbum(album);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showChangePasswordDialog(AlbumObject album) {
        // Create a layout for the dialog
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.change_password_dialog, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        String currentPassword = database.getAlbumPassword(album.getAlbumID());
        if (!currentPassword.isEmpty()) {
            passwordInput.setText(currentPassword);
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Change Album Password")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Change password
                    String newPassword = passwordInput.getText().toString();
                    if (newPassword.isEmpty()) {
                        database.deleteAlbumPassword(album.getAlbumID());
                        Toast.makeText(getContext(), "Password removed", Toast.LENGTH_SHORT).show();
                    } else if (database.getAlbumPassword(album.getAlbumID()).isEmpty()) {
                        database.insertAlbumPassword(album.getAlbumID(), newPassword);
                        Toast.makeText(getContext(), "Password added", Toast.LENGTH_SHORT).show();
                    } else {
                        database.updateAlbumPassword(album.getAlbumID(), newPassword);
                        Toast.makeText(getContext(), "Password updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDeleteAlbum(AlbumObject album) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete this album?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    database.deleteAlbum(album.getAlbumID());
                    albumModel.removeAlbum(album);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Resume current fragment
    @Override
    public void onResume() {
        super.onResume();
        backToMain = true; // Reset when returning
    }

    // Destroy current fragment and goes back to main
    @Override
    public void onDestroyView() {
        Bundle args = new Bundle();
        super.onDestroyView();

        if (getActivity() != null && backToMain) {
            args.putBoolean("refresh_images", true);
            getParentFragmentManager().setFragmentResult("album_closed", args);
        }
    }

    public CompletableFuture<Boolean> passwordCheckAsync(String albumName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AlbumObject album = albumModel.getAlbumByName(albumName);

        // if no password, complete immediately
        if (database.getAlbumPassword(album.getAlbumID()).isEmpty()) {
            future.complete(true);
            return future;
        }

        // otherwise show the dialog
        PasswordDialogFragment dlg = PasswordDialogFragment.newInstance(album.getAlbumID());
        dlg.setCancelable(false);
        dlg.show(this.getParentFragmentManager(), "PasswordDialog");

        this.getParentFragmentManager().setFragmentResultListener(
                "password_result", this,
                (requestKey, bundle) ->
                        future.complete(bundle.getBoolean("password_correct", false))
        );

        return future;
    }
}
