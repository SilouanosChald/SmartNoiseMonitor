package com.example.smartnoisemonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class NoiseMonitorService extends Service {

    private static final String CHANNEL_ID = "NoiseMonitorChannel";
    private static final int NOTIFICATION_ID = 1;

    private AudioRecord audioRecord;
    private Handler handler;
    private Runnable soundRunnable;

    private final int sampleRate = 16000;
    private int bufferSize;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        handler = new Handler();
        startAudioRecording();
    }

    private void startAudioRecording() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); // Permission not granted, stop service safely
            return;
        }

        bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        audioRecord.startRecording();

        soundRunnable = new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[bufferSize];
                int read = audioRecord.read(buffer, 0, buffer.length);

                if (read > 0) {
                    // ✅ You can now calculate decibels or send data to Firebase if needed
                    // Example: calculate average sound level here
                }

                handler.postDelayed(this, 1000); // Measure every second
            }
        };

        handler.post(soundRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Noise Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Noise Monitoring Active")
                .setContentText("Listening to ambient noise...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Restart service automatically if killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (handler != null && soundRunnable != null) {
            handler.removeCallbacks(soundRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
