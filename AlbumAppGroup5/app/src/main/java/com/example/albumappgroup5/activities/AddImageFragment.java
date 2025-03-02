package com.example.albumappgroup5.activities;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.ImageModel;

public class AddImageFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    private static List<ImageModel> allOfImages;
    private ImageView selectedImage;
    private String selectedImagePath;
    private GalleryAdapter adapter;
    private int clickedPosition;

    public static AddImageFragment newInstance(List<ImageModel> allImages) {
        AddImageFragment fragment = new AddImageFragment();
        Bundle args = new Bundle();
        allOfImages = allImages; // Set image list
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_image, container, false);

        selectedImage = view.findViewById(R.id.imagePreview);
        Button btnAddImage = view.findViewById(R.id.btnAddImage);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewAllImages);

        // Setup RecyclerView with GalleryAdapter
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new GalleryAdapter(getContext(), allOfImages, this);
        recyclerView.setAdapter(adapter);

        // Add selected image to album
        btnAddImage.setOnClickListener(v -> addImageToAlbum());

        return view;
    }

    @Override
    public void onImageClick(int position) {
        // Select the image
        selectedImagePath = allOfImages.get(position).getImagePath();
        clickedPosition = position;
        selectedImage.setImageURI(android.net.Uri.parse(selectedImagePath));
    }

    @Override
    public void onImageLongClick(int position) {
        // To be implemented (Deletes photo in-app)
    }

    private void addImageToAlbum() {
        if (selectedImagePath != null) {
            Bundle result = new Bundle();
            result.putParcelable("new_image", allOfImages.get(clickedPosition));
            getParentFragmentManager().setFragmentResult("add_image_result", result);
            getParentFragmentManager().popBackStack(); // Close fragment
        } else {
            Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }
}
