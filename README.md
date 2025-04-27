# Smart Noise Monitor

A mobile application designed for real-time environmental noise monitoring, sound classification, and interactive community-driven noise visualization.

## Overview

Smart Noise Monitor is an Android application that continuously tracks ambient sound levels, classifies sounds using machine learning techniques, and provides visual insights through interactive maps and community-driven data. Users can easily discover quiet and noisy areas around them, making informed decisions based on real-time noise analysis.

---

## Features

### Real-Time Noise Level Monitoring

Smart Noise Monitor utilizes Android's `AudioRecord` API to capture ambient sound continuously. It calculates the Root Mean Square (RMS) of audio samples to estimate noise levels accurately in decibels (dB), visualized through a dynamic speedometer UI.

### Sound Classification with TensorFlow Lite

The app integrates TensorFlow Lite's pre-trained YAMNet model to categorize incoming audio in real-time. Common classifications include speech, animals, music, vehicles, and more, enhancing user understanding of their acoustic surroundings.

### Interactive Community Noise Heatmaps

Leveraging Firebase Realtime Database and Google Maps API, Smart Noise Monitor aggregates user-generated noise data to produce interactive heatmaps. These heatmaps visually represent areas of varying noise intensity, helping users identify and navigate quieter zones in their communities.

### Quiet and Noisy Zone Recommendations

The app automatically identifies areas with consistently low or high noise levels, providing users with curated lists accessible via intuitive UI components. Locations include readable addresses and calculated distances from the user's current position, facilitating easy navigation.

### Dynamic Map Styling

Smart Noise Monitor dynamically adjusts its map styling based on the user's device theme (dark/light). Customized map themes improve readability and reduce visual fatigue, offering an optimal user experience under varying lighting conditions.

### Background Noise Monitoring

The application employs a foreground service to continue monitoring environmental noise even when minimized or closed, alerting users through persistent notifications if set noise thresholds are exceeded.

---

## Technologies Used

- Android SDK
- Java
- Firebase Realtime Database
- Google Maps SDK for Android
- TensorFlow Lite (YAMNet Audio Classification)
- Material Design Components

---

## Installation & Setup

Follow these instructions to get the app running locally on your device or emulator:

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/smart-noise-monitor.git
