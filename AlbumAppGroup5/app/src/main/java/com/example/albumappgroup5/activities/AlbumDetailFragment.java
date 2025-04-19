package com.example.albumappgroup5.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.AlbumModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AlbumDetailFragment extends Fragment implements GalleryAdapter.OnImageClickListener {
    static private List<ImageDetailsObject> imagesOfAlbum;
    static private List<ImageDetailsObject> allOfImages;
    static private String nameOfAlbum;
    private TextView albumTitle;
    private GalleryAdapter adapter;
    private RecyclerView recyclerView;
    private DatabaseHandler database;
    public static AlbumDetailFragment newInstance(String albumName, List<ImageDetailsObject> imageList, List<ImageDetailsObject> allImages) {
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
        database = DatabaseHandler.getInstance(getContext());

        albumTitle = view.findViewById(R.id.albumTitle);
        albumTitle.setText(nameOfAlbum);

        recyclerView = view.findViewById(R.id.recyclerViewAlbumImages);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new GalleryAdapter(getContext(), imagesOfAlbum, this);
        recyclerView.setAdapter(adapter);

        // Get images from database
        getImagesFromAlbum();

        // Listen for the result when AddImageFragment is closed
        getParentFragmentManager().setFragmentResultListener("add_image_result", this, (requestKey, result) -> {
            ImageDetailsObject newImage = result.getParcelable("new_image");
            if (newImage != null) {
                imagesOfAlbum.add(newImage);
                database.addToAlbum(newImage.getImageID(), database.getAlbumID(nameOfAlbum));
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

    private void getImagesFromAlbum() {
        try {
            List<String> tempImageList = database.getAlbumImages(database.getAlbumID(nameOfAlbum));
            imagesOfAlbum.clear();
            for (String image : tempImageList) {
                ImageDetailsObject tempObject = new ImageDetailsObject(image);

                // Check for password
                if (!database.getImagePassword(image).isEmpty()) {
                    tempObject.setHasPassword(true);
                    tempObject.setPasswordProtected(true);
                }

                imagesOfAlbum.add(tempObject);
            }
        } catch (Exception e) {
            Log.e("error", e.toString());
        }
    }

    @Override
    public void onImageClick(int position) {
        ImageDetailsObject image = imagesOfAlbum.get(position); // Get the clicked image

        passwordCheckAsync(position)
                .thenAccept(ok -> {
                    requireActivity().runOnUiThread(() -> {
                        if (ok) {
                            openImageLargeFragment(image);
                        } else {
                            Toast.makeText(this.getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    public void openImageLargeFragment(ImageDetailsObject image) {
        ImageLargeFragment imageLargeFragment = ImageLargeFragment.newInstance(
                image.getImageID(),
                "AlbumDetailFragment"
        );

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, imageLargeFragment);
        transaction.addToBackStack("IMAGE_LARGE");
        transaction.commit();

        if (getActivity() != null) {
            getActivity().findViewById(R.id.fragmentContainerBottom).setVisibility(View.GONE);
        }
    }

    @Override
    public void onImageLongClick(int position) {
        passwordCheckAsync(position)
                .thenAccept(ok -> {
                    requireActivity().runOnUiThread(() -> {
                        if (ok) {
                            thumbnailOrRemoval(position);
                        } else {
                            Toast.makeText(this.getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }


    public void thumbnailOrRemoval (int position) {
            new AlertDialog.Builder(getContext())
                .setTitle("Image Options")
                .setItems(new CharSequence[]{"Make as album thumbnail", "Remove from album"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Set as album thumbnail
                            AlbumModel albumModel = new ViewModelProvider(requireActivity()).get(AlbumModel.class);
                            albumModel.setAlbumThumbnail(nameOfAlbum, imagesOfAlbum.get(position).getImageID());
                            saveThumbnail(nameOfAlbum, imagesOfAlbum.get(position).getImageID());
                            break;

                        case 1: // Delete
                            showDeleteConfirmationDialog(position);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void saveThumbnail(String albumName, String thumbnailPath) {
        SharedPreferences sharedPref = getContext().getSharedPreferences("AlbumPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        // Sử dụng key là "thumbnail_<albumName>"
        editor.putString("thumbnail_" + albumName, thumbnailPath);
        editor.apply();
    }

    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    imagesOfAlbum.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Resume current fragment
    @Override
    public void onResume() {
        if (getActivity() != null) {
            getActivity().findViewById(R.id.fragmentContainerBottom).setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    public CompletableFuture<Boolean> passwordCheckAsync(int index) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ImageDetailsObject image = imagesOfAlbum.get(index);

        // if no password, complete immediately
        if (database.getImagePassword(image.getImageID()).isEmpty()) {
            future.complete(true);
            return future;
        }

        // otherwise show the dialog
        PasswordDialogFragment dlg = PasswordDialogFragment.newInstance(image.getImageID());
        dlg.setCancelable(false);
        dlg.show(this.getParentFragmentManager(), "PasswordDialog");

        this.getParentFragmentManager().setFragmentResultListener(
                "password_result", this,
                (requestKey, bundle) ->
                        future.complete(bundle.getBoolean("password_correct", false))
        );

        return future;
    }
}
