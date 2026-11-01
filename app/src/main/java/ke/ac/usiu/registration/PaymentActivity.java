package ke.ac.usiu.registration;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import ke.ac.usiu.registration.model.Confirmation;
import ke.ac.usiu.registration.model.PaymentRecord;
import ke.ac.usiu.registration.mpesa.MpesaClient;
import ke.ac.usiu.registration.util.NetworkUtil;
import ke.ac.usiu.registration.util.Session;
import ke.ac.usiu.registration.util.Validators;

/**
 * Activity 4 - pay fees with M-Pesa.
 *
 * 1. Student enters phone + amount and taps Pay.
 * 2. We send an STK Push - the M-Pesa PIN prompt pops up on the phone.
 * 3. We poll Safaricom every few seconds; when it reports success we write
 *    confirmations/{idNo}, which is what makes the registration "confirmed".
 */
public class PaymentActivity extends BaseActivity {

    private static final long FIRST_POLL_DELAY_MS = 10_000;
    private static final long POLL_INTERVAL_MS = 6_000;
    private static final int MAX_POLLS = 12;

    private final MpesaClient mpesa = new MpesaClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextInputEditText etIdNo;
    private TextInputEditText etPhone;
    private TextInputEditText etAmount;
    private Button btnPay;
    private Button btnCheck;
    private Button btnSummary;
    private ProgressBar progress;
    private TextView tvStatus;

    private String idNo;
    private String phone;
    private long amount;
    private String checkoutRequestId;
    private int polls;
    private boolean confirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        etIdNo = findViewById(R.id.etPayIdNo);
        etPhone = findViewById(R.id.etPhone);
        etAmount = findViewById(R.id.etAmount);
        btnPay = findViewById(R.id.btnPay);
        btnCheck = findViewById(R.id.btnCheckStatus);
        btnSummary = findViewById(R.id.btnGoSummary);
        progress = findViewById(R.id.progressPay);
        tvStatus = findViewById(R.id.tvPayStatus);

        etIdNo.setText(Session.getIdNo(this));

        // A payment may have been started before the screen was closed - let the student re-check it.
        String lastCheckout = Session.getCheckoutId(this);
        if (!lastCheckout.isEmpty()) {
            checkoutRequestId = lastCheckout;
            phone = Session.getPayPhone(this);
            amount = Session.getPayAmount(this);
            idNo = Session.getIdNo(this);
            btnCheck.setVisibility(View.VISIBLE);
            setStatus("You have a payment waiting to be confirmed. Tap \"Check payment status\".");
        }

        btnPay.setOnClickListener(v -> startPayment());
        btnCheck.setOnClickListener(v -> checkNow());
        btnSummary.setOnClickListener(v ->
                startActivity(new Intent(this, SummaryActivity.class)));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ------------------------------------------------------------------ step 1: STK push

    private void startPayment() {
        String id = text(etIdNo);
        String normalizedPhone = Validators.normalizePhone(text(etPhone));
        String amountText = text(etAmount);

        if (!Validators.isValidIdNo(id)) {
            etIdNo.setError("Enter your ID number (5-15 letters/digits)");
            etIdNo.requestFocus();
            return;
        }
        if (normalizedPhone == null) {
            etPhone.setError("Enter a valid Safaricom number, e.g. 0712345678");
            etPhone.requestFocus();
            return;
        }
        int amountValue;
        try {
            amountValue = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            amountValue = 0;
        }
        if (amountValue < 1 || amountValue > 150000) {
            etAmount.setError("Enter a whole amount from 1 to 150000");
            etAmount.requestFocus();
            return;
        }
        if (!NetworkUtil.isOnline(this)) {
            setStatus("No internet connection. M-Pesa payment needs the internet.\n\n"
                    + "Your details are already saved on this phone - come back to this screen "
                    + "when you are online to pay and confirm your registration.");
            return;
        }
        if (!mpesa.isConfigured()) {
            setStatus("M-Pesa is not configured. Add your Daraja keys to local.properties "
                    + "(see local.properties.example) and rebuild the app.");
            return;
        }

        idNo = id;
        phone = normalizedPhone;
        amount = amountValue;
        Session.setIdNo(this, id);

        setBusy(true);
        setStatus("Sending the M-Pesa request...");

        mpesa.stkPush(phone, amountValue, id, new MpesaClient.Callback<MpesaClient.StkResponse>() {
            @Override
            public void onResult(MpesaClient.StkResponse r) {
                checkoutRequestId = r.checkoutRequestId;
                polls = 0;
                Session.saveLastPayment(PaymentActivity.this, checkoutRequestId, phone, amount);
                recordAttempt(PaymentRecord.PENDING, "STK push sent");
                setStatus("A payment prompt has been sent to " + phone + ".\n\n"
                        + "Enter your M-Pesa PIN on your phone to pay Kshs " + amount + ".");
                btnCheck.setVisibility(View.VISIBLE);
                handler.postDelayed(PaymentActivity.this::pollOnce, FIRST_POLL_DELAY_MS);
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                setStatus("Could not start the payment.\n\n" + message);
            }
        });
    }

    // ------------------------------------------------------------------ step 2: confirm

    private void checkNow() {
        if (checkoutRequestId == null || checkoutRequestId.isEmpty()) {
            return;
        }
        if (!NetworkUtil.isOnline(this)) {
            setStatus("No internet connection. Connect and try again.");
            return;
        }
        handler.removeCallbacksAndMessages(null);
        setBusy(true);
        setStatus("Checking your payment...");
        polls = 0;
        pollOnce();
    }

    private void pollOnce() {
        if (confirmed || isDestroyed()) {
            return;
        }
        mpesa.queryStatus(checkoutRequestId, new MpesaClient.Callback<MpesaClient.QueryResult>() {
            @Override
            public void onResult(MpesaClient.QueryResult r) {
                if (isDestroyed()) {
                    return;
                }
                switch (r.state) {
                    case SUCCESS:
                        onPaid();
                        break;
                    case FAILED:
                        onPaymentFailed(r.message);
                        break;
                    default:
                        waitAndRetry("Waiting for you to enter your PIN...");
                        break;
                }
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) {
                    return;
                }
                waitAndRetry("Still checking (" + message + ")");
            }
        });
    }

    private void waitAndRetry(String message) {
        polls++;
        if (polls >= MAX_POLLS) {
            setBusy(false);
            setStatus("Payment not confirmed yet.\n\nIf you entered your PIN, wait a moment and tap "
                    + "\"Check payment status\". If you cancelled, tap Pay to try again.");
            return;
        }
        setStatus(message);
        handler.postDelayed(this::pollOnce, POLL_INTERVAL_MS);
    }

    private void onPaid() {
        confirmed = true;
        setBusy(false);
        btnPay.setEnabled(false);
        btnCheck.setVisibility(View.GONE);
        btnSummary.setVisibility(View.VISIBLE);
        Session.clearLastPayment(this);

        recordAttempt(PaymentRecord.SUCCESS, "Payment successful");

        Confirmation c = new Confirmation();
        c.status = "CONFIRMED";
        c.amount = amount;
        c.phone = phone;
        c.checkoutRequestId = checkoutRequestId;
        c.confirmedAt = System.currentTimeMillis();
        FirebaseHelper.setValue(FirebaseHelper.confirmations().document(idNo), c,
                FirebaseHelper.toastListener(this, "Registration confirmation"));

        setStatus("Payment of Kshs " + amount + " received.\n\n"
                + "Your registration is now CONFIRMED. Welcome to the University!");
    }

    private void onPaymentFailed(String message) {
        setBusy(false);
        btnCheck.setVisibility(View.GONE);
        Session.clearLastPayment(this);
        recordAttempt(PaymentRecord.FAILED, message);
        setStatus("Payment was not completed.\n\n" + message
                + "\n\nYour registration is NOT confirmed yet. Tap Pay to try again.");
    }

    // ------------------------------------------------------------------ helpers

    /** Saves/updates the attempt in payments/{checkoutRequestId}. */
    private void recordAttempt(String status, String message) {
        if (idNo == null || checkoutRequestId == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("studentIdNo", idNo);
        values.put("phone", phone);
        values.put("amount", amount);
        values.put("status", status);
        values.put("message", message);
        values.put("updatedAt", System.currentTimeMillis());
        if (PaymentRecord.PENDING.equals(status)) {
            values.put("createdAt", System.currentTimeMillis());
        }
        FirebaseHelper.mergeValues(
                FirebaseHelper.payments().document(checkoutRequestId), values, null);
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!busy && !confirmed);
    }

    private void setStatus(String message) {
        tvStatus.setText(message);
    }
}
