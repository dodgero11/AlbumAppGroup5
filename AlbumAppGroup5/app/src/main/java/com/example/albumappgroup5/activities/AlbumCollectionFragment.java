package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.AlbumAdapter;
import com.example.albumappgroup5.models.AlbumModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AlbumCollectionFragment extends Fragment implements AlbumAdapter.OnAlbumClickListener {
    private AlbumModel albumModel;
    private AlbumAdapter adapter; // Using global adapter
    private Button btnAddAlbum;
    boolean backToMain = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_collection, container, false);

        // Initialize model for ablums
        albumModel = new ViewModelProvider(requireActivity()).get(AlbumModel.class);

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

        // Set up RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter
        adapter = new AlbumAdapter(albumModel.getAlbumList(), this);
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
        adapter.notifyDataSetChanged(); // Update RecyclerView when a new album is added
    }

    @Override
    public void onAlbumClick(String albumName) {
        // Check if user is going back to main
        backToMain = false;

        Toast.makeText(getContext(), "Selected: " + albumName, Toast.LENGTH_SHORT).show();

        List<String> images = albumModel.getAlbumImages().getOrDefault(albumName, new ArrayList<>()); // Get images for album

        // Pass both album name and image list
        AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(albumName, images);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, albumDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
        super.onDestroyView();

        if (getActivity() != null && backToMain) {
            View recyclerViewImages = requireActivity().findViewById(R.id.recyclerViewImages);
            View bottomButtonContainer = requireActivity().findViewById(R.id.bottom_button_container);
            View fragmentContainer = requireActivity().findViewById(R.id.fragmentContainer);

            if (recyclerViewImages != null) recyclerViewImages.setVisibility(View.VISIBLE);
            if (bottomButtonContainer != null) bottomButtonContainer.setVisibility(View.VISIBLE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        }
    }


}
