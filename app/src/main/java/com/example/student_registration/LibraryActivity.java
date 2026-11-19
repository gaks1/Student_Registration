package com.example.student_registration;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 2nd Activity: library book request.
 * Stores records at library/{studentIdNo}/{pushId} in Cloud Firestore.
 *
 * A request is only accepted if the student has already registered (a students/{idNo}
 * document must exist). This keeps the data model consistent with the rest of the app.
 */
public class LibraryActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID_NO = "extra_student_id_no";

    private static final String TAG = "LibraryActivity";
    private static final String COLLECTION_STUDENTS = "students";
    private static final String COLLECTION_LIBRARY = "library";

    private TextInputEditText etIsbn, etAuthor, etTitle, etDateBorrowed, etDateReturn, etStudentIdNo;
    private TextInputLayout tilIsbn, tilAuthor, tilTitle, tilDateBorrowed, tilDateReturn, tilStudentIdNo;
    private Button btnRequestBook, btnClearLibrary, btnNextHostel;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initViews();
        setupDatePickers();
        setupListeners();
        prefillStudentId();
    }

    private void initViews() {
        etIsbn = findViewById(R.id.etIsbn);
        etAuthor = findViewById(R.id.etAuthor);
        etTitle = findViewById(R.id.etTitle);
        etDateBorrowed = findViewById(R.id.etDateBorrowed);
        etDateReturn = findViewById(R.id.etDateReturn);
        etStudentIdNo = findViewById(R.id.etStudentIdNo);

        tilIsbn = findViewById(R.id.tilIsbn);
        tilAuthor = findViewById(R.id.tilAuthor);
        tilTitle = findViewById(R.id.tilTitle);
        tilDateBorrowed = findViewById(R.id.tilDateBorrowed);
        tilDateReturn = findViewById(R.id.tilDateReturn);
        tilStudentIdNo = findViewById(R.id.tilStudentIdNo);

        btnRequestBook = findViewById(R.id.btnRequestBook);
        btnClearLibrary = findViewById(R.id.btnClearLibrary);
        btnNextHostel = findViewById(R.id.btnNextHostel);
    }

    /** Carries the ID number over from Activity 1 so the student does not retype it. */
    private void prefillStudentId() {
        String idNo = getIntent().getStringExtra(EXTRA_STUDENT_ID_NO);
        if (!TextUtils.isEmpty(idNo)) {
            etStudentIdNo.setText(idNo);
        }
    }

    private void setupListeners() {
        btnRequestBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBook();
            }
        });

        btnClearLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearForm();
            }
        });

        btnNextHostel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToHostel();
            }
        });
    }

    private void goToHostel() {
        String idNo = textOf(etStudentIdNo);
        if (TextUtils.isEmpty(idNo)) {
            tilStudentIdNo.setError("Enter a Student ID Number to continue to Hostel selection");
            etStudentIdNo.requestFocus();
            Toast.makeText(this, "Enter a Student ID Number to continue.", Toast.LENGTH_SHORT).show();
            return;
        }
        tilStudentIdNo.setError(null);

        Intent intent = new Intent(LibraryActivity.this, HostelActivity.class);
        intent.putExtra(HostelActivity.EXTRA_STUDENT_ID_NO, idNo);
        startActivity(intent);
    }

    private void setupDatePickers() {
        etDateBorrowed.setOnClickListener(v -> showDatePicker(etDateBorrowed));
        etDateReturn.setOnClickListener(v -> showDatePicker(etDateReturn));
    }

    private void showDatePicker(final TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    target.setText(dateFormat.format(picked.getTime()));
                    clearError(target);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void requestBook() {
        // 1. Read inputs
        String isbn = textOf(etIsbn);
        String author = textOf(etAuthor);
        String title = textOf(etTitle);
        String dateBorrowed = textOf(etDateBorrowed);
        String dateReturn = textOf(etDateReturn);
        String studentIdNo = textOf(etStudentIdNo);

        // 2. Validate
        if (TextUtils.isEmpty(isbn)) {
            tilIsbn.setError("Book ISBN is required");
            etIsbn.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(author)) {
            tilAuthor.setError("Author is required");
            etAuthor.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(dateBorrowed)) {
            tilDateBorrowed.setError("Date Borrowed is required");
            return;
        }
        if (TextUtils.isEmpty(dateReturn)) {
            tilDateReturn.setError("Date of Return is required");
            return;
        }
        if (TextUtils.isEmpty(studentIdNo)) {
            tilStudentIdNo.setError("Student ID Number is required");
            etStudentIdNo.requestFocus();
            return;
        }

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

        // 3. A library request only makes sense for a student who has registered.
        //    Check for the students/{idNo} document first.
        firestore.collection(COLLECTION_STUDENTS).document(studentIdNo).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        tilStudentIdNo.setError("No registration found for this ID Number");
                        Toast.makeText(LibraryActivity.this,
                                "This ID Number is not registered yet. Complete Activity 1 first.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveRequest(firestore, new LibraryRequest(
                            isbn, author, title, dateBorrowed, dateReturn, studentIdNo,
                            System.currentTimeMillis()), online);
                })
                .addOnFailureListener(e -> {
                    // Reading can also fail when offline with an empty cache.
                    Log.e(TAG, "Could not verify student: " + e.getMessage(), e);
                    Toast.makeText(LibraryActivity.this,
                            "Could not verify the student: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveRequest(FirebaseFirestore firestore,
                             LibraryRequest request,
                             boolean online) {
        CollectionReference requests = firestore
                .collection(COLLECTION_LIBRARY)
                .document(request.getStudentIdNo())
                .collection("requests");

        // Firestore writes to the local cache first, so this works with no connection.
        Toast.makeText(this,
                online
                        ? "Saving book request..."
                        : "You are offline. Request saved on this device and will sync "
                          + "when you reconnect.",
                Toast.LENGTH_LONG).show();

        requests.add(request)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Book request synced: library/"
                            + request.getStudentIdNo() + "/requests/"
                            + documentReference.getId());
                    Toast.makeText(LibraryActivity.this,
                            "Book request saved to Firestore.",
                            Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Book request failed: " + e.getMessage(), e);
                    Toast.makeText(LibraryActivity.this,
                            "Failed to save book request: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void clearError(TextInputEditText field) {
        if (field == etDateBorrowed) tilDateBorrowed.setError(null);
        if (field == etDateReturn) tilDateReturn.setError(null);
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
        if (etIsbn != null) etIsbn.setText("");
        if (etAuthor != null) etAuthor.setText("");
        if (etTitle != null) etTitle.setText("");
        if (etDateBorrowed != null) etDateBorrowed.setText("");
        if (etDateReturn != null) etDateReturn.setText("");
        // Keep the ID number: it identifies the student and is usually the same person.

        tilIsbn.setError(null);
        tilAuthor.setError(null);
        tilTitle.setError(null);
        tilDateBorrowed.setError(null);
        tilDateReturn.setError(null);
        tilStudentIdNo.setError(null);

        if (etIsbn != null) etIsbn.requestFocus();
    }
}
