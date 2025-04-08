package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Button;

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
import com.example.albumappgroup5.models.ImageModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AlbumCollectionFragment extends Fragment implements AlbumAdapter.OnAlbumClickListener {

    static private List<ImageModel> allImages;
    static private AlbumModel albumModel;
    private AlbumAdapter adapter; // Using global adapter
    private FloatingActionButton btnAddAlbum;
    private DatabaseHandler database;
    boolean backToMain = true;

    public static AlbumCollectionFragment newInstance(AlbumModel album, List<ImageModel> images) {
        AlbumCollectionFragment fragment = new AlbumCollectionFragment();
        Bundle args = new Bundle();
        albumModel = album;
        allImages = images;
        fragment.setArguments(args);
        return fragment;
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

        // Initialize adapter
        adapter = new AlbumAdapter(albumModel.getAlbumList(), this);
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
            } else if (albumModel.getAlbumList().contains(newAlbumName)) {
                Toast.makeText(getContext(), "Album already exists!", Toast.LENGTH_SHORT).show();
            } else {
                createAlbum(newAlbumName);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Create new album
    private void createAlbum(String albumName) {
        albumModel.addAlbum(albumName);
        albumModel.getAlbumImages().put(albumName, new ArrayList<>());

        database.insertAlbum(albumName);

        adapter.notifyDataSetChanged();
    }

    private void getAlbumsFromDatabase() {
        try {
            List<AlbumObject> tempAlbumList = database.getAlbums();
            for (AlbumObject album : tempAlbumList) {
                albumModel.addAlbum(album.getAlbumName());
            }
        } catch (Exception e) {
            Log.e("error", e.toString());
        }
    }

    @Override
    public void onAlbumClick(String albumName) {
        // Check if user is going back to main
        backToMain = false;

        Toast.makeText(getContext(), "Selected: " + albumName, Toast.LENGTH_SHORT).show();

        List<ImageModel> images = albumModel.getAlbumImages().getOrDefault(albumName, new ArrayList<>()); // Get images for album

        // Pass both album name and image list
        AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(albumName, images, allImages);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, albumDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onAlbumLongClick(String albumName) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete this album?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    albumModel.removeAlbum(albumName);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Album deleted", Toast.LENGTH_SHORT).show();
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
}
