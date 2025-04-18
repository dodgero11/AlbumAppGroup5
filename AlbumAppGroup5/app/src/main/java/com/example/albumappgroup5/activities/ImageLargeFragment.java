package com.example.albumappgroup5.activities;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.ImageDetailsObject;

public class ImageLargeFragment extends Fragment implements View.OnTouchListener {
    private static String imageID;
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private float scaleFactor = 1.0f;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseHandler database;

    // Constants for touch modes
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // For dragging
    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    private float oldDist = 1f;
    private boolean imageLoaded = false;

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

        // Initialize touch listeners and scale detector
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        imageView.setOnTouchListener(this);

        // Set the scale type to matrix for custom transformations
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Disable swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
        }

        if (getArguments() != null) {
            String imagePath = imageID;
            ImageDetailsObject tempImage = database.getImageDetails(imageID);

            // Load image with Glide and set up listener for when loading is complete
            Glide.with(this)
                    .load(imagePath)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView);

            // Set a listener to initialize the matrix once the image is loaded
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    if (imageView.getDrawable() != null) {
                        initializeMatrix();
                        imageLoaded = true;
                    } else {
                        // If image isn't loaded yet, try again in a moment
                        imageView.postDelayed(this, 100);
                    }
                }
            });

            largeImageName.setText(tempImage.getImageName());
        }
        return view;
    }

    private void initializeMatrix() {
        // Reset matrix
        matrix.reset();

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();
        int drawableWidth = imageView.getDrawable().getIntrinsicWidth();
        int drawableHeight = imageView.getDrawable().getIntrinsicHeight();

        // Calculate the scale needed to fit the image
        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        float scale = Math.min(scaleX, scaleY);

        // Calculate translation to center the image
        float dx = (viewWidth - drawableWidth * scale) / 2;
        float dy = (viewHeight - drawableHeight * scale) / 2;

        // Set initial matrix to fit center
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);

        // Save initial scale for reference
        scaleFactor = scale;

        // Apply matrix to image view
        imageView.setImageMatrix(matrix);

        // Save this initial matrix for reset functionality
        savedMatrix.set(matrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!imageLoaded) return false;

        scaleGestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startPoint.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(midPoint, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - startPoint.x;
                    float dy = event.getY() - startPoint.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        // Check if image is out of bounds and adjust if necessary
        // Map the current matrix to get the actual image bounds in view space.
        RectF rect = new RectF(0, 0,
                imageView.getDrawable().getIntrinsicWidth(),
                imageView.getDrawable().getIntrinsicHeight());
        matrix.mapRect(rect);

        // Get view dimensions.
        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        float deltaX = 0;
        float deltaY = 0;

        // Horizontal adjustment:
        if (rect.width() < viewWidth) {
            // Center if image is smaller than the view.
            deltaX = (viewWidth - rect.width()) / 2f - rect.left;
        } else {
            // If the image is larger than the view, ensure it doesn't go off-screen.
            if (rect.left > 0) {
                deltaX = -rect.left;  // Shift left edge to align with view's left.
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;  // Shift right edge to align with view's right.
            }
        }

        // Vertical adjustment:
        if (rect.height() < viewHeight) {
            // Center vertically if the image is smaller than the view.
            deltaY = (viewHeight - rect.height()) / 2f - rect.top;
        } else {
            // Ensure top and bottom edges are within bounds.
            if (rect.top > 0) {
                deltaY = -rect.top;  // Shift top edge up to align with view's top.
            } else if (rect.bottom < viewHeight) {
                deltaY = viewHeight - rect.bottom;  // Shift bottom edge down to align with view's bottom.
            }
        }

        // Apply the translation adjustments to the matrix.
        matrix.postTranslate(deltaX, deltaY);
        imageView.setImageMatrix(matrix);
        return true;
    }

    // Helper method for calculating distance between two touch points
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // Helper method for calculating midpoint between two touch points
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // Scale listener for pinch zoom
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Limit scale range
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));

            matrix.set(savedMatrix);
            matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                    detector.getFocusX(), detector.getFocusY());

            imageView.setImageMatrix(matrix);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            savedMatrix.set(matrix);
            super.onScaleEnd(detector);
        }
    }

    @Override
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