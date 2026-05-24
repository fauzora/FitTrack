package com.fittrack.pro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.fittrack.pro.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private String selectedDate = "";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupSpinner();

        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnCalculate.setOnClickListener(v -> validateAndCalculate());
    }

    private void setupSpinner() {
        String[] phases = {"Hypertrophy", "Cutting", "Maintenance"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, phases);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPhase.setAdapter(adapter);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year1;
                    binding.btnPickDate.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void validateAndCalculate() {
        if (binding.etWeight.getText() == null || binding.etHeight.getText() == null) return;
        String weightStr = binding.etWeight.getText().toString();
        String heightStr = binding.etHeight.getText().toString();

        if (weightStr.isEmpty() || heightStr.isEmpty() || selectedDate.isEmpty()) {
            showErrorDialog();
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double height = Double.parseDouble(heightStr) / 100; // convert cm to m
        double bmi = weight / (height * height);

        String phase = binding.spinnerPhase.getSelectedItem().toString();
        Log.d(TAG, "Calculated BMI: " + bmi + " for phase: " + phase);

        saveToFirestore(bmi, phase);
    }

    private void saveToFirestore(double bmi, String phase) {
        String userId = mAuth.getUid();
        if (userId == null) userId = "test_user_id";

        HistoryModel history = new HistoryModel(bmi, phase, selectedDate, userId);

        // Save locally using SharedPreferences as a fallback/primary storage
        saveLocally(history);

        Log.d(TAG, "Proceeding to result screen");
        navigateToResult(bmi, phase);

        // Attempt Firestore save in background
        db.collection("history")
                .add(history)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Sync to Firestore success"))
                .addOnFailureListener(e -> Log.w(TAG, "Firestore sync failed - using local only"));
    }

    private void saveLocally(HistoryModel model) {
        android.content.SharedPreferences prefs = getSharedPreferences("fit_track_prefs", MODE_PRIVATE);
        String currentHistory = prefs.getString("history_json", "[]");
        try {
            org.json.JSONArray array = new org.json.JSONArray(currentHistory);
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("bmi", model.getBmi());
            obj.put("phase", model.getPhase());
            obj.put("dateStr", model.getDateStr());
            obj.put("userId", model.getUserId());
            array.put(obj);
            prefs.edit().putString("history_json", array.toString()).apply();
            Log.d(TAG, "Saved locally: " + array.length() + " items");
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error saving local history", e);
        }
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.validation_error)
                .setMessage(R.string.please_fill_all)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void navigateToResult(double bmi, String phase) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("BMI_VALUE", bmi);
        intent.putExtra("PHASE", phase);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reset) {
            resetForm();
            return true;
        } else if (item.getItemId() == R.id.action_clear) {
            clearLocalHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetForm() {
        binding.etWeight.setText("");
        binding.etHeight.setText("");
        binding.btnPickDate.setText(R.string.select_date);
        selectedDate = "";
    }

    private void clearLocalHistory() {
        getSharedPreferences("fit_track_prefs", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
    }
}
