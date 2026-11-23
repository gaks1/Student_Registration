package com.example.student_registration.mpesa;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.student_registration.BuildConfig;

import org.json.JSONObject;


import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal Safaricom Daraja client: M-Pesa Express (STK Push) plus STK Push Query.
 *
 * <p>Flow: getToken -> stkPush (the customer's phone shows the PIN prompt) ->
 * queryStatus every few seconds until Safaricom reports success or failure.
 * No callback server is needed, which is what makes this usable from an app
 * with no backend.
 *
 * <p>Credentials are read from {@link BuildConfig}, which is populated from the
 * gitignored local.properties at build time. Note that anything shipped inside an
 * APK can be extracted: acceptable for a sandbox coursework demo, but a production
 * app must call Daraja from a backend instead.
 *
 * <p>Endpoints used:
 * <ul>
 *   <li>GET  /oauth/v1/generate?grant_type=client_credentials</li>
 *   <li>POST /mpesa/stkpush/v1/processrequest</li>
 *   <li>POST /mpesa/stkpushquery/v1/query</li>
 * </ul>
 */
public class MpesaClient {

    /** Simple async callback. */

    public interface Callback<T> {
        void onResult(T result);

        void onError(String message);
    }

    public static class StkResponse {
        public String checkoutRequestId;
        public String merchantRequestId;
        public String customerMessage;
    }

    public enum State {PENDING, SUCCESS, FAILED}

    public static class QueryResult {
        public State state = State.PENDING;
        public String message = "";
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private String token;
    private long tokenExpiresAt;

    /** True when all required M-Pesa settings were supplied in local.properties. */
    public boolean isConfigured() {
        return !BuildConfig.MPESA_CONSUMER_KEY.isEmpty()
                && !BuildConfig.MPESA_CONSUMER_SECRET.isEmpty()
                && !BuildConfig.MPESA_SHORTCODE.isEmpty()
                && !BuildConfig.MPESA_PASSKEY.isEmpty();
    }

    private String baseUrl() {
        return "production".equalsIgnoreCase(BuildConfig.MPESA_ENV)
                ? "https://api.safaricom.co.ke"
                : "https://sandbox.safaricom.co.ke";
    }

    // ------------------------------------------------------------------ public API

    /** Sends the PIN prompt to the customer's phone. */
    public void stkPush(final String phone, final int amount, final String accountRef,
                        final Callback<StkResponse> cb) {
        io.execute(() -> {
            try {
                String timestamp = timestamp();
                JSONObject body = new JSONObject();
                body.put("BusinessShortCode", BuildConfig.MPESA_SHORTCODE);
                body.put("Password", password(timestamp));
                body.put("Timestamp", timestamp);
                body.put("TransactionType", BuildConfig.MPESA_TRANSACTION_TYPE);
                body.put("Amount", amount);
                body.put("PartyA", phone);
                body.put("PartyB", BuildConfig.MPESA_PARTY_B);
                body.put("PhoneNumber", phone);
                body.put("CallBackURL", BuildConfig.MPESA_CALLBACK_URL);
                body.put("AccountReference",
                        accountRef.length() > 12 ? accountRef.substring(0, 12) : accountRef);
                body.put("TransactionDesc", "Fee Payment");

                Raw raw = post("/mpesa/stkpush/v1/processrequest", body);
                JSONObject json = parse(raw);

                if (raw.code == 200 && "0".equals(json.optString("ResponseCode"))) {
                    StkResponse r = new StkResponse();
                    r.checkoutRequestId = json.optString("CheckoutRequestID");
                    r.merchantRequestId = json.optString("MerchantRequestID");
                    r.customerMessage = json.optString("CustomerMessage");
                    deliver(cb, r);
                } else {
                    fail(cb, "M-Pesa request failed: " + errorText(json, raw));
                }
            } catch (Exception e) {
                fail(cb, friendly(e));
            }
        });
    }

    /** Asks Safaricom what happened to a previous STK push. */
    public void queryStatus(final String checkoutRequestId, final Callback<QueryResult> cb) {
        io.execute(() -> {
            try {
                String timestamp = timestamp();
                JSONObject body = new JSONObject();
                body.put("BusinessShortCode", BuildConfig.MPESA_SHORTCODE);
                body.put("Password", password(timestamp));
                body.put("Timestamp", timestamp);
                body.put("CheckoutRequestID", checkoutRequestId);

                Raw raw = post("/mpesa/stkpushquery/v1/query", body);
                JSONObject json = parse(raw);
                QueryResult result = new QueryResult();

                if (json.has("ResultCode")) {
                    String code = json.optString("ResultCode");
                    result.message = json.optString("ResultDesc", "No description");
                    result.state = "0".equals(code) ? State.SUCCESS : State.FAILED;
                } else if (json.has("errorCode")) {
                    // "The transaction is being processed" -> still pending, keep polling.
                    result.state = State.PENDING;
                    result.message = json.optString("errorMessage",
                            "The transaction is still being processed");
                } else {
                    result.state = State.PENDING;
                    result.message = "No result yet";
                }
                deliver(cb, result);
            } catch (Exception e) {
                fail(cb, friendly(e));
            }
        });
    }

    /** Releases the background thread. Call from Activity.onDestroy(). */
    public void shutdown() {
        io.shutdownNow();
    }

    // ------------------------------------------------------------------ internals

    /** Safaricom requires yyyyMMddHHmmss in East African Time. */
    private String timestamp() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
        return fmt.format(new Date());
    }

    /** Base64(shortcode + passkey + timestamp). */
    private String password(String timestamp) {
        String raw = BuildConfig.MPESA_SHORTCODE + BuildConfig.MPESA_PASSKEY + timestamp;
        return Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private String accessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (token != null && now < tokenExpiresAt) {
            return token;
        }

        String basic = Credentials.basic(
                BuildConfig.MPESA_CONSUMER_KEY, BuildConfig.MPESA_CONSUMER_SECRET);
        Request request = new Request.Builder()
                .url(baseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                .addHeader("Authorization", basic)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            String bodyText = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IllegalStateException(
                        "Daraja auth failed (" + response.code() + "): " + bodyText);
            }
            JSONObject json = new JSONObject(bodyText);
            token = json.getString("access_token");
            long expiresIn = json.optLong("expires_in", 3599);
            // Refresh a minute early to avoid a race at expiry.
            tokenExpiresAt = now + (expiresIn - 60) * 1000L;
            return token;
        }
    }

    private static class Raw {
        int code;
        String body;
    }

    private Raw post(String path, JSONObject body) throws Exception {
        Raw raw = new Raw();
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .addHeader("Authorization", "Bearer " + accessToken())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            raw.code = response.code();
            raw.body = response.body() == null ? "" : response.body().string();
        }
        return raw;
    }

    private JSONObject parse(Raw raw) {
        try {
            return new JSONObject(raw.body);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private String errorText(JSONObject json, Raw raw) {
        String msg = json.optString("errorMessage", "");
        if (msg.isEmpty()) {
            msg = json.optString("ResponseDescription", "");
        }
        if (msg.isEmpty()) {
            msg = "HTTP " + raw.code + (raw.body.isEmpty() ? "" : ": " + raw.body);
        }
        return msg;
    }

    private String friendly(Exception e) {
        String m = e.getMessage();
        if (m == null) {
            m = e.getClass().getSimpleName();
        }
        if (m.contains("Unable to resolve host") || m.contains("Failed to connect")) {
            return "No internet connection. M-Pesa payments require a live connection.";
        }
        return m;
    }

    private <T> void deliver(final Callback<T> cb, final T value) {
        main.post(() -> cb.onResult(value));
    }

    private void fail(final Callback<?> cb, final String message) {
        main.post(() -> cb.onError(message));
    }
}
