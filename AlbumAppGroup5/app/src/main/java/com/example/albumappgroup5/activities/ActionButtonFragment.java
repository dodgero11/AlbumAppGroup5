package com.example.albumappgroup5.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.albumappgroup5.R;

public class ActionButtonFragment extends Fragment {
    MainActivity main;

    public static ActionButtonFragment newInstance() {
        return new ActionButtonFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        main = (MainActivity) getActivity();
        if (main == null)
            throw new IllegalStateException("Fragment not attached to any activity");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_action_buttons, null);

        // bind buttons
        final Button btnOpenAlbum = view.findViewById(R.id.btnOpenAlbum);
        final Button btnOpenCamera = view.findViewById(R.id.btnCaptureImage);
        final Button btnReturnHome = view.findViewById(R.id.btnReturnHome);
        final Button btnSearchImage = view.findViewById(R.id.btnSearchImage);


        btnOpenAlbum.setOnClickListener(v -> {
            main.receiveMessage("OPEN ALBUM");
        });
        btnOpenCamera.setOnClickListener(v -> {
            main.receiveMessage("TAKE PHOTO");
        });
        btnReturnHome.setOnClickListener(v -> {
            main.receiveMessage("RETURN HOME");
        });
        btnSearchImage.setOnClickListener(v -> {
            main.receiveMessage("SEARCH IMAGE");
        });

        return view;
    }
}
