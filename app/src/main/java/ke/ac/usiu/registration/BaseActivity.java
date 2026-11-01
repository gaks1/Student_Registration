package ke.ac.usiu.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Shared behaviour for all screens: the online/offline banner, the navigation menu
 * and a few small helpers.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        TextView banner = findViewById(R.id.syncBanner);
        if (banner != null) {
            SyncStatus.connected.observe(this, c -> updateBanner(banner));
            SyncStatus.pending.observe(this, p -> updateBanner(banner));
        }
    }

    private void updateBanner(TextView banner) {
        boolean online = SyncStatus.isConnected();
        Integer p = SyncStatus.pending.getValue();
        int pending = p == null ? 0 : p;

        if (online && pending == 0) {
            banner.setText("Online - all data is saved on the server");
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.ok));
        } else if (online) {
            banner.setText("Online - uploading " + pending + " item(s)...");
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.warn));
        } else {
            String extra = pending > 0
                    ? " " + pending + " item(s) will upload when internet returns."
                    : " Data you enter is kept on this phone.";
            banner.setText("Offline -" + extra);
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.err));
        }
    }

    // ---------- Navigation menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Class<?> target = null;
        if (id == R.id.action_registration) {
            target = RegistrationActivity.class;
        } else if (id == R.id.action_library) {
            target = LibraryActivity.class;
        } else if (id == R.id.action_hostel) {
            target = HostelActivity.class;
        } else if (id == R.id.action_payment) {
            target = PaymentActivity.class;
        } else if (id == R.id.action_summary) {
            target = SummaryActivity.class;
        }
        if (target != null) {
            if (!target.equals(getClass())) {
                startActivity(new Intent(this, target));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- Helpers ----------

    protected void setupSpinner(Spinner spinner, @ArrayRes int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /** Short message describing what happened to a save, depending on connectivity. */
    protected String savedMessage(boolean online, String what) {
        return online
                ? what + " saved - uploading now."
                : what + " saved on your phone. It will upload automatically when you are back online.";
    }
}
