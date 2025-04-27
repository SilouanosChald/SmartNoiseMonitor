package com.example.smartnoisemonitor;

public class NoiseLog {
    private String timestamp;
    private double decibel;
    private double latitude;
    private double longitude;
    private String detectedSound; // NEW FIELD

    public NoiseLog() {
        // Required for Firebase deserialization
    }

    public NoiseLog(String timestamp, double decibel, double latitude, double longitude, String detectedSound) {
        this.timestamp = timestamp;
        this.decibel = decibel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.detectedSound = detectedSound;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getDecibel() {
        return decibel;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getDetectedSound() {
        return detectedSound;
    }
}
