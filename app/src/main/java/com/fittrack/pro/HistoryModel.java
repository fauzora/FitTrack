package com.fittrack.pro;

import java.util.Date;

public class HistoryModel {
    private String id;
    private double bmi;
    private String phase;
    private String dateStr;
    private String userId;

    public HistoryModel() {
        // Required empty constructor for Firestore
    }

    public HistoryModel(double bmi, String phase, String dateStr, String userId) {
        this.bmi = bmi;
        this.phase = phase;
        this.dateStr = dateStr;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
