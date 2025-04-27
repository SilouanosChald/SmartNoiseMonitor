package com.example.smartnoisemonitor;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class FirebaseLogActivity extends AppCompatActivity {

    private ListView logListView;
    private ProgressBar progressBar;
    private TextView emptyMessage;

    private final List<NoiseLog> logList = new ArrayList<>();
    private NoiseLogAdapter adapter;
    private DatabaseReference firebaseRef;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_log);

        logListView = findViewById(R.id.logListView);
        progressBar = findViewById(R.id.progressBar);
        emptyMessage = findViewById(R.id.emptyMessage);

        adapter = new NoiseLogAdapter(this, logList);
        logListView.setAdapter(adapter);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseRef = FirebaseDatabase.getInstance()
                .getReference("noise_logs")
                .child(user.getUid());

        loadLogsFromFirebase();
    }

    private void loadLogsFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                logList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    NoiseLog log = snap.getValue(NoiseLog.class);
                    if (log != null) logList.add(log);
                }

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                emptyMessage.setVisibility(logList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FirebaseLogActivity.this, "Failed to load logs", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                emptyMessage.setVisibility(View.VISIBLE);
            }
        });
    }
}
