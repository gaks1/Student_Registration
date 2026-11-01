package ke.ac.usiu.registration.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern PHONE = Pattern.compile("^(?:\\+?254|0)?([17]\\d{8})$");

    private Validators() {
    }

    /**
     * ID numbers are used as Firebase keys, so they may only contain letters, digits and '-'
     * (Firebase keys cannot contain . # $ [ ] /).
     */
    public static boolean isValidIdNo(String idNo) {
        return idNo != null && idNo.matches("[A-Za-z0-9-]{5,15}");
    }

    public static boolean isValidFullNames(String names) {
        return names != null && names.trim().matches(".*\\S+\\s+\\S+.*") && names.trim().length() >= 5;
    }

    /** Returns the phone in 2547XXXXXXXX / 2541XXXXXXXX form, or null if it is not a valid number. */
    public static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[\\s-]", "");
        Matcher m = PHONE.matcher(cleaned);
        return m.matches() ? "254" + m.group(1) : null;
    }
}
