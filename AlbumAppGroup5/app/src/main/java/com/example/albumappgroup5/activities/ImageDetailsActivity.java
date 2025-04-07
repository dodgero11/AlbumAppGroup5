package com.example.albumappgroup5.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.TagObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageDetailsActivity extends Activity {
    String imageID;

    // view variables
    EditText nameEdit, descriptionEdit, locationEdit;
    AutoCompleteTextView addTagEdit;
    TextView dateAdded;
    GridView tagsList;
    Button tagAddButton, updateDetailsButton;

    // database object
    DatabaseHandler database;

    // data variables from database
    List<TagObject> tagData;

    // lists to handle tag deletion
    List<Integer> deleteMarked = new ArrayList<>();

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
        Date dateDetails = details.getTimeAdded();
        if (dateDetails == null)
            dateAdded.setText(R.string.dateDefault);
        else
            dateAdded.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(dateDetails));

        tagData = database.getImageTagList(imageID);
        ArrayAdapter<String> adapter;
        if (tagData.isEmpty()) {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[]{getResources().getString(R.string.tagDefault)});
        }
        else {
            List<String> tagNameList = new ArrayList<>();
            for (TagObject item : tagData) {
                tagNameList.add(item.getTagName());
            }
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tagNameList) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    try {
                        if (deleteMarked.contains(position))
                            view.setBackgroundColor(0xFFFF0000);
                        else
                            view.setBackgroundColor(0xC0FFFFFF);

                    }
                    catch (Exception e) {
                        Log.e("error", e.toString());
                    }
                    return view;
                }
            };

            tagsList.setOnItemClickListener((parent, view, position, id) -> {
                if (deleteMarked.contains(position)) { // uncheck item
                    deleteMarked.remove((Integer) position);
                    view.setBackgroundColor(0x00FFFFFF);
                }
                else { // check item
                    deleteMarked.add(position);
                    view.setBackgroundColor(0xC0FF0000); //// consider changing to match theme later
                }
            });
        }
        tagsList.setAdapter(adapter);

    }
}
