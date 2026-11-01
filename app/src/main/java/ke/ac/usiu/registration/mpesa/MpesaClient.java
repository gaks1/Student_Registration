package ke.ac.usiu.registration.mpesa;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ke.ac.usiu.registration.BuildConfig;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal Safaricom Daraja client: M-Pesa Express (STK Push) + STK Push Query.
 *
 * Flow: getToken -> stkPush (phone shows the PIN prompt) -> queryStatus every few seconds
 * until Safaricom reports success or failure. No callback server is needed.
 *
 * NOTE: the consumer key/secret live inside the APK. That is acceptable for a coursework
 * demo with sandbox keys, but a production app must call Daraja from a backend server.
 */
public class MpesaClient {

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
        public State state;
        public String message;
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

    public boolean isConfigured() {
        return !BuildConfig.MPESA_CONSUMER_KEY.isEmpty()
                && !BuildConfig.MPESA_CONSUMER_SECRET.isEmpty()
                && !BuildConfig.MPESA_SHORTCODE.isEmpty()
                && !BuildConfig.MPESA_PASSKEY.isEmpty();
    }

    // ------------------------------------------------------------------ public API

    /** Sends the PIN prompt to the customer's phone. */
    public void stkPush(String phone, int amount, String accountRef, Callback<StkResponse> cb) {
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
                body.put("AccountReference", accountRef.length() > 12 ? accountRef.substring(0, 12) : accountRef);
                body.put("TransactionDesc", "Registration");

                Raw raw = post("/mpesa/stkpush/v1/processrequest", body);
                JSONObject json = parse(raw);

                if (raw.code == 200 && "0".equals(json.optString("ResponseCode"))) {
                    StkResponse r = new StkResponse();
                    r.checkoutRequestId = json.getString("CheckoutRequestID");
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
    public void queryStatus(String checkoutRequestId, Callback<QueryResult> cb) {
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
                    deliver(cb, result);
                } else {
                    // While the customer is still entering the PIN Daraja answers with an
                    // error such as "The transaction is being processed".
                    String text = errorText(json, raw);
                    String lower = text.toLowerCase(Locale.ENGLISH);
                    if (lower.contains("being processed") || lower.contains("processing")
                            || "500.001.1001".equals(json.optString("errorCode"))) {
                        result.state = State.PENDING;
                        result.message = text;
                        deliver(cb, result);
                    } else {
                        fail(cb, "Could not check payment: " + text);
                    }
                }
            } catch (Exception e) {
                fail(cb, friendly(e));
            }
        });
    }

    // ------------------------------------------------------------------ internals

    private static class Raw {
        int code;
        String body;
    }

    private String baseUrl() {
        return "production".equalsIgnoreCase(BuildConfig.MPESA_ENV)
                ? "https://api.safaricom.co.ke"
                : "https://sandbox.safaricom.co.ke";
    }

    private synchronized String accessToken() throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpiresAt) {
            return token;
        }
        Request request = new Request.Builder()
                .url(baseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                .header("Authorization",
                        Credentials.basic(BuildConfig.MPESA_CONSUMER_KEY, BuildConfig.MPESA_CONSUMER_SECRET))
                .build();
        try (Response response = http.newCall(request).execute()) {
            String text = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Could not get an M-Pesa access token (HTTP " + response.code()
                        + "). Check MPESA_CONSUMER_KEY / MPESA_CONSUMER_SECRET and MPESA_ENV.");
            }
            JSONObject json = new JSONObject(text);
            token = json.getString("access_token");
            long seconds = json.optLong("expires_in", 3599);
            tokenExpiresAt = System.currentTimeMillis() + (seconds - 60) * 1000;
            return token;
        }
    }

    private Raw post(String path, JSONObject json) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .header("Authorization", "Bearer " + accessToken())
                .post(RequestBody.create(json.toString(), JSON))
                .build();
        try (Response response = http.newCall(request).execute()) {
            Raw raw = new Raw();
            raw.code = response.code();
            raw.body = response.body() != null ? response.body().string() : "";
            return raw;
        }
    }

    private static JSONObject parse(Raw raw) {
        try {
            return new JSONObject(raw.body);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String errorText(JSONObject json, Raw raw) {
        String text = json.optString("errorMessage", "");
        if (text.isEmpty()) {
            text = json.optString("ResponseDescription", "");
        }
        if (text.isEmpty()) {
            text = json.optString("CustomerMessage", "");
        }
        if (text.isEmpty()) {
            text = "HTTP " + raw.code;
        }
        return text;
    }

    private static String friendly(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isEmpty()) ? "Network error while contacting M-Pesa." : m;
    }

    private static String timestamp() {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
        return f.format(System.currentTimeMillis());
    }

    private static String password(String timestamp) {
        String raw = BuildConfig.MPESA_SHORTCODE + BuildConfig.MPESA_PASSKEY + timestamp;
        return Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private <T> void deliver(Callback<T> cb, T result) {
        main.post(() -> cb.onResult(result));
    }

    private <T> void fail(Callback<T> cb, String message) {
        main.post(() -> cb.onError(message));
    }
}
