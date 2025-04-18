package com.example.albumappgroup5.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.ImageDetailsObject;

public class PasswordDialogFragment extends DialogFragment {
    private EditText passwordInput;
    private Button submitButton;
    private Button cancelButton;
    private String imageID;
    private DatabaseHandler database;

    public static PasswordDialogFragment newInstance(String imageID) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString("imageID", imageID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageID = getArguments().getString("imageID");
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

        builder.setView(view)
                .setTitle("Enter Password");

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String enteredPassword = passwordInput.getText().toString();
            verifyPassword(enteredPassword);
        });

        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void verifyPassword(String enteredPassword) {
        // Get image from database
        ImageDetailsObject image = database.getImageDetails(imageID);

        if (image != null) {
            // Check if password matches (You need to implement password checking logic)
            boolean isPasswordCorrect;

            if (enteredPassword.equals(database.getImagePassword(imageID))) {
                isPasswordCorrect = true;
            } else {
                isPasswordCorrect = false;
            }

            // Send result back to calling fragment
            Bundle result = new Bundle();
            result.putBoolean("password_correct", isPasswordCorrect);
            getParentFragmentManager().setFragmentResult("password_result", result);

            if (isPasswordCorrect) {
                dismiss();
            } else {
                Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
            }
        } else {
            // No password set or image not found
            Bundle result = new Bundle();
            result.putBoolean("password_correct", true);
            getParentFragmentManager().setFragmentResult("password_result", result);
            dismiss();
        }
    }
}