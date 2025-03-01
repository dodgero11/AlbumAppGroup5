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

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.AlbumAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AlbumCollectionFragment extends Fragment implements AlbumAdapter.OnAlbumClickListener {
    private List<String> albumList;
    private Map<String, List<String>> albumImages; // Stores album names & images
    private AlbumAdapter adapter; // Properly using global adapter
    private Button btnAddAlbum;
    boolean backToMain = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_collection, container, false);

        // Initialize album data
        albumList = new ArrayList<>();
        albumImages = new HashMap<>();

        // Sample album list
        albumList.add("Vacation");
        albumList.add("Family");
        albumList.add("Friends");
        albumList.add("Work");
        albumList.add("Memories");

        // Set up RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter
        adapter = new AlbumAdapter(albumList, this);
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
            } else if (albumList.contains(newAlbumName)) {
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
        albumList.add(albumName);
        albumImages.put(albumName, new ArrayList<>()); // Initialize album with empty image list
        adapter.notifyDataSetChanged(); // Update RecyclerView when a new album is added
    }

    @Override
    public void onAlbumClick(String albumName) {
        // Check if user is going back to main
        backToMain = false;

        Toast.makeText(getContext(), "Selected: " + albumName, Toast.LENGTH_SHORT).show();

        List<String> images = albumImages.getOrDefault(albumName, new ArrayList<>()); // Get images for album

        // Pass both album name and image list
        AlbumDetailFragment albumDetailFragment = AlbumDetailFragment.newInstance(albumName, images);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, albumDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
