package com.fittrack.pro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fittrack.pro.databinding.ActivityResultBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private ActivityResultBinding binding;
    private HistoryAdapter adapter;
    private List<HistoryModel> historyList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        double bmi = getIntent().getDoubleExtra("BMI_VALUE", 0.0);
        String phase = getIntent().getStringExtra("PHASE");

        displayResult(bmi, phase);
        setupRecyclerView();
        fetchHistory();

        binding.fabShare.setOnClickListener(v -> shareBmi(bmi));
    }

    private void displayResult(double bmi, String phase) {
        binding.tvBmiValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));
        binding.tvBmiCategory.setText(getBmiCategory(bmi));
        binding.tvPhase.setText("Phase: " + phase);
    }

    private String getBmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        else if (bmi < 25) return "Normal";
        else if (bmi < 30) return "Overweight";
        else return "Obese";
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
    }

    private void fetchHistory() {
        String userId = mAuth.getUid();
        if (userId == null) userId = "test_user_id";

        // Load local history first
        loadLocalHistory();

        // Attempt to fetch from Firestore to sync
        db.collection("history")
                .whereEqualTo("userId", userId)
                .orderBy("dateStr", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        historyList.clear();
                        historyList.addAll(queryDocumentSnapshots.toObjects(HistoryModel.class));
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "History synced from Firestore: " + historyList.size());
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Cloud sync failed, showing local data only"));
    }

    private void loadLocalHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences("fit_track_prefs", MODE_PRIVATE);
        String historyJson = prefs.getString("history_json", "[]");
        try {
            org.json.JSONArray array = new org.json.JSONArray(historyJson);
            historyList.clear();
            for (int i = array.length() - 1; i >= 0; i--) { // Show latest first
                org.json.JSONObject obj = array.getJSONObject(i);
                historyList.add(new HistoryModel(
                        obj.getDouble("bmi"),
                        obj.getString("phase"),
                        obj.getString("dateStr"),
                        obj.getString("userId")
                ));
            }
            adapter.notifyDataSetChanged();
            Log.d(TAG, "Local history loaded: " + historyList.size());
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error loading local history", e);
        }
    }

    private void shareBmi(double bmi) {
        String message = "My current BMI is " + String.format(Locale.getDefault(), "%.1f", bmi) + ". Tracking my progress with FitTrack!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share BMI via"));
    }
}
