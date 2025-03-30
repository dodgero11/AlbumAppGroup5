package com.example.albumappgroup5.activities;

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;

public class ImageDetailFragment extends Fragment {
    private static final String ARG_IMAGE_PATH = "imagePath";
    private static final String ARG_IMAGE_NAME = "imageName";
    private static final String ARG_FILE_SIZE = "fileSize";
    private static final String ARG_DATE_TAKEN = "dateTaken";

    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private Matrix matrix = new Matrix();
    private float scaleFactor = 1.0f;
    private SwipeRefreshLayout swipeRefreshLayout;


    public static ImageDetailFragment newInstance(String imagePath, String imageName, long fileSize, String dateTaken, String cameFromOrigin) {
        ImageDetailFragment fragment = new ImageDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        args.putString(ARG_IMAGE_NAME, imageName);
        args.putLong(ARG_FILE_SIZE, fileSize);
        args.putString(ARG_DATE_TAKEN, dateTaken);
        args.putString("cameFrom", cameFromOrigin);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_detail, container, false);

        imageView = view.findViewById(R.id.imageViewDetail);
        TextView textViewInfo = view.findViewById(R.id.textViewImageInfo);
        swipeRefreshLayout = getActivity().findViewById(R.id.swipeRefreshLayout);


        if (getArguments() != null) {
            String imagePath = getArguments().getString(ARG_IMAGE_PATH);
            String imageName = getArguments().getString(ARG_IMAGE_NAME);
            long fileSize = getArguments().getLong(ARG_FILE_SIZE);
            String dateTaken = getArguments().getString(ARG_DATE_TAKEN);

            Glide.with(this).load(imagePath).into(imageView);
            String info = "Tên: " + imageName + "\nDung lượng: " + fileSize + " bytes\nNgày chụp: " + dateTaken;
            textViewInfo.setText(info);
        }

        // Initialize ScaleGestureDetector
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        // Set touch listener to handle pinch-to-zoom
        imageView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        return view;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setEnabled(false); // Disable swipe refresh while zooming
            }
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f)); // Limit zoom levels (min 1x, max 5x)

            matrix.setScale(scaleFactor, scaleFactor, imageView.getWidth() / 2f, imageView.getHeight() / 2f);
            imageView.setImageMatrix(matrix);
            return true;
        }
    }

    public void onDestroyView() {
        super.onDestroyView();

        if (getParentFragmentManager().isStateSaved())
            return; // Prevent crashes when state is saved

        Bundle result = new Bundle();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(true); // Re-enable swipe refresh after zooming
        }

        // Get "cameFrom" from arguments instead of static variable
        if (getArguments() != null && "mainActivity".equals(getArguments().getString("cameFrom"))) {
            result.putBoolean("refresh_images", true);
            getParentFragmentManager().setFragmentResult("image_detail_closed", result);
        }
    }
}
