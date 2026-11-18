package com.example.student_registration;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentRegistrationApp extends Application {
    private static final String TAG = "StudentRegistrationApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp.initializeApp(this);

            // Cloud Firestore keeps a persistent local disk cache by default on Android.
            // That default is exactly what makes offline registration work: every write is
            // saved locally straight away, and Firestore uploads it automatically once the
            // connection returns. We deliberately do NOT override the cache settings here,
            // because replacing them with a memory-only cache would break the offline
            // requirement. The getter below is only used to confirm initialisation succeeded.
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore initialized. Database: "
                    + firestore.getFirestoreSettings().getHost()
                    + " | offline writes are queued locally and uploaded when reconnected.");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization FAILED: " + e.getMessage(), e);
        }
    }
}
