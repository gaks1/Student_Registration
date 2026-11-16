package com.example.student_registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * 1st Activity: First Year Student Registration Interface (Java Implementation)
 * Allows students to enter:
 * i. Full Names
 * ii. IDNo
 * iii. Registration Number
 * iv. Gender (radio buttons)
 * v. Course (drop down list box / Spinner)
 * vi. Department (drop down list box / Spinner)
 * Saves records to Firebase Realtime Database with offline persistence support.
 */
public class MainActivity extends AppCompatActivity {

    private TextInputEditText etFullNames, etIdNo, etRegNo;
    private RadioGroup rgGender;
    private Spinner spCourse, spDepartment;
    private Button btnRegister, btnClear;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Realtime Database Reference
        try {
            databaseReference = FirebaseDatabase.getInstance().getReference("students");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize Views
        initViews();

        // Setup Dropdown Adapters
        setupSpinners();

        // Set Button Action Listeners
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
    }

    private void initViews() {
        etFullNames = findViewById(R.id.etFullNames);
        etIdNo = findViewById(R.id.etIdNo);
        etRegNo = findViewById(R.id.etRegNo);
        rgGender = findViewById(R.id.rgGender);
        spCourse = findViewById(R.id.spCourse);
        spDepartment = findViewById(R.id.spDepartment);
        btnRegister = findViewById(R.id.btnRegister);
        btnClear = findViewById(R.id.btnClear);
    }

    private void setupSpinners() {
        // Course Dropdown Adapter
        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.courses_array,
                android.R.layout.simple_spinner_item
        );
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

        // Department Dropdown Adapter
        ArrayAdapter<CharSequence> deptAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.departments_array,
                android.R.layout.simple_spinner_item
        );
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(deptAdapter);
    }

    private void registerStudent() {
        // 1. Get input values
        String fullNames = etFullNames.getText() != null ? etFullNames.getText().toString().trim() : "";
        String idNo = etIdNo.getText() != null ? etIdNo.getText().toString().trim() : "";
        String regNo = etRegNo.getText() != null ? etRegNo.getText().toString().trim() : "";

        // 2. Validate Text Inputs
        if (TextUtils.isEmpty(fullNames)) {
            etFullNames.setError("Full Names is required");
            etFullNames.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(idNo)) {
            etIdNo.setError("ID Number is required");
            etIdNo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(regNo)) {
            etRegNo.setError("Registration Number is required");
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
        if (spCourse.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a Course", Toast.LENGTH_SHORT).show();
            return;
        }
        String course = spCourse.getSelectedItem().toString();

        // 5. Validate Department Selection
        if (spDepartment.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a Department", Toast.LENGTH_SHORT).show();
            return;
        }
        String department = spDepartment.getSelectedItem().toString();

        // 6. Save to Firebase Realtime Database
        if (databaseReference == null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("students");
        }

        String studentId = databaseReference.push().getKey();
        if (studentId == null) {
            studentId = String.valueOf(System.currentTimeMillis());
        }

        Student student = new Student(
                studentId,
                fullNames,
                idNo,
                regNo,
                gender,
                course,
                department,
                System.currentTimeMillis()
        );

        // Save student record (works offline & syncs when online)
        databaseReference.child(studentId).setValue(student)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this,
                            "Student Registration Saved Successfully!\n(Offline data queued for sync when online)",
                            Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to register student: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        if (etFullNames != null) etFullNames.setText("");
        if (etIdNo != null) etIdNo.setText("");
        if (etRegNo != null) etRegNo.setText("");
        if (rgGender != null) rgGender.clearCheck();
        if (spCourse != null) spCourse.setSelection(0);
        if (spDepartment != null) spDepartment.setSelection(0);
        if (etFullNames != null) etFullNames.requestFocus();
    }
}
