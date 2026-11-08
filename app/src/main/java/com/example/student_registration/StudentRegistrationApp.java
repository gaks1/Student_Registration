package com.example.student_registration;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StudentRegistrationApp extends Application {
    private static final String TAG = "StudentRegistrationApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Initialize Firebase if not already initialized
            FirebaseApp.initializeApp(this);

            // Enable offline persistence for Firebase Realtime Database.
            // This ensures all registrations performed while offline are saved locally
            // and automatically synced to the server when network connectivity resumes.
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);

            // Keep the "students" reference synced offline
            DatabaseReference studentsRef = database.getReference("students");
            studentsRef.keepSynced(true);

            Log.d(TAG, "Firebase Offline Persistence Enabled Successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Firebase offline persistence: " + e.getMessage(), e);
        }
    }
}
