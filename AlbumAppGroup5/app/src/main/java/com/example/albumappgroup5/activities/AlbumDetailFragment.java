package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.ImageModel;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    static private List<ImageModel> imagesOfAlbum;
    static private List<ImageModel> allOfImages;
    static private String nameOfAlbum;
    private TextView albumTitle;
    private GalleryAdapter adapter;
    private RecyclerView recyclerView;
    public static AlbumDetailFragment newInstance(String albumName, List<ImageModel> imageList, List<ImageModel> allImages) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        nameOfAlbum = albumName;
        imagesOfAlbum = imageList;
        allOfImages = allImages;
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_detail, container, false);

        albumTitle = view.findViewById(R.id.albumTitle);
        albumTitle.setText(nameOfAlbum);

        recyclerView = view.findViewById(R.id.recyclerViewAlbumImages);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new GalleryAdapter(getContext(), imagesOfAlbum, this);
        recyclerView.setAdapter(adapter);

        // Listen for the result when AddImageFragment is closed
        getParentFragmentManager().setFragmentResultListener("add_image_result", this, (requestKey, result) -> {
            ImageModel newImage = result.getParcelable("new_image");
            if (newImage != null) {
                imagesOfAlbum.add(newImage);
                adapter.notifyItemInserted(imagesOfAlbum.size() - 1);
                Toast.makeText(getContext(), "Image added to album", Toast.LENGTH_SHORT).show();
            }
        });

        // Adding images
        view.findViewById(R.id.btnAddImageToAlbum).setOnClickListener(v -> selectImageForAlbum());

        return view;
    }

    private void selectImageForAlbum() {
        AddImageFragment addImageFragment = AddImageFragment.newInstance(allOfImages, imagesOfAlbum );
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, addImageFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onImageClick(int position) {
        Toast.makeText(getContext(), "Clicked on image at position " + position, Toast.LENGTH_SHORT).show();

        ImageModel image = imagesOfAlbum.get(position); // Get the clicked image

        ImageDetailFragment imageDetailFragment = ImageDetailFragment.newInstance(
                image.getImagePath(),
                image.getName(),
                image.getFileSize(),
                image.getDateTaken(),
                "AlbumDetailFragment"
        );

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onImageLongClick(int position) {
        imagesOfAlbum.remove(position);
        adapter.notifyItemRemoved(position);
    }

    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    imagesOfAlbum.remove(position);
                    adapter.notifyItemRemoved(position);

                    Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
