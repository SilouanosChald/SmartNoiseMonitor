package com.example.smartnoisemonitor;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MyItem implements ClusterItem {
    private final LatLng position;
    private final String title;

    public MyItem(LatLng position, String title) {
        this.position = position;
        this.title = title;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return "";
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }
}
