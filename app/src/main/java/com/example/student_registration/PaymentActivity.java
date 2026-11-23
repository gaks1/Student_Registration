package com.example.student_registration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.student_registration.mpesa.MpesaClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * 4th Activity: fee payment via M-Pesa (Daraja STK Push).
 *
 * <p>Flow:
 * <ol>
 *   <li>Student enters ID, phone and amount, then taps Pay Fees.</li>
 *   <li>We send an STK Push; the M-Pesa PIN prompt appears on their phone.</li>
 *   <li>We poll Safaricom until it reports success or failure.</li>
 *   <li>Only on success do we write confirmations/{idNo}, which is what makes the
 *       registration "confirmed", per the specification.</li>
 * </ol>
 *
 * <p>Unlike Activities 1-3, this screen deliberately does NOT work offline. Daraja is a
 * remote service, so a payment cannot be queued locally. Offline attempts are refused
 * with a clear message rather than being silently accepted.
 */
public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID_NO = "extra_student_id_no";

    private static final String TAG = "PaymentActivity";

    private static final String COLLECTION_PAYMENTS = "payments";
    private static final String COLLECTION_CONFIRMATIONS = "confirmations";

    /** Give the customer time to read the prompt before the first status check. */
    private static final long FIRST_POLL_DELAY_MS = 8_000L;
    private static final long POLL_INTERVAL_MS = 6_000L;
    private static final int MAX_POLLS = 15;

    private final MpesaClient mpesa = new MpesaClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextInputEditText etPayIdNo, etPhone, etAmount;
    private TextInputLayout tilPayIdNo, tilPhone, tilAmount;
    private Button btnPay, btnCheckStatus;
    private ProgressBar progressPay;
    private TextView tvPayStatus;

    // State for the payment currently in flight.
    private String checkoutRequestId;
    private String studentIdNo;
    private String phone;
    private long amount;
    private int polls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupListeners();
        prefillStudentId();
        reportConfiguration();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        mpesa.shutdown();
        super.onDestroy();
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
                if (!TextUtils.isEmpty(checkoutRequestId)) {
                    setBusy(true);
                    pollStatus();
                }
            }
        });
    }

    private void prefillStudentId() {
        String idNo = getIntent().getStringExtra(EXTRA_STUDENT_ID_NO);
        if (!TextUtils.isEmpty(idNo)) {
            etPayIdNo.setText(idNo);
        }
    }

    /**
     * Warns immediately (in logcat and on screen) when the M-Pesa credentials were not
     * supplied in local.properties, so the failure is obvious rather than mysterious.
     */
    private void reportConfiguration() {
        if (!mpesa.isConfigured()) {
            Log.w(TAG, "M-Pesa is NOT configured. Add MPESA_CONSUMER_KEY, "
                    + "MPESA_CONSUMER_SECRET, MPESA_SHORTCODE and MPESA_PASSKEY to "
                    + "local.properties, then rebuild.");
            tvPayStatus.setText("M-Pesa is not configured yet. Add your Daraja "
                    + "credentials to local.properties and rebuild the app.");
        }
    }

    // ------------------------------------------------------------------ step 1: validate

    private void startPayment() {
        String idNo = textOf(etPayIdNo);
        String rawPhone = textOf(etPhone);
        String amountText = textOf(etAmount);

        tilPayIdNo.setError(null);
        tilPhone.setError(null);
        tilAmount.setError(null);

        if (TextUtils.isEmpty(idNo)) {
            tilPayIdNo.setError("Student ID Number is required");
            etPayIdNo.requestFocus();
            return;
        }

        String normalizedPhone = normalizePhone(rawPhone);
        if (normalizedPhone == null) {
            tilPhone.setError("Enter a valid Safaricom number, e.g. 0712 345 678");
            etPhone.requestFocus();
            return;
        }

        int amountValue;
        try {
            amountValue = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            amountValue = 0;
        }
        if (amountValue < 1 || amountValue > 150_000) {
            tilAmount.setError("Enter a whole amount from 1 to 150,000");
            etAmount.requestFocus();
            return;
        }

        if (!mpesa.isConfigured()) {
            Toast.makeText(this,
                    "M-Pesa credentials are missing. See local.properties.",
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, "Payment blocked: M-Pesa not configured.");
            return;
        }

        // Payments genuinely cannot work offline: Daraja is a remote service.
        if (!isOnline()) {
            Toast.makeText(this,
                    "You are offline. M-Pesa payments need an internet connection.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        this.studentIdNo = idNo;
        this.phone = normalizedPhone;
        this.amount = amountValue;
        this.polls = 0;

        setBusy(true);
        tvPayStatus.setText("Sending M-Pesa request to " + normalizedPhone + "...");
        Log.i(TAG, "STK push requested for idNo=" + idNo + " phone=" + normalizedPhone
                + " amount=" + amountValue);

        mpesa.stkPush(normalizedPhone, amountValue, idNo,
                new MpesaClient.Callback<MpesaClient.StkResponse>() {
                    @Override
                    public void onResult(MpesaClient.StkResponse response) {
                        checkoutRequestId = response.checkoutRequestId;
                        Log.i(TAG, "STK push accepted. checkoutRequestId="
                                + checkoutRequestId);

                        tvPayStatus.setText("Enter your M-Pesa PIN on your phone to "
                                + "confirm KShs " + amount + ".");
                        btnCheckStatus.setVisibility(View.VISIBLE);

                        writePaymentRecord(PaymentRecord.STATUS_PENDING,
                                "STK push sent; awaiting customer PIN");

                        handler.postDelayed(pollRunnable, FIRST_POLL_DELAY_MS);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "STK push failed: " + message);
                        setBusy(false);
                        tvPayStatus.setText("Payment request failed: " + message);
                    }
                });
    }

    // ------------------------------------------------------------------ step 2: poll

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollStatus();
        }
    };

    private void pollStatus() {
        if (TextUtils.isEmpty(checkoutRequestId)) {
            return;
        }
        polls++;
        Log.d(TAG, "Polling status (" + polls + "/" + MAX_POLLS + ") for "
                + checkoutRequestId);

        mpesa.queryStatus(checkoutRequestId, new MpesaClient.Callback<MpesaClient.QueryResult>() {
            @Override
            public void onResult(MpesaClient.QueryResult result) {
                handler.removeCallbacks(pollRunnable);

                if (result.state == MpesaClient.State.SUCCESS) {
                    onPaymentSucceeded(result.message);
                } else if (result.state == MpesaClient.State.FAILED) {
                    onPaymentFailed(result.message);
                } else if (polls < MAX_POLLS) {
                    tvPayStatus.setText("Waiting for you to enter your M-Pesa PIN... ("
                            + polls + "/" + MAX_POLLS + ")");
                    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                } else {
                    setBusy(false);
                    tvPayStatus.setText("Still pending after " + MAX_POLLS
                            + " checks. Tap \"Check Payment Status\" to try again.");
                    writePaymentRecord(PaymentRecord.STATUS_PENDING,
                            "Timed out waiting for confirmation");
                }
            }

            @Override
            public void onError(String message) {
                handler.removeCallbacks(pollRunnable);
                if (polls < MAX_POLLS) {
                    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                } else {
                    setBusy(false);
                    tvPayStatus.setText("Could not check status: " + message);
                }
            }
        });
    }

    // ------------------------------------------------------------------ step 3: outcome

    private void onPaymentSucceeded(String message) {
        Log.i(TAG, "Payment SUCCEEDED for idNo=" + studentIdNo + " checkout="
                + checkoutRequestId);
        setBusy(false);
        tvPayStatus.setText("Payment confirmed. Registration is now complete.");
        Toast.makeText(this, "Payment successful. Registration confirmed.",
                Toast.LENGTH_LONG).show();

        writePaymentRecord(PaymentRecord.STATUS_SUCCESS, message);
        writeConfirmation();
    }

    private void onPaymentFailed(String message) {
        Log.w(TAG, "Payment FAILED for idNo=" + studentIdNo + ": " + message);
        setBusy(false);
        tvPayStatus.setText("Payment failed: " + message);
        writePaymentRecord(PaymentRecord.STATUS_FAILED, message);
    }

    // ------------------------------------------------------------------ step 4: Firestore

    /** Record of the attempt. Keyed by the Safaricom checkout ID, so set() not add(). */
    private void writePaymentRecord(String status, String message) {
        Map<String, Object> record = new HashMap<>();
        record.put("studentIdNo", studentIdNo);
        record.put("phone", phone);
        record.put("amount", amount);
        record.put("checkoutRequestId", checkoutRequestId);
        record.put("status", status);
        record.put("message", message);
        record.put("updatedAt", System.currentTimeMillis());

        firestore().collection(COLLECTION_PAYMENTS)
                .document(checkoutRequestId)
                .set(record)
                .addOnFailureListener(e -> Log.e(TAG,
                        "Could not write payment record: " + e.getMessage(), e));
    }

    /** Written only on success - this is what "confirms" the registration. */
    private void writeConfirmation() {
        Map<String, Object> confirmation = new HashMap<>();
        confirmation.put("studentIdNo", studentIdNo);
        confirmation.put("phone", phone);
        confirmation.put("amount", amount);
        confirmation.put("checkoutRequestId", checkoutRequestId);
        confirmation.put("confirmedAt", System.currentTimeMillis());

        firestore().collection(COLLECTION_CONFIRMATIONS)
                .document(studentIdNo)
                .set(confirmation)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Registration confirmed: confirmations/" + studentIdNo);
                    btnPay.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Log.e(TAG,
                        "Could not write confirmation: " + e.getMessage(), e));
    }

    private FirebaseFirestore firestore() {
        return FirebaseFirestore.getInstance();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Normalises a Kenyan phone number to the 2547XXXXXXXX form Daraja requires.
     * Accepts 0712345678, 712345678, 254712345678 and +254712345678.
     *
     * @return the normalised number, or null when it is not a valid Safaricom number
     */
    private String normalizePhone(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("[^0-9]", "");

        if (digits.startsWith("254")) {
            // already in international form
        } else if (digits.startsWith("0")) {
            digits = "254" + digits.substring(1);
        } else if (digits.startsWith("7") || digits.startsWith("1")) {
            digits = "254" + digits;
        } else {
            return null;
        }

        if (digits.length() != 12) {
            return null;
        }
        return digits;
    }

    private void setBusy(boolean busy) {
        progressPay.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!busy);
        btnPay.setVisibility(busy ? View.GONE : View.VISIBLE);
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
}
