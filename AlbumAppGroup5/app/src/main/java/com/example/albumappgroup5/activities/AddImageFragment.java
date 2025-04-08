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
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.ImageModel;

public class AddImageFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    private static List<ImageDetailsObject> allOfImages;
    private ImageView selectedImage;
    private String selectedImagePath;
    private GalleryAdapter adapter;
    private int clickedPosition;

    // albumImages được lấy từ database

    private List<ImageDetailsObject> albumImages;

    public static AddImageFragment newInstance(List<ImageDetailsObject> allImages, List<ImageDetailsObject> albumImages) {
        AddImageFragment fragment = new AddImageFragment();
        Bundle args = new Bundle();
        allOfImages = allImages; // Set image list
        fragment.setArguments(args);
        fragment.setAlbumImages(albumImages);
        return fragment;
    }

    public void setAlbumImages(List<ImageDetailsObject> albumImages) {
        this.albumImages = albumImages;
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
        // Chọn ảnh
        selectedImagePath = allOfImages.get(position).getImageID();
        clickedPosition = position;
        selectedImage.setImageURI(android.net.Uri.parse(selectedImagePath));
    }

    @Override
    public void onImageLongClick(int position) {
        // Xử lý long click nếu cần (ví dụ: xoá ảnh trong danh sách chọn)
    }

    // Kiểm tra xem ảnh đã có trong album chưa (so sánh dựa trên đường dẫn)
    private boolean isImageAlreadyAdded(String imagePath) {
        if (albumImages != null) {
            for (ImageDetailsObject img : albumImages) {
                if (img.getImageID().equals(imagePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addImageToAlbum() {
        if (selectedImagePath != null) {
            if (isImageAlreadyAdded(selectedImagePath)) {
                Toast.makeText(getContext(), "Image is already added to the album", Toast.LENGTH_SHORT).show();
                return;
            }
            // Nếu ảnh chưa được thêm, truyền ảnh qua Bundle
            Bundle result = new Bundle();
            result.putParcelable("new_image", allOfImages.get(clickedPosition));
            getParentFragmentManager().setFragmentResult("add_image_result", result);
            getParentFragmentManager().popBackStack(); // Đóng fragment
        } else {
            Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }
}

