package com.example.albumappgroup5.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.albumappgroup5.R;

public class ImageOptionsFragment extends Fragment implements OptionsFragmentCallback {
    MainActivity main; // activity this fragment is attached to
    int itemAffected = -1; // index of the item to apply options to

    public static ImageOptionsFragment newInstance() {
//        Bundle args = new Bundle();
//        args.putInt("index", index);

        ImageOptionsFragment fragment = new ImageOptionsFragment();
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        main = (MainActivity) getActivity();
        if (main == null)
        {
            throw new IllegalStateException("Fragment not attached to any activity");
        }
//        Bundle args = getArguments();
//        if (args != null)
//            itemAffected = args.getInt("index", -1);
//        else
//            itemAffected = -1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_image_options, null);

        // view element variables
        LinearLayout detailsOption = view.findViewById(R.id.detailsOption);
        LinearLayout deleteOption = view.findViewById(R.id.deleteOption);
        LinearLayout cancelOption = view.findViewById(R.id.cancelOption);
        LinearLayout setWallpaperOption = view.findViewById(R.id.setWallpaperOption);
        LinearLayout editImageOption = view.findViewById(R.id.editImageOption);

        setWallpaperOption.setOnClickListener(v -> {
            main.selectOption(itemAffected, "setWallpaper");
        });

        detailsOption.setOnClickListener(v -> {
            main.selectOption(itemAffected, "details");
        });
        editImageOption.setOnClickListener(v -> {
            main.selectOption(itemAffected, "edit");
        });
        deleteOption.setOnClickListener(v -> {
            main.selectOption(itemAffected, "delete");
        });
        cancelOption.setOnClickListener(v -> {
            main.selectOption(itemAffected, "cancel");
        });
        return view;
    }

    @Override
    public void changeIndex(int index) {
        itemAffected = index;
    }
}
