package ke.ac.usiu.registration;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import ke.ac.usiu.registration.model.ReportData;
import ke.ac.usiu.registration.report.PrintHelper;
import ke.ac.usiu.registration.report.ReportBuilder;
import ke.ac.usiu.registration.report.ReportLoader;
import ke.ac.usiu.registration.util.Session;
import ke.ac.usiu.registration.util.Validators;

/**
 * Shows and prints the registration summary for one student, or for all students.
 * The print dialog also offers "Save as PDF".
 */
public class SummaryActivity extends BaseActivity {

    private TextInputEditText etIdNo;
    private TextView tvPreview;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        etIdNo = findViewById(R.id.etSummaryIdNo);
        tvPreview = findViewById(R.id.tvPreview);
        progress = findViewById(R.id.progressSummary);

        etIdNo.setText(Session.getIdNo(this));

        findViewById(R.id.btnShowMine).setOnClickListener(v -> showAndPrintMine());
        findViewById(R.id.btnPrintAll).setOnClickListener(v -> printAll());
    }

    private void showAndPrintMine() {
        final String idNo = text(etIdNo);
        if (!Validators.isValidIdNo(idNo)) {
            etIdNo.setError("Enter your ID number (5-15 letters/digits)");
            etIdNo.requestFocus();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        ReportLoader.load(idNo, (ReportData data, boolean timedOut) -> {
            if (isDestroyed()) {
                return;
            }
            progress.setVisibility(View.GONE);
            if (!data.students.containsKey(idNo)) {
                tvPreview.setText(timedOut
                        ? "Could not load data. You appear to be offline and nothing is stored "
                        + "on this phone for ID " + idNo + " yet."
                        : "No registration found for ID " + idNo + ".");
                return;
            }
            tvPreview.setText(ReportBuilder.studentText(data, idNo));
            PrintHelper.print(this, "USIU_Registration_" + idNo,
                    ReportBuilder.studentHtml(data, idNo), false);
        });
    }

    private void printAll() {
        progress.setVisibility(View.VISIBLE);
        ReportLoader.load(null, (ReportData data, boolean timedOut) -> {
            if (isDestroyed()) {
                return;
            }
            progress.setVisibility(View.GONE);
            if (data.students.isEmpty()) {
                tvPreview.setText(timedOut
                        ? "Could not load students. You appear to be offline and no data is "
                        + "stored on this phone yet."
                        : "No students have registered yet.");
                return;
            }
            int confirmed = 0;
            for (String id : data.students.keySet()) {
                if (ReportBuilder.isConfirmed(data, id)) {
                    confirmed++;
                }
            }
            tvPreview.setText("Registered students: " + data.students.size()
                    + "\nConfirmed (paid): " + confirmed
                    + "\nPending payment: " + (data.students.size() - confirmed)
                    + (timedOut ? "\n\n(Some data could not be loaded - you may be offline.)" : ""));
            PrintHelper.print(this, "USIU_All_Registered_Students",
                    ReportBuilder.allHtml(data), true);
        });
    }
}
