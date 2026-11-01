package ke.ac.usiu.registration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputEditText;

import ke.ac.usiu.registration.model.Student;
import ke.ac.usiu.registration.util.Session;
import ke.ac.usiu.registration.util.Validators;

/** Activity 1 - personal details. Works offline: data is queued and uploaded later. */
public class RegistrationActivity extends BaseActivity {

    private TextInputEditText etFullNames;
    private TextInputEditText etIdNo;
    private TextInputEditText etRegNo;
    private RadioGroup rgGender;
    private Spinner spCourse;
    private Spinner spDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        etFullNames = findViewById(R.id.etFullNames);
        etIdNo = findViewById(R.id.etIdNo);
        etRegNo = findViewById(R.id.etRegNo);
        rgGender = findViewById(R.id.rgGender);
        spCourse = findViewById(R.id.spCourse);
        spDepartment = findViewById(R.id.spDepartment);

        setupSpinner(spCourse, R.array.courses);
        setupSpinner(spDepartment, R.array.departments);

        findViewById(R.id.btnSaveRegistration).setOnClickListener(v -> save());
        findViewById(R.id.btnViewSummary).setOnClickListener(v ->
                startActivity(new Intent(this, SummaryActivity.class)));
    }

    private void save() {
        String fullNames = text(etFullNames);
        String idNo = text(etIdNo);
        String regNo = text(etRegNo);

        if (!Validators.isValidFullNames(fullNames)) {
            etFullNames.setError("Enter your full names (at least two names)");
            etFullNames.requestFocus();
            return;
        }
        if (!Validators.isValidIdNo(idNo)) {
            etIdNo.setError("5-15 letters, digits or '-' only");
            etIdNo.requestFocus();
            return;
        }
        if (regNo.length() < 3) {
            etRegNo.setError("Enter your registration number");
            etRegNo.requestFocus();
            return;
        }

        int genderId = rgGender.getCheckedRadioButtonId();
        if (genderId == -1) {
            toast("Please select your gender");
            return;
        }
        if (spCourse.getSelectedItemPosition() == 0) {
            toast("Please select your course");
            return;
        }
        if (spDepartment.getSelectedItemPosition() == 0) {
            toast("Please select your department");
            return;
        }

        Student student = new Student();
        student.fullNames = fullNames;
        student.idNo = idNo;
        student.regNo = regNo.toUpperCase();
        student.gender = genderId == R.id.rbMale ? "Male" : "Female";
        student.course = spCourse.getSelectedItem().toString();
        student.department = spDepartment.getSelectedItem().toString();
        student.createdAt = System.currentTimeMillis();

        boolean online = FirebaseHelper.setValue(
                FirebaseHelper.students().document(idNo), student,
                FirebaseHelper.toastListener(this, "Registration details"));

        Session.setIdNo(this, idNo);
        toast(savedMessage(online, "Registration details"));
        startActivity(new Intent(this, LibraryActivity.class));
    }
}
