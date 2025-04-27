package com.example.smartnoisemonitor;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoisePredictionActivity extends AppCompatActivity {

    private GraphView graphView;
    private TextView predictionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noise_prediction);

        graphView = findViewById(R.id.noiseGraph);
        predictionText = findViewById(R.id.predictionText);

        drawFakePrediction();
    }

    private void drawFakePrediction() {
        List<DataPoint> points = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            double noise = 60 + Math.random() * 30; // random noise between 60-90 dB
            points.add(new DataPoint(i, noise));
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points.toArray(new DataPoint[0]));
        series.setThickness(8);
        series.setDrawBackground(true);
        series.setBackgroundColor(getResources().getColor(R.color.teal_200));
        graphView.addSeries(series);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(120);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(5);

        predictionText.setText(String.format(Locale.getDefault(), "Next 5 hour Noise Prediction"));
    }
}
