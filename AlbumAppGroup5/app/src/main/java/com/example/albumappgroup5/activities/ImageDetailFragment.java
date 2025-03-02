package com.example.albumappgroup5.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;

public class ImageDetailFragment extends Fragment {
    private static final String ARG_IMAGE_PATH = "imagePath";
    private static final String ARG_IMAGE_NAME = "imageName";
    private static final String ARG_FILE_SIZE = "fileSize";
    private static final String ARG_DATE_TAKEN = "dateTaken";
    private String cameFrom;

    public static ImageDetailFragment newInstance(String imagePath, String imageName, long fileSize, String dateTaken, String cameFrom) {
        ImageDetailFragment fragment = new ImageDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        args.putString(ARG_IMAGE_NAME, imageName);
        args.putLong(ARG_FILE_SIZE, fileSize);
        args.putString(ARG_DATE_TAKEN, dateTaken);
        fragment.cameFrom = cameFrom;
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_detail, container, false);

        ImageView imageView = view.findViewById(R.id.imageViewDetail);
        TextView textViewInfo = view.findViewById(R.id.textViewImageInfo);

        if (getArguments() != null) {
            String imagePath = getArguments().getString(ARG_IMAGE_PATH);
            String imageName = getArguments().getString(ARG_IMAGE_NAME);
            long fileSize = getArguments().getLong(ARG_FILE_SIZE);
            String dateTaken = getArguments().getString(ARG_DATE_TAKEN);

            Glide.with(this).load(imagePath).into(imageView);
            String info = "Tên: " + imageName + "\nDung lượng: " + fileSize + " bytes\nNgày chụp: " + dateTaken;
            textViewInfo.setText(info);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Send a result back to MainActivity
        Bundle result = new Bundle();

        // If came from MainActivity, refresh images
        if (cameFrom.equals("mainActivity"))
            result.putBoolean("refresh_images", true);
        getParentFragmentManager().setFragmentResult("image_detail_closed", result);
    }
}
