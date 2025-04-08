package com.example.albumappgroup5.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.adapters.GalleryAdapter;
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.ImageModel;
import java.util.ArrayList;
import java.util.List;

public class ImageSearchFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;

    // Full list of images loaded from your source.
    private List<ImageDetailsObject> allImages;
    // Filtered images as query changes.
    private List<ImageDetailsObject> filteredImages;
    private DatabaseHandler database;

    public static ImageSearchFragment newInstance() {
        ImageSearchFragment fragment = new ImageSearchFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = DatabaseHandler.getInstance(getContext());

        // Retrieve image list from arguments
        if (getArguments() != null) {
            allImages = getArguments().getParcelableArrayList("images");
        }
        if (allImages == null) {
            allImages = new ArrayList<>();
        }
        // Initially, filtered list is the same as the full list
        filteredImages = new ArrayList<>(allImages);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_image, container, false);

        searchView = view.findViewById(R.id.searchViewImages);
        recyclerView = view.findViewById(R.id.recyclerViewSearchResults);

        // Initialize RecyclerView layout
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Initialize your adapter with filteredImages
        adapter = new GalleryAdapter(getContext(), filteredImages, /* your click listener */ null);
        recyclerView.setAdapter(adapter);

        // Load all images
        filteredImages.addAll(database.getAllImages());
        adapter.notifyDataSetChanged();

        // Setup SearchView query change listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Optionally hide the keyboard or perform final search here
                filterImages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterImages(newText);
                return true;
            }
        });

        return view;
    }

    /**
     * Filters allImages list based on the query string.
     * Adjust the filtering criteria based on your image model (e.g., name, description, etc.)
     */
    private void filterImages(String query) {
        filteredImages.clear();
        List<String> imageIDs = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            // Show full list if query is empty
            filteredImages.addAll(database.getAllImages());
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<String> tagNames = database.getTagNames();

            for (String tagName : tagNames) {
                if (tagName.toLowerCase().contains(lowerCaseQuery)) {
                    imageIDs.addAll(database.getImagesByTag(tagName));
                }
            }
        }
        for (String imageID : imageIDs) {
            filteredImages.add(database.getImageDetails(imageID));
        }
        adapter.notifyDataSetChanged();
    }
}
