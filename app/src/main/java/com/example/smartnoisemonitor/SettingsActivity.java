package com.example.smartnoisemonitor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private EditText thresholdInput;
    private EditText intervalInput;
    private Button saveButton;
    private SwitchCompat themeSwitch;
    private SwitchCompat monitoringSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_theme", true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        thresholdInput = findViewById(R.id.thresholdInput);
        intervalInput = findViewById(R.id.intervalInput);
        saveButton = findViewById(R.id.saveSettingsButton);
         themeSwitch = findViewById(R.id.themeSwitch);


        // Set current state of the switch
        themeSwitch.setChecked(isDark);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_theme", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
        monitoringSwitch = findViewById(R.id.monitoringSwitch);

// Load previous monitoring state
        boolean monitoringActive = prefs.getBoolean("monitoring_active", true);
        monitoringSwitch.setChecked(monitoringActive);

// Save monitoring state when user changes it
        monitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("monitoring_active", isChecked).apply();
        });

        // Load stored noise settings
        loadSettings();

        saveButton.setOnClickListener(v -> {
            try {
                float threshold = Float.parseFloat(thresholdInput.getText().toString());
                int interval = Integer.parseInt(intervalInput.getText().toString());

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("threshold", threshold);
                editor.putInt("interval", interval);
                editor.apply();

                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        float threshold = prefs.getFloat("threshold", 85f);
        int interval = prefs.getInt("interval", 1000);

        thresholdInput.setText(String.valueOf(threshold));
        intervalInput.setText(String.valueOf(interval));
    }
}
