package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.ImageModel;
import com.example.albumappgroup5.models.AlbumModel;

import java.util.List;

public class AlbumDetailFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    static private List<ImageModel> imagesOfAlbum;
    static private List<ImageModel> allOfImages;
    static private String nameOfAlbum;
    private TextView albumTitle;
    private GalleryAdapter adapter;
    private RecyclerView recyclerView;
    private DatabaseHandler database;
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
        view.findViewById(R.id.btnAddImage).setOnClickListener(v -> selectImageForAlbum());
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

        ImageLargeFragment imageLargeFragment = ImageLargeFragment.newInstance(
                image.getImagePath(),
                image.getName(),
                image.getFileSize(),
                image.getDateTaken(),
                "AlbumDetailFragment"
        );

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageLargeFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onImageLongClick(int position) {
        ImageModel selectedImage = imagesOfAlbum.get(position);

        new AlertDialog.Builder(getContext())
                .setTitle("Tùy chọn ảnh")
                .setItems(new CharSequence[]{"Đặt làm ảnh đại diện album", "Xóa ảnh khỏi album"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Set as album thumbnail
                            AlbumModel albumModel = new ViewModelProvider(requireActivity()).get(AlbumModel.class);
                            albumModel.setAlbumThumbnail(nameOfAlbum, selectedImage.getImagePath());
                            Toast.makeText(getContext(), "Đã đặt ảnh đại diện cho album", Toast.LENGTH_SHORT).show();
                            break;

                        case 1: // Delete
                            showDeleteConfirmationDialog(position);
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
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
