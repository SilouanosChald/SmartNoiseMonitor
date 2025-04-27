package com.example.smartnoisemonitor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartnoisemonitor.NoiseMonitorService;

import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.github.anastr.speedviewlib.components.Section;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 999;
    private static final int LOCATION_PERMISSION_CODE = 2000;
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private final Handler handler = new Handler();
    private Runnable soundRunnable;
    private AudioRecord audioRecord;
    private int bufferSize;
    private int sampleRate = 16000; // Default sample rate, will adjust dynamically
    private TextView levelIndicator;

    private double NOISE_THRESHOLD_DB = 85.0;
    private int samplingInterval = 1000;

    private PointerSpeedometer speedometer;
    private TextView soundLabel;

    private FirebaseUser user;
    private DatabaseReference firebaseRef;
    private FusedLocationProviderClient fusedLocationClient;

    private double latitude = 0.0, longitude = 0.0;

    private AudioClassifier audioClassifier;
    private TensorAudio tensorAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        levelIndicator = findViewById(R.id.levelIndicator);
        NotificationHelper.createNotificationChannel(this);
        Button predictionBtn = findViewById(R.id.noisePredictionButton);

        predictionBtn.setOnClickListener(v -> startActivity(new Intent(this, NoisePredictionActivity.class)));

        speedometer = findViewById(R.id.speedometer);
        soundLabel = findViewById(R.id.soundLabel);
        Button logBtn = findViewById(R.id.viewFirebaseBtn);
        Button mapBtn = findViewById(R.id.mapButton);
        Button settingsBtn = findViewById(R.id.settingsButton);

        logBtn.setOnClickListener(v -> startActivity(new Intent(this, FirebaseLogActivity.class)));
        mapBtn.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        firebaseRef = FirebaseDatabase.getInstance().getReference("noise_logs");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadSettings();
        setupSpeedometer();
        initializeAudioClassifier();
        requestAllPermissions();

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean monitoringActive = prefs.getBoolean("monitoring_active", true);

        if (!monitoringActive) {
            stopMonitoring();
        }

    }
    private void stopMonitoring() {
        if (audioRecord != null) {
            audioRecord.stop();
        }
        handler.removeCallbacks(soundRunnable);
    }

    private void startMonitoring() {
        if (audioRecord != null) {
            audioRecord.startRecording();
        }
        monitorSoundLevel();
    }

    private void setupSpeedometer() {
        speedometer.setMaxSpeed(125);
        speedometer.setMinSpeed(0);
        speedometer.setUnit("dB");
        speedometer.setWithTremble(false);
        speedometer.setTickNumber(6);
        speedometer.setSpeedAt(0);
        speedometer.clearSections(); // We’ll set sections dynamically instead
    }


    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        NOISE_THRESHOLD_DB = prefs.getFloat("threshold", 85f);
        samplingInterval = prefs.getInt("interval", 1000);
    }

    private void initializeAudioClassifier() {
        try {
            audioClassifier = AudioClassifier.createFromFile(this, "yamnet_classification.tflite");

            if (audioClassifier == null) {
                Toast.makeText(this, "Failed to load classifier.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dynamically set sample rate based on model
            sampleRate = audioClassifier.getRequiredTensorAudioFormat().getSampleRate();
            tensorAudio = audioClassifier.createInputTensorAudio();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestAllPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.RECORD_AUDIO


        };
        startService(new Intent(this, NoiseMonitorService.class));

        List<String> toRequest = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_CODE);
        } else {
            startAudioRecording();
            getLastKnownLocation();
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                });
    }

    private void startAudioRecording() {
        bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        audioRecord.startRecording();
        monitorSoundLevel();
    }

    private void monitorSoundLevel() {
        soundRunnable = new Runnable() {
            @Override
            public void run() {
                getLastKnownLocation();

                short[] buffer = new short[bufferSize];
                int read = audioRecord.read(buffer, 0, buffer.length);

                if (read <= 0) {
                    handler.postDelayed(this, samplingInterval);
                    return;
                }

                double sum = 0;
                for (short s : buffer) sum += s * s;
                double rms = Math.sqrt(sum / read);
                double decibels = (rms > 0) ? 20 * Math.log10(rms) : 0;

                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                speedometer.speedTo((float) decibels);

                String detectedLabel = "Unknown";

                if (audioClassifier != null && tensorAudio != null) {
                    tensorAudio.load(audioRecord);


                    List<Classifications> results = audioClassifier.classify(tensorAudio);
                    if (!results.isEmpty()) {
                        List<Category> categories = results.get(0).getCategories();
                        if (!categories.isEmpty()) {
                            detectedLabel = categories.get(0).getLabel();
                            final String labelForUI = detectedLabel;

                            runOnUiThread(() -> {
                                soundLabel.setText("Detected: " + labelForUI);
                                soundLabel.setAlpha(0f);
                                soundLabel.animate().alpha(1f).setDuration(500).start();

                                if (labelForUI.toLowerCase().contains("vehicle")) {
                                    soundLabel.setTextColor(Color.parseColor("#FF5722"));
                                } else if (labelForUI.toLowerCase().contains("dog") || labelForUI.toLowerCase().contains("animal")) {
                                    soundLabel.setTextColor(Color.parseColor("#FFC107"));
                                } else if (labelForUI.toLowerCase().contains("speech") || labelForUI.toLowerCase().contains("talk")) {
                                    soundLabel.setTextColor(Color.parseColor("#4CAF50"));
                                } else {
                                    soundLabel.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_blue_light));
                                }

                                // 🔊 Update level indicator
                                if (decibels < 70) {
                                    levelIndicator.setText("Safe");
                                    levelIndicator.setTextColor(Color.GREEN);
                                    speedometer.setPointerColor(Color.GREEN); // or GREEN / YELLOW

                                } else if (decibels < 90) {
                                    levelIndicator.setText("Caution");
                                    levelIndicator.setTextColor(Color.YELLOW);
                                    speedometer.setPointerColor(Color.YELLOW); // or GREEN / YELLOW

                                } else {
                                    levelIndicator.setText("Danger");
                                    levelIndicator.setTextColor(Color.RED);
                                    speedometer.setPointerColor(Color.RED); // or GREEN / YELLOW

                                }
                            });
                        }
                    }
                }

                if (user != null) {
                    NoiseLog log = new NoiseLog(timestamp, decibels, latitude, longitude, detectedLabel);
                    firebaseRef.child(user.getUid()).push().setValue(log);
                }

                if (decibels >= NOISE_THRESHOLD_DB) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED){
                        NotificationHelper.sendNotification(MainActivity.this, decibels);
                    }

                    NotificationHelper.sendNotification(MainActivity.this, decibels);
                }


                handler.postDelayed(this, samplingInterval);
            }
        };

        handler.post(soundRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        handler.removeCallbacks(soundRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE || requestCode == LOCATION_PERMISSION_CODE) {
            requestAllPermissions();
        }
    }
}
