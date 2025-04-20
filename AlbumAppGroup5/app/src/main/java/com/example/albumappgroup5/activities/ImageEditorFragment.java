package com.example.albumappgroup5.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.albumappgroup5.R;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ImageEditorFragment extends Fragment {
    private String imageId;
    private ImageView imagePreview;
    private Uri imageUri;
    private Bitmap editedBitmap;
    private Bitmap displayBitmap; // For showing crop overlay

    // Crop related variables
    private RectF cropRect;
    private boolean inCropMode = false;
    private Paint cropPaint;
    private int currentCorner = -1; // -1: none, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right
    private float lastTouchX, lastTouchY;
    private static final float CORNER_TOUCH_THRESHOLD = 40; // pixels

    // Button references
    private Button cropButton;

    private SwipeRefreshLayout swipeRefreshLayout;

    public static ImageEditorFragment newInstance(String imageId) {
        ImageEditorFragment fragment = new ImageEditorFragment();
        Bundle args = new Bundle();
        args.putString("imageId", imageId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageId = getArguments().getString("imageId");
        }


        // Initialize crop paint
        cropPaint = new Paint();
        cropPaint.setColor(Color.WHITE);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_editor, container, false);

        imagePreview = view.findViewById(R.id.imagePreview);

        // Disable swipe refresh
        swipeRefreshLayout = getActivity().findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
        }

        // Check if imageId is a file path or a content URI
        if (imageId.startsWith("content://")) {
            // It's already a content URI
            imageUri = Uri.parse(imageId);
        } else {
            // It's a file path, create a file URI
            File imageFile = new File(imageId);
            imageUri = Uri.fromFile(imageFile);
        }

        // Load the image
        try {
            if (imageUri.getScheme().equals("file")) {
                // Use BitmapFactory for file URIs
                editedBitmap = android.graphics.BitmapFactory.decodeFile(imageUri.getPath());
            } else {
                // Use ContentResolver for content URIs
                editedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            }

            // Check for discrepancies between editedBitmap and displayed image
            RectF tempRect = new RectF(
                    editedBitmap.getWidth() * 0f,
                    editedBitmap.getHeight() * 0f,
                    editedBitmap.getWidth() * 1f,
                    editedBitmap.getHeight() * 1f
            );

            displayBitmap = editedBitmap.copy(editedBitmap.getConfig(), true);

            // DisplayBitMap
            Log.d("ImageEditor", "Loaded bitmap: width=" + editedBitmap.getWidth() + ", height=" + editedBitmap.getHeight() + ", config=" + editedBitmap.getConfig() + ", byteCount=" + editedBitmap.getByteCount());

            Log.d("ImageEditor", "Displayed image dimensions: width=" + displayBitmap.getWidth() + ", height=" + displayBitmap.getHeight());


            if (tempRect.width() != editedBitmap.getWidth() || tempRect.height() != editedBitmap.getHeight()) {
                // Fix editedBitmap to match the displayed image
                Log.d("ImageEditor", "Fixing editedBitmap to match displayed image");
                Log.d("ImageEditor", "Original bitmap dimensions: width=" + editedBitmap.getWidth() +
                        ", height=" + editedBitmap.getHeight());
                Log.d("ImageEditor", "Displayed image dimensions: width=" + tempRect.width() + ", height=" + tempRect.height());
                editedBitmap = Bitmap.createScaledBitmap(editedBitmap, (int) tempRect.width(), (int) tempRect.height(), false);
            }

            if (editedBitmap != null) {
                displayBitmap = editedBitmap.copy(editedBitmap.getConfig(), true);
                imagePreview.setImageBitmap(displayBitmap);
                Log.d("ImageEditor", "Loaded bitmap: width=" + editedBitmap.getWidth() +
                        ", height=" + editedBitmap.getHeight() +
                        ", config=" + editedBitmap.getConfig() +
                        ", byteCount=" + editedBitmap.getByteCount());
            } else {
                Toast.makeText(getContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ImageEditorFragment", "Error loading image", e);
        }

        // Set up buttons
        Button rotateButton = view.findViewById(R.id.btnRotate);
        cropButton = view.findViewById(R.id.btnCrop);
        Button saveButton = view.findViewById(R.id.btnSave);
        Button cancelButton = view.findViewById(R.id.btnCancel);

        rotateButton.setOnClickListener(v -> rotateImage());
        cropButton.setOnClickListener(v -> toggleCropMode());
        saveButton.setOnClickListener(v -> saveEditedImage());
        cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void rotateImage() {
        if (editedBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90); // Rotate 90 degrees clockwise
            editedBitmap = Bitmap.createBitmap(editedBitmap, 0, 0,
                    editedBitmap.getWidth(), editedBitmap.getHeight(), matrix, true);

            // Update the display bitmap
            displayBitmap = editedBitmap.copy(editedBitmap.getConfig(), true);
            imagePreview.setImageBitmap(displayBitmap);

            // Reset crop mode if active
            if (inCropMode) {
                toggleCropMode();
            }
        }
    }

    private void toggleCropMode() {
        inCropMode = !inCropMode;

        if (inCropMode) {
            // Enter crop mode
            Toast.makeText(getContext(), "Drag corners to adjust crop area", Toast.LENGTH_SHORT).show();
            cropButton.setText("Apply");

            // Initialize crop rectangle (default to 80% of the image centered)
            cropRect = new RectF(
                    editedBitmap.getWidth() * 0.1f,
                    editedBitmap.getHeight() * 0.1f,
                    editedBitmap.getWidth() * 0.9f,
                    editedBitmap.getHeight() * 0.9f
            );

            // Set touch listener for crop adjustments
            imagePreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return handleCropTouch(event);
                }
            });

            // Display the crop overlay
            updateCropOverlay();
        } else {
            // Exit crop mode
            cropButton.setText("Crop");
            imagePreview.setOnTouchListener(null);

            if (cropRect != null) {
                applyCrop();
            }
        }
    }

    private boolean handleCropTouch(MotionEvent event) {
        // Convert touch coordinates to bitmap coordinates
        float[] touchPoint = convertTouchPointToBitmapCoordinates(event.getX(), event.getY());
        float touchX = touchPoint[0];
        float touchY = touchPoint[1];

        // Add to handleCropTouch at ACTION_DOWN
        Log.d("ImageEditor", "Touch at bitmap coordinates: " + touchX + ", " + touchY);
        Log.d("ImageEditor", "Current crop rect: " + cropRect.toString());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if the user is touching near a corner
                currentCorner = findTouchedCorner(touchX, touchY);
                lastTouchX = touchX;
                lastTouchY = touchY;
                return currentCorner != -1; // return true if we're touching a corner

            case MotionEvent.ACTION_MOVE:
                if (currentCorner != -1) {
                    // Calculate the delta movement
                    float deltaX = touchX - lastTouchX;
                    float deltaY = touchY - lastTouchY;

                    // Update the appropriate corner
                    updateCropRect(currentCorner, deltaX, deltaY);

                    // Save the current position for the next move event
                    lastTouchX = touchX;
                    lastTouchY = touchY;

                    // Update the display with the new crop rectangle
                    updateCropOverlay();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                currentCorner = -1; // Reset the selected corner
                break;
        }

        return false;
    }

    private float[] convertTouchPointToBitmapCoordinates(float touchX, float touchY) {
        // Get the image matrix and the f values
        Matrix matrix = new Matrix();
        imagePreview.getImageMatrix().invert(matrix);

        // Map touch point to bitmap point
        float[] point = new float[] {touchX, touchY};

        // Account for ImageView padding
        point[0] -= imagePreview.getPaddingLeft();
        point[1] -= imagePreview.getPaddingTop();

        // Apply the inverted matrix transformation
        matrix.mapPoints(point);

        // Log values for debugging
        Log.d("ImageEditor", "Touch view coordinates: " + touchX + ", " + touchY);
        Log.d("ImageEditor", "Converted to bitmap coordinates: " + point[0] + ", " + point[1]);

        return point;
    }

    private int findTouchedCorner(float x, float y) {
        // Check if touch is near any of the corners of the crop rect
        if (isNearPoint(x, y, cropRect.left, cropRect.top)) {
            Log.d("ImageEditor", "Touch near top-left corner");
            return 0; // top-left
        } else if (isNearPoint(x, y, cropRect.right, cropRect.top)) {
            Log.d("ImageEditor", "Touch near top-right corner");
            return 1; // top-right
        } else if (isNearPoint(x, y, cropRect.left, cropRect.bottom)) {
            Log.d("ImageEditor", "Touch near bottom-left corner");
            return 2; // bottom-left
        } else if (isNearPoint(x, y, cropRect.right, cropRect.bottom)) {
            Log.d("ImageEditor", "Touch near bottom-right corner");
            return 3; // bottom-right
        }
        return -1; // Not near any corner
    }

    private boolean isNearPoint(float x, float y, float pointX, float pointY) {
        float distance = (float) Math.sqrt(
                Math.pow(x - pointX, 2) + Math.pow(y - pointY, 2));
        return distance < CORNER_TOUCH_THRESHOLD;
    }

    private void updateCropRect(int corner, float deltaX, float deltaY) {
        switch (corner) {
            case 0: // top-left
                cropRect.left = Math.max(0, Math.min(cropRect.right - 100, cropRect.left + deltaX));
                cropRect.top = Math.max(0, Math.min(cropRect.bottom - 100, cropRect.top + deltaY));
                break;
            case 1: // top-right
                cropRect.right = Math.max(cropRect.left + 100, Math.min(editedBitmap.getWidth(), cropRect.right + deltaX));
                cropRect.top = Math.max(0, Math.min(cropRect.bottom - 100, cropRect.top + deltaY));
                break;
            case 2: // bottom-left
                cropRect.left = Math.max(0, Math.min(cropRect.right - 100, cropRect.left + deltaX));
                cropRect.bottom = Math.max(cropRect.top + 100, Math.min(editedBitmap.getHeight(), cropRect.bottom + deltaY));
                break;
            case 3: // bottom-right
                cropRect.right = Math.max(cropRect.left + 100, Math.min(editedBitmap.getWidth(), cropRect.right + deltaX));
                cropRect.bottom = Math.max(cropRect.top + 100, Math.min(editedBitmap.getHeight(), cropRect.bottom + deltaY));
                break;
        }
    }

    private void updateCropOverlay() {
        if (editedBitmap == null || cropRect == null) return;

        // Create a copy of the edited bitmap
        displayBitmap = editedBitmap.copy(editedBitmap.getConfig(), true);

        // Create a canvas to draw on the displayBitmap
        Canvas canvas = new Canvas(displayBitmap);

        // Draw semi-transparent overlay outside the crop area
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#80000000")); // semi-transparent black

        // Draw the four regions outside the crop rect
        canvas.drawRect(0, 0, displayBitmap.getWidth(), cropRect.top, overlayPaint); // top
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint); // left
        canvas.drawRect(cropRect.right, cropRect.top, displayBitmap.getWidth(), cropRect.bottom, overlayPaint); // right
        canvas.drawRect(0, cropRect.bottom, displayBitmap.getWidth(), displayBitmap.getHeight(), overlayPaint); // bottom

        // Draw the crop rectangle border
        canvas.drawRect(cropRect, cropPaint);

        // Draw corner handles
        float cornerSize = 10;
        Paint cornerPaint = new Paint();
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.FILL);

        // Top-left corner
        canvas.drawCircle(cropRect.left, cropRect.top, cornerSize, cornerPaint);
        Log.d("ImageEditor", "Drawing top-left corner: " + cropRect.left + ", " + cropRect.top);
        // Top-right corner
        canvas.drawCircle(cropRect.right, cropRect.top, cornerSize, cornerPaint);
        // Bottom-left corner
        canvas.drawCircle(cropRect.left, cropRect.bottom, cornerSize, cornerPaint);
        // Bottom-right corner
        canvas.drawCircle(cropRect.right, cropRect.bottom, cornerSize, cornerPaint);

        // Update the ImageView
        imagePreview.setImageBitmap(displayBitmap);
    }

    // Alternative cropping method
    private void applyCrop() {
        try {
            // Calculate crop dimensions
            int left = Math.max(0, Math.round(cropRect.left));
            int top = Math.max(0, Math.round(cropRect.top));
            int width = Math.min(editedBitmap.getWidth() - left, Math.round(cropRect.width()));
            int height = Math.min(editedBitmap.getHeight() - top, Math.round(cropRect.height()));

            if (width <= 0 || height <= 0) {
                Toast.makeText(getContext(), "Invalid crop area", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a new bitmap with the cropped dimensions
            Bitmap croppedBitmap = Bitmap.createBitmap(width, height, editedBitmap.getConfig());

            // Draw the portion of the original bitmap onto the new one
            Canvas canvas = new Canvas(croppedBitmap);
            Rect srcRect = new Rect(left, top, left + width, top + height);
            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(editedBitmap, srcRect, dstRect, null);

            // Update the edited bitmap
            editedBitmap = croppedBitmap;
            Log.d("ImageEditor", "Edited bitmap dimensions: width=" + editedBitmap.getWidth() + ", height=" + editedBitmap.getHeight());
            displayBitmap = editedBitmap.copy(editedBitmap.getConfig(), true);
            imagePreview.setImageBitmap(displayBitmap);

            Toast.makeText(getContext(), "Image cropped", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ImageEditor", "Error in alternative cropping", e);
            Toast.makeText(getContext(), "Crop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEditedImage() {
        if (editedBitmap == null) {
            Toast.makeText(getContext(), "No edits to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the edited image to storage
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "edited_" + new File(imageUri.getPath()).getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri newImageUri = requireActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (newImageUri != null) {
            try (OutputStream outputStream = requireActivity().getContentResolver().openOutputStream(newImageUri)) {
                if (outputStream != null) {
                    editedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    Toast.makeText(getContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();

                    // Notify that we need to refresh the gallery
                    Bundle result = new Bundle();
                    result.putBoolean("refresh_images", true);
                    getParentFragmentManager().setFragmentResult("image_editor_closed", result);

                    // Close the editor
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Destroy current fragment and goes back to main
    @Override
    public void onDestroyView() {
        Bundle args = new Bundle();
        super.onDestroyView();

        if (getActivity() != null) {
            args.putBoolean("refresh_images", true);
            getParentFragmentManager().setFragmentResult("image_editor_closed", args);
        }
    }
}