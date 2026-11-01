package ke.ac.usiu.registration.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers the current student's ID and the last M-Pesa request so screens can pre-fill. */
public final class Session {

    private static final String FILE = "usiu_session";

    private Session() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getIdNo(Context c) {
        return prefs(c).getString("idNo", "");
    }

    public static void setIdNo(Context c, String idNo) {
        prefs(c).edit().putString("idNo", idNo).apply();
    }

    public static void saveLastPayment(Context c, String checkoutId, String phone, long amount) {
        prefs(c).edit()
                .putString("checkoutId", checkoutId)
                .putString("payPhone", phone)
                .putLong("payAmount", amount)
                .apply();
    }

    public static String getCheckoutId(Context c) {
        return prefs(c).getString("checkoutId", "");
    }

    public static String getPayPhone(Context c) {
        return prefs(c).getString("payPhone", "");
    }

    public static long getPayAmount(Context c) {
        return prefs(c).getLong("payAmount", 0);
    }

    public static void clearLastPayment(Context c) {
        prefs(c).edit().remove("checkoutId").remove("payPhone").remove("payAmount").apply();
    }
}
