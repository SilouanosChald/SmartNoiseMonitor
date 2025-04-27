package com.example.smartnoisemonitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.clustering.ClusterManager;
import com.google.firebase.database.*;

import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.IOException;
import java.util.*;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference firebaseRef;
    private ClusterManager<MyItem> clusterManager;

    private final List<WeightedLatLng> weightedPoints = new ArrayList<>();
    private final Map<LatLng, String> quietSpots = new HashMap<>();
    private final Map<LatLng, String> noisySpots = new HashMap<>();

    private TileOverlay heatmapOverlay;
    private HeatmapTileProvider heatmapProvider;
    private Location currentLocation;
    private static final int LOCATION_PERMISSION_CODE = 1001;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseRef = FirebaseDatabase.getInstance().getReference("noise_logs");
        geocoder = new Geocoder(this, Locale.getDefault());

        FloatingActionButton quietFab = findViewById(R.id.showQuietFab);
        FloatingActionButton noisyFab = findViewById(R.id.showNoisyFab);
        FloatingActionButton resetFab = findViewById(R.id.resetMapFab);

        quietFab.setOnClickListener(v -> showSpotsBottomSheet(true));
        noisyFab.setOnClickListener(v -> showSpotsBottomSheet(false));
        resetFab.setOnClickListener(v -> reloadHeatmap());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        applyMapStyle();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationAndCenter();
        }
        setupClusterManager();
        loadHeatmapData();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void enableLocationAndCenter() {
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 12f));
            }
        });
    }

    private void setupClusterManager() {
        clusterManager = new ClusterManager<>(this, mMap);
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
    }

    private void loadHeatmapData() {
        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                weightedPoints.clear();
                quietSpots.clear();
                noisySpots.clear();
                clusterManager.clearItems();

                Map<LatLng, List<Double>> noiseData = new HashMap<>();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot logSnap : userSnap.getChildren()) {
                        Double lat = logSnap.child("latitude").getValue(Double.class);
                        Double lon = logSnap.child("longitude").getValue(Double.class);
                        Double db = logSnap.child("decibel").getValue(Double.class);

                        if (lat != null && lon != null && db != null) {
                            LatLng loc = new LatLng(lat, lon);
                            noiseData.computeIfAbsent(loc, k -> new ArrayList<>()).add(db);
                        }
                    }
                }

                updateHeatmap();
                detectSpots(noiseData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reloadHeatmap() {
        if (heatmapOverlay != null) heatmapOverlay.remove();
        mMap.clear();
        setupClusterManager();
        loadHeatmapData();
    }

    private void updateHeatmap() {
        if (!weightedPoints.isEmpty()) {
            int[] colors = { 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
            float[] startPoints = { 0.2f, 0.5f, 1.0f };
            Gradient gradient = new Gradient(colors, startPoints);

            heatmapProvider = new HeatmapTileProvider.Builder()
                    .weightedData(weightedPoints)
                    .radius(50)
                    .gradient(gradient)
                    .build();

            heatmapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
        }
    }

    private void detectSpots(Map<LatLng, List<Double>> noiseData) {
        for (Map.Entry<LatLng, List<Double>> entry : noiseData.entrySet()) {
            double avgDb = 0;
            for (double db : entry.getValue()) avgDb += db;
            avgDb /= entry.getValue().size();

            String name = getPlaceName(entry.getKey());

            if (avgDb < 60.0) {
                quietSpots.put(entry.getKey(), name);
                clusterManager.addItem(new MyItem(entry.getKey(), name));
            } else if (avgDb >= 80.0) {
                noisySpots.put(entry.getKey(), name);
                clusterManager.addItem(new MyItem(entry.getKey(), name));
            }
        }
        clusterManager.cluster();
    }

    private String getPlaceName(LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                if (addr.getFeatureName() != null) return addr.getFeatureName();
                else if (addr.getThoroughfare() != null) return addr.getThoroughfare();
                else return addr.getLocality() != null ? addr.getLocality() : "Unknown";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private void showSpotsBottomSheet(boolean isQuiet) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_spots, null);

        TextView sheetTitle = sheetView.findViewById(R.id.sheet_title);
        LinearLayout spotsContainer = sheetView.findViewById(R.id.spots_list_container);

        Map<LatLng, String> spots = isQuiet ? quietSpots : noisySpots;

        sheetTitle.setText(isQuiet ? "🧘 Quiet Zones" : "🚨 Noisy Zones");
        spotsContainer.removeAllViews();

        Set<String> addedPlaces = new HashSet<>();

        for (Map.Entry<LatLng, String> entry : spots.entrySet()) {
            if (addedPlaces.contains(entry.getValue())) continue; // Only add unique names
            addedPlaces.add(entry.getValue());

            TextView spotView = new TextView(this);
            double distance = calculateDistance(entry.getKey());
            spotView.setText(String.format(Locale.getDefault(), "📍 %s (%.0fm)", entry.getValue(), distance));
            spotView.setTextSize(16f);
            spotView.setPadding(16, 16, 16, 16);
            spotView.setClickable(true);
            spotView.setTextColor(getThemeTextColor());
            spotView.setOnClickListener(v -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(entry.getKey(), 17f));
                bottomSheetDialog.dismiss();
            });
            spotsContainer.addView(spotView);
        }

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private double calculateDistance(LatLng target) {
        if (currentLocation == null) return 0;
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                target.latitude, target.longitude, results);
        return results[0];
    }

    private int getThemeTextColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                ? getResources().getColor(android.R.color.white)
                : getResources().getColor(android.R.color.black);
    }

    private void applyMapStyle() {
        try {
            boolean nightMode = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            int styleRes = nightMode ? R.raw.map_style_dark : R.raw.map_style_light;

            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, styleRes));

            if (!success) {
                Toast.makeText(this, "Map style parsing failed.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationAndCenter();
            } else {
                Toast.makeText(this, "Location permission needed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
