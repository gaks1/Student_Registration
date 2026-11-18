package com.example.student_registration;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 1st Activity: First Year Student Registration Interface (Java Implementation)
 * Allows students to enter:
 * i. Full Names
 * ii. IDNo
 * iii. Registration Number
 * iv. Gender (radio buttons)
 * v. Course (drop down list box / Spinner)
 * vi. Department (drop down list box / Spinner)
 * Saves records to Cloud Firestore with offline persistence support.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String COLLECTION_STUDENTS = "students";

    private TextInputEditText etFullNames, etIdNo, etRegNo;
    private TextInputLayout tilFullNames, tilIdNo, tilRegNo;
    private RadioGroup rgGender;
    private Spinner spCourse, spDepartment;
    private Button btnRegister, btnClear, btnNextLibrary;

    /**
     * ID number of the student most recently registered in this session.
     * Kept because clearForm() wipes the input fields after a successful save, and the
     * user still needs the ID number when moving on to the library activity.
     */
    private String lastRegisteredIdNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etFullNames = findViewById(R.id.etFullNames);
        etIdNo = findViewById(R.id.etIdNo);
        etRegNo = findViewById(R.id.etRegNo);
        tilFullNames = findViewById(R.id.tilFullNames);
        tilIdNo = findViewById(R.id.tilIdNo);
        tilRegNo = findViewById(R.id.tilRegNo);
        rgGender = findViewById(R.id.rgGender);
        spCourse = findViewById(R.id.spCourse);
        spDepartment = findViewById(R.id.spDepartment);
        btnRegister = findViewById(R.id.btnRegister);
        btnClear = findViewById(R.id.btnClear);
        btnNextLibrary = findViewById(R.id.btnNextLibrary);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.courses_array,
                android.R.layout.simple_spinner_item
        );
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

        ArrayAdapter<CharSequence> deptAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.departments_array,
                android.R.layout.simple_spinner_item
        );
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(deptAdapter);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerStudent();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearForm();
            }
        });

        btnNextLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLibrary();
            }
        });
    }

    /**
     * Opens Activity 2 (library book request), passing the ID number so the request can be
     * stored under library/{idNo}. Requires the ID field to be filled in.
     */
    private void goToLibrary() {
        // Prefer whatever is typed now; otherwise fall back to the ID that was just registered
        // (the form is cleared after a successful save, so the field is usually empty here).
        String idNo = textOf(etIdNo);
        if (TextUtils.isEmpty(idNo)) {
            idNo = lastRegisteredIdNo;
        }

        if (TextUtils.isEmpty(idNo)) {
            tilIdNo.setError("Register a student first, or enter an ID Number");
            etIdNo.requestFocus();
            Toast.makeText(this,
                    "Register a student first, then continue to the library.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        tilIdNo.setError(null);

        Intent intent =
                new Intent(MainActivity.this, LibraryActivity.class);
        intent.putExtra(LibraryActivity.EXTRA_STUDENT_ID_NO, idNo);
        startActivity(intent);
    }

    private void registerStudent() {
        // 1. Get input values
        String fullNames = textOf(etFullNames);
        String idNo = textOf(etIdNo);
        String regNo = textOf(etRegNo);

        // 2. Validate Text Inputs
        if (TextUtils.isEmpty(fullNames)) {
            tilFullNames.setError("Full Names is required");
            etFullNames.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(idNo)) {
            tilIdNo.setError("ID Number is required");
            etIdNo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(regNo)) {
            tilRegNo.setError("Registration Number is required");
            etRegNo.requestFocus();
            return;
        }

        // 3. Validate Gender Radio Selection
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select a Gender", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedGenderId);
        String gender = selectedRadioButton.getText().toString();

        // 4. Validate Course Selection
        if (spCourse.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a Course", Toast.LENGTH_SHORT).show();
            return;
        }
        String course = spCourse.getSelectedItem().toString();

        // 5. Validate Department Selection
        if (spDepartment.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a Department", Toast.LENGTH_SHORT).show();
            return;
        }
        String department = spDepartment.getSelectedItem().toString();

        // 6. Save to Cloud Firestore.
        //    Firestore writes to its local cache FIRST and syncs in the background, so this
        //    works with no connection. The success listener fires when the server confirms -
        //    straight away when online, or once connectivity returns if the device was offline.
        boolean online = isOnline();
        Student student = new Student(
                fullNames,
                idNo,
                regNo,
                gender,
                course,
                department,
                System.currentTimeMillis()
        );

        CollectionReference students;
        try {
            students = FirebaseFirestore.getInstance().collection(COLLECTION_STUDENTS);
        } catch (Exception e) {
            // Surface the real problem instead of failing silently.
            Log.e(TAG, "Firestore is unavailable: " + e.getMessage(), e);
            Toast.makeText(this,
                    "Firebase is not configured: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Document ID = ID number, matching the shared schema used by the rest of the app.
        DocumentReference docRef = students.document(idNo);

        Toast.makeText(this,
                online
                        ? "Saving registration..."
                        : "You are offline. Registration saved on this device and will "
                          + "sync when you reconnect.",
                Toast.LENGTH_LONG).show();

        docRef.set(student)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Registration synced for idNo=" + idNo);
                    lastRegisteredIdNo = idNo;
                    Toast.makeText(MainActivity.this,
                            "Registration saved to Firestore.",
                            Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration failed for idNo=" + idNo + ": " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this,
                            "Failed to save registration: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    /**
     * Connectivity check used only to give the user an accurate message. Firestore handles
     * the actual queuing itself; this does not control whether the write is attempted.
     */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void clearForm() {
        if (etFullNames != null) etFullNames.setText("");
        if (etIdNo != null) etIdNo.setText("");
        if (etRegNo != null) etRegNo.setText("");

        // Reset TextInputLayout error states, otherwise the red error text survives clearForm().
        if (tilFullNames != null) tilFullNames.setError(null);
        if (tilIdNo != null) tilIdNo.setError(null);
        if (tilRegNo != null) tilRegNo.setError(null);

        if (rgGender != null) rgGender.clearCheck();
        if (spCourse != null) spCourse.setSelection(0);
        if (spDepartment != null) spDepartment.setSelection(0);
        if (etFullNames != null) etFullNames.requestFocus();
    }
}
