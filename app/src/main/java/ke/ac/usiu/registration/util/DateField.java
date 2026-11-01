package ke.ac.usiu.registration.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Turns a read-only EditText into a date picker. The chosen time (millis) is kept in the tag. */
public final class DateField {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    private DateField() {
    }

    public static void attach(Context context, EditText field, long initialMillis) {
        set(field, initialMillis);
        field.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            Object tag = field.getTag();
            if (tag instanceof Long) {
                c.setTimeInMillis((Long) tag);
            }
            new DatePickerDialog(context, (view, year, month, day) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, day, 0, 0, 0);
                picked.set(Calendar.MILLISECOND, 0);
                set(field, picked.getTimeInMillis());
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    public static void set(EditText field, long millis) {
        field.setTag(millis);
        field.setText(FORMAT.format(millis));
    }

    public static long millis(EditText field) {
        Object tag = field.getTag();
        return tag instanceof Long ? (Long) tag : 0L;
    }

    public static long daysFromNow(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
