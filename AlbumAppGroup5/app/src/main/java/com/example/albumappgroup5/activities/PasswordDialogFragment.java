package com.example.albumappgroup5.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.ImageDetailsObject;

public class PasswordDialogFragment extends DialogFragment {
    private EditText passwordInput;
    private Button submitButton;
    private Button cancelButton;
    private String imageID;
    private int albumID = -1;
    private boolean isAlbum = false;
    private DatabaseHandler database;

    // For image password check
    public static PasswordDialogFragment newInstance(String imageID) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString("imageID", imageID);
        args.putBoolean("isAlbum", false);
        fragment.setArguments(args);
        return fragment;
    }

    // For album password check
    public static PasswordDialogFragment newInstance(int albumID) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putInt("albumID", albumID);
        args.putBoolean("isAlbum", true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isAlbum = getArguments().getBoolean("isAlbum", false);
            if (isAlbum) {
                albumID = getArguments().getInt("albumID", -1);
            } else {
                imageID = getArguments().getString("imageID");
            }
        }
        database = DatabaseHandler.getInstance(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.password_dialog, null);
        passwordInput = view.findViewById(R.id.passwordInput);
        submitButton = view.findViewById(R.id.submitButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        String title = isAlbum ? "Enter Album Password" : "Enter Image Password";
        builder.setView(view)
                .setTitle(title);

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String enteredPassword = passwordInput.getText().toString();
            verifyPassword(enteredPassword);
        });

        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void verifyPassword(String enteredPassword) {
        boolean isPasswordCorrect = false;

        if (isAlbum) {
            // Album password verification
            if (albumID != -1) {
                String albumPassword = database.getAlbumPassword(albumID);
                isPasswordCorrect = enteredPassword.equals(albumPassword);
            }
        } else {
            // Image password verification
            ImageDetailsObject image = database.getImageDetails(imageID);
            if (image != null) {
                String imagePassword = database.getImagePassword(imageID);
                isPasswordCorrect = enteredPassword.equals(imagePassword);
            }
        }

        // Send result back to calling fragment
        Bundle result = new Bundle();
        result.putBoolean("password_correct", isPasswordCorrect);

        // Include information about what was checked
        result.putBoolean("isAlbum", isAlbum);
        if (isAlbum) {
            result.putInt("albumID", albumID);
        } else {
            result.putString("imageID", imageID);
        }
        getParentFragmentManager().setFragmentResult("password_result", result);

        if (isPasswordCorrect) {
            dismiss();
        } else {
            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
        }
    }
}