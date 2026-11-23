package com.example.student_registration;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 3rd Activity: Hostel Selection.
 * Enables students to select hostel information:
 * i. Hostel Location (Spinner)
 * ii. Room type (bedsitter, 1 bedroom, etc.) (Spinner)
 * iii. Whether sharable or not (RadioGroup)
 * iv. Student IDNo (to be entered)
 * Saves records to Cloud Firestore at hostels/{studentIdNo}.
 * Aligns with Wilson's branch database schema and supports offline persistence.
 */
public class HostelActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID_NO = "extra_student_id_no";

    private static final String TAG = "HostelActivity";
    private static final String COLLECTION_STUDENTS = "students";
    private static final String COLLECTION_HOSTELS = "hostels";

    private Spinner spHostelLocation, spRoomType;
    private RadioGroup rgSharable;
    private TextInputEditText etStudentIdNo;
    private TextInputLayout tilStudentIdNo;
    private Button btnSaveHostel, btnClearHostel, btnNextPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hostel);

        initViews();
        setupSpinners();
        setupListeners();
        prefillStudentId();
    }

    private void initViews() {
        spHostelLocation = findViewById(R.id.spHostelLocation);
        spRoomType = findViewById(R.id.spRoomType);
        rgSharable = findViewById(R.id.rgSharable);
        etStudentIdNo = findViewById(R.id.etStudentIdNo);
        tilStudentIdNo = findViewById(R.id.tilStudentIdNo);
        btnSaveHostel = findViewById(R.id.btnSaveHostel);
        btnClearHostel = findViewById(R.id.btnClearHostel);
        btnNextPayment = findViewById(R.id.btnNextPayment);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.hostel_locations_array,
                android.R.layout.simple_spinner_item
        );
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHostelLocation.setAdapter(locationAdapter);

        ArrayAdapter<CharSequence> roomAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.room_types_array,
                android.R.layout.simple_spinner_item
        );
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRoomType.setAdapter(roomAdapter);
    }

    private void setupListeners() {
        btnSaveHostel.setOnClickListener(v -> saveHostelChoice());
        btnClearHostel.setOnClickListener(v -> clearForm());
        btnNextPayment.setOnClickListener(v -> goToPayment());
    }

    /**
     * Opens Activity 4 (fee payment), passing the ID number so the payment screen can be
     * pre-filled without the student retyping it.
     */
    private void goToPayment() {
        String idNo = textOf(etStudentIdNo);
        if (TextUtils.isEmpty(idNo)) {
            tilStudentIdNo.setError("Enter the Student ID Number first");
            etStudentIdNo.requestFocus();
            Toast.makeText(this,
                    "Enter the Student ID Number first, then continue to payment.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        tilStudentIdNo.setError(null);

        Intent intent = new Intent(HostelActivity.this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_STUDENT_ID_NO, idNo);
        startActivity(intent);
    }

    private void prefillStudentId() {
        String idNo = getIntent().getStringExtra(EXTRA_STUDENT_ID_NO);
        if (!TextUtils.isEmpty(idNo)) {
            etStudentIdNo.setText(idNo);
        }
    }

    private void saveHostelChoice() {
        // 1. Get input values
        String studentIdNo = textOf(etStudentIdNo);

        // 2. Validate Hostel Location Selection
        if (spHostelLocation.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a Hostel Location", Toast.LENGTH_SHORT).show();
            return;
        }
        String location = spHostelLocation.getSelectedItem().toString();

        // 3. Validate Room Type Selection
        if (spRoomType.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a Room Type", Toast.LENGTH_SHORT).show();
            return;
        }
        String roomType = spRoomType.getSelectedItem().toString();

        // 4. Validate Sharable Radio Selection
        int selectedSharableId = rgSharable.getCheckedRadioButtonId();
        if (selectedSharableId == -1) {
            Toast.makeText(this, "Please select whether the room is sharable", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean sharable = (selectedSharableId == R.id.rbSharable);

        // 5. Validate Student ID Number
        if (TextUtils.isEmpty(studentIdNo)) {
            tilStudentIdNo.setError("Student ID Number is required");
            etStudentIdNo.requestFocus();
            return;
        }
        tilStudentIdNo.setError(null);

        final FirebaseFirestore firestore;
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firestore is unavailable: " + e.getMessage(), e);
            Toast.makeText(this, "Firebase is not configured: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean online = isOnline();

        // 6. Verify that the student document exists first (students/{studentIdNo}).
        //
        //    Firestore READS fail when offline and the document is not cached, while WRITES
        //    always succeed locally. Gating the write behind the read would block offline
        //    use, which contradicts the offline requirement. So verify when possible, and
        //    allow the write when verification is impossible.
        final String finalLocation = location;
        final String finalRoomType = roomType;
        final boolean finalSharable = sharable;

        if (!online) {
            saveBookingToFirestore(firestore, buildBooking(studentIdNo, finalLocation,
                    finalRoomType, finalSharable), online);
            return;
        }

        firestore.collection(COLLECTION_STUDENTS).document(studentIdNo).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        tilStudentIdNo.setError("No registration found for this ID Number");
                        Toast.makeText(HostelActivity.this,
                                "This ID Number is not registered yet. Complete Activity 1 first.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveBookingToFirestore(firestore, buildBooking(studentIdNo, finalLocation,
                            finalRoomType, finalSharable), online);
                })
                .addOnFailureListener(e -> {
                    // Read failed despite a network being reported (e.g. captive portal).
                    // Do not discard the student's choice: save it for later sync.
                    Log.w(TAG, "Student verification failed, saving offline-style: "
                            + e.getMessage(), e);
                    Toast.makeText(HostelActivity.this,
                            "Could not verify the student, but the hostel choice has been "
                                    + "saved on this device and will sync later.",
                            Toast.LENGTH_LONG).show();
                    saveBookingToFirestore(firestore, buildBooking(studentIdNo, finalLocation,
                            finalRoomType, finalSharable), false);
                });
    }

    private HostelBooking buildBooking(String studentIdNo, String location, String roomType,
                                       boolean sharable) {
        return new HostelBooking(studentIdNo, location, roomType, sharable,
                System.currentTimeMillis());
    }

    private void saveBookingToFirestore(FirebaseFirestore firestore,
                                        HostelBooking booking,
                                        boolean online) {
        // Document ID = studentIdNo, matching Wilson's branch database schema
        DocumentReference docRef = firestore
                .collection(COLLECTION_HOSTELS)
                .document(booking.getStudentIdNo());

        Toast.makeText(this,
                online
                        ? "Saving hostel choice..."
                        : "You are offline. Hostel choice saved locally and will sync when reconnected.",
                Toast.LENGTH_LONG).show();

        docRef.set(booking)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Hostel choice synced: hostels/" + booking.getStudentIdNo());
                    Toast.makeText(HostelActivity.this,
                            "Hostel choice saved to Firestore.",
                            Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hostel choice save failed: " + e.getMessage(), e);
                    Toast.makeText(HostelActivity.this,
                            "Failed to save hostel choice: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

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
        if (spHostelLocation != null) spHostelLocation.setSelection(0);
        if (spRoomType != null) spRoomType.setSelection(0);
        if (rgSharable != null) rgSharable.clearCheck();
        // Keep etStudentIdNo prefilled as it identifies the student
        if (tilStudentIdNo != null) tilStudentIdNo.setError(null);
    }
}
