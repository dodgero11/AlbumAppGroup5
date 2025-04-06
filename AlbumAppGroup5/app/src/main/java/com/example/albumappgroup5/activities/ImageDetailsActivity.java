package com.example.albumappgroup5.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.ImageDetailsObject;

public class ImageDetailsActivity extends Activity {
    String imageID;

    // view variables
    EditText nameEdit, descriptionEdit, locationEdit;
    AutoCompleteTextView addTagEdit;
    TextView dateAdded;
    ListView tagsList;
    Button tagAddButton, updateDetailsButton;

    // database object
    DatabaseHandler database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get imageID for this details activity
        Intent caller = getIntent();
        imageID = caller.getStringExtra("imageID");
        if (imageID == null) // no imageID passed to activity somehow, end activity
        {
            Log.e("activity error", "no imageID for image details activity");
            Toast.makeText(this, "Error showing image details", Toast.LENGTH_LONG
            ).show();
            finish();
        }

        // get database handler
        database = DatabaseHandler.getInstance(this);
        if (database == null)
        {
            Log.e("database error", "cannot access database");
            Toast.makeText(this, "Error showing image details", Toast.LENGTH_LONG
            ).show();
            finish();
        }

        setContentView(R.layout.image_details);
        // bind views
        nameEdit = findViewById(R.id.nameEdit);
        descriptionEdit = findViewById(R.id.descriptionEdit);
        locationEdit = findViewById(R.id.locationEdit);
        addTagEdit = findViewById(R.id.addTagEdit);
        dateAdded = findViewById(R.id.dateAdded);
        tagsList = findViewById(R.id.tagsList);
        tagAddButton = findViewById(R.id.tagAddButton);
        updateDetailsButton = findViewById(R.id.updateDetailsButton);

        ImageDetailsObject details = database.getImageDetails(imageID);
        nameEdit.setText(details.getImageName());
        descriptionEdit.setText(details.getDescription());
        locationEdit.setText(details.getLocation());
    }
}
