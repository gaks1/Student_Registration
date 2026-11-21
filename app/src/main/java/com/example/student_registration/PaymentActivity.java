package com.example.student_registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 4th Activity: Fee payment via M-Pesa.
 *
 * This class currently provides the SCREEN SHELL only: it displays the form, validates the
 * inputs and pre-fills the student ID carried over from Activity 3. The M-Pesa payment flow
 * (STK push, callback handling, writing to the payments collection) is not implemented here.
 *
 * The record is only meant to be confirmed once payment succeeds, per the specification.
 */
public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID_NO = "extra_student_id_no";

    private static final String TAG = "PaymentActivity";

    private TextInputEditText etPayIdNo, etPhone, etAmount;
    private TextInputLayout tilPayIdNo, tilPhone, tilAmount;
    private Button btnPay, btnCheckStatus;
    private ProgressBar progressPay;
    private TextView tvPayStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupListeners();
        prefillStudentId();
    }

    private void initViews() {
        etPayIdNo = findViewById(R.id.etPayIdNo);
        etPhone = findViewById(R.id.etPhone);
        etAmount = findViewById(R.id.etAmount);

        tilPayIdNo = findViewById(R.id.tilPayIdNo);
        tilPhone = findViewById(R.id.tilPhone);
        tilAmount = findViewById(R.id.tilAmount);

        btnPay = findViewById(R.id.btnPay);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        progressPay = findViewById(R.id.progressPay);
        tvPayStatus = findViewById(R.id.tvPayStatus);
    }

    private void setupListeners() {
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPayment();
            }
        });

        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStatus();
            }
        });
    }

    /** Carries the ID number over from Activity 3. */
    private void prefillStudentId() {
        String idNo = getIntent().getStringExtra(EXTRA_STUDENT_ID_NO);
        if (!TextUtils.isEmpty(idNo)) {
            etPayIdNo.setText(idNo);
        }
    }

    private void startPayment() {
        String idNo = textOf(etPayIdNo);
        String phone = textOf(etPhone);
        String amount = textOf(etAmount);

        if (TextUtils.isEmpty(idNo)) {
            tilPayIdNo.setError("Student ID Number is required");
            etPayIdNo.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("M-Pesa number is required");
            etPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(amount)) {
            tilAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        tilPayIdNo.setError(null);
        tilPhone.setError(null);
        tilAmount.setError(null);

        // TODO(M-Pesa): send an STK push to `phone` for `amount`, then poll/wait for the
        // callback. Only write to the payments collection once the transaction succeeds.
        progressPay.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);
        tvPayStatus.setText("Sending payment request to " + phone + "...");

        Toast.makeText(this,
                "M-Pesa payment is not connected yet. This screen is ready for the "
                        + "payment integration.",
                Toast.LENGTH_LONG).show();

        progressPay.setVisibility(View.GONE);
        btnPay.setEnabled(true);
    }

    private void checkStatus() {
        // TODO(M-Pesa): query the payments collection for the current attempt.
        Toast.makeText(this, "Payment status checking is not connected yet.",
                Toast.LENGTH_SHORT).show();
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
