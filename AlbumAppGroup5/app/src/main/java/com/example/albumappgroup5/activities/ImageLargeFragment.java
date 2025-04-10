package com.example.albumappgroup5.activities;

import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.example.albumappgroup5.models.ImageDetailsObject;

import java.util.Date;

public class ImageLargeFragment extends Fragment {
    private static String imageID;
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private Matrix matrix = new Matrix();
    private float scaleFactor = 1.0f;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseHandler database;


    public static ImageLargeFragment newInstance(String imagePath, String cameFromOrigin) {
        ImageLargeFragment fragment = new ImageLargeFragment();
        Bundle args = new Bundle();
        imageID = imagePath;
        args.putString("cameFrom", cameFromOrigin);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_large, container, false);
        database = DatabaseHandler.getInstance(getContext());

        imageView = view.findViewById(R.id.imageViewDetail);
        TextView largeImageName = view.findViewById(R.id.largeImageName);
        swipeRefreshLayout = getActivity().findViewById(R.id.swipeRefreshLayout);

        // Disable swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false); // Disable swipe refresh while zooming
        }

        if (getArguments() != null) {
            String imagePath = imageID;
            ImageDetailsObject tempImage = database.getImageDetails(imageID);
//            long fileSize = getArguments().getLong(ARG_FILE_SIZE);
//            String dateTaken = getArguments().getString(ARG_DATE_TAKEN);

            Glide.with(this).load(imagePath).into(imageView);
//            String info = "Tên: " + imageName + "\nDung lượng: " + fileSize + " bytes\nNgày chụp: " + dateTaken;
            largeImageName.setText(tempImage.getImageName());
        }
        return view;
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
