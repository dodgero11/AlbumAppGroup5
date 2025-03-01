package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.ImageModel;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    private static final String ARG_ALBUM_NAME = "albumName";
    private static final String ARG_IMAGE_LIST = "imageList";

    private String albumName;
    private List<String> imagePaths;
    private GalleryAdapter adapter;
    private RecyclerView recyclerView;

    public static AlbumDetailFragment newInstance(String albumName, List<String> imageList) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_NAME, albumName);
        args.putStringArrayList(ARG_IMAGE_LIST, new ArrayList<>(imageList)); // Convert to ArrayList
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_detail, container, false);

        if (getArguments() != null) {
            albumName = getArguments().getString(ARG_ALBUM_NAME);
            imagePaths = getArguments().getStringArrayList(ARG_IMAGE_LIST);
        }

        if (imagePaths == null) {
            imagePaths = new ArrayList<>();
        }

        recyclerView = view.findViewById(R.id.recyclerViewAlbumImages);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new GalleryAdapter(getContext(), convertToImageModelList(imagePaths), this);
        recyclerView.setAdapter(adapter);

        // Adding images
        view.findViewById(R.id.btnAddImageToAlbum).setOnClickListener(v -> selectImageForAlbum());

        return view;
    }

    private void selectImageForAlbum() {

    }

    private List<ImageModel> convertToImageModelList(List<String> imagePaths) {
        List<ImageModel> images = new ArrayList<>();
        for (String path : imagePaths) {
            images.add(new ImageModel(path, "Image", 0, "Today"));
        }
        return images;
    }

    @Override
    public void onImageClick(int position) {
        Toast.makeText(getContext(), "Clicked on image at position " + position, Toast.LENGTH_SHORT).show();
        // To be implemented (Opens activity_image_detail.xml)
    }

    @Override
    public void onImageLongClick(int position) {
        Toast.makeText(getContext(), "Long click on image at position " + position, Toast.LENGTH_SHORT).show();
        // To be implemented (Deletes photo in-app)
    }

    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    imagePaths.remove(position);
                    adapter.notifyItemRemoved(position);

                    Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
