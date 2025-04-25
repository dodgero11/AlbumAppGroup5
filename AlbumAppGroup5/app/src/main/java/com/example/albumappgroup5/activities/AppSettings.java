package com.example.albumappgroup5.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.albumappgroup5.R;

public class AppSettings extends AppCompatActivity {
    Spinner spinnerAppearance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_settings);

        // bind views
        spinnerAppearance = findViewById(R.id.spinnerAppearance);
        final Toolbar settingsToolbar = findViewById(R.id.settingsToolbar);
        final Button settingsSaveButton = findViewById(R.id.settingsSaveButton);

        // spinner items
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.appearanceOptions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAppearance.setAdapter(adapter);

        // toolbar behavior
        settingsToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AppSettings.this)
                        .setTitle("Exit settings")
                        .setMessage("Save your changes?")
                        .setPositiveButton("Save and exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveSettings();
                                setResult(RESULT_OK);
                                finish();
                            }
                        })
                        .setNegativeButton("Exit without saving", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        })
                        .show();
            }
        });

        // save button
        settingsSaveButton.setOnClickListener(v -> {
            saveSettings();
            setResult(RESULT_OK);
            finish();
        });

        loadSettings(); // get current settings to set selected items
    }

    private void saveSettings () {
        SharedPreferences preferences = getSharedPreferences(Global.SETTINGS, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Global.SETTINGS_NIGHT, spinnerAppearance.getSelectedItemPosition() == 1);
        editor.apply();
        Toast.makeText(this, "Settings changed", Toast.LENGTH_LONG).show();
    }

    public void loadSettings () {
        SharedPreferences preferences = getSharedPreferences(Global.SETTINGS, Activity.MODE_PRIVATE);
        if (preferences == null)
            return;

        if (preferences.getBoolean(Global.SETTINGS_NIGHT, false)) {
            spinnerAppearance.setSelection(1);
        }
        else {
            spinnerAppearance.setSelection(0);
        }
    }
}
