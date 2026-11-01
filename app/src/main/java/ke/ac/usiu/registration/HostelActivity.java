package ke.ac.usiu.registration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputEditText;

import ke.ac.usiu.registration.model.HostelBooking;
import ke.ac.usiu.registration.util.Session;
import ke.ac.usiu.registration.util.Validators;

/** Activity 3 - hostel information. Works offline. */
public class HostelActivity extends BaseActivity {

    private Spinner spHostelLocation;
    private Spinner spRoomType;
    private RadioGroup rgSharable;
    private TextInputEditText etHostelIdNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hostel);

        spHostelLocation = findViewById(R.id.spHostelLocation);
        spRoomType = findViewById(R.id.spRoomType);
        rgSharable = findViewById(R.id.rgSharable);
        etHostelIdNo = findViewById(R.id.etHostelIdNo);

        setupSpinner(spHostelLocation, R.array.hostel_locations);
        setupSpinner(spRoomType, R.array.room_types);
        etHostelIdNo.setText(Session.getIdNo(this));

        findViewById(R.id.btnSaveHostel).setOnClickListener(v -> save());
        findViewById(R.id.btnSkipHostel).setOnClickListener(v -> goNext());
    }

    private void save() {
        String idNo = text(etHostelIdNo);

        if (spHostelLocation.getSelectedItemPosition() == 0) {
            toast("Please select a hostel location");
            return;
        }
        if (spRoomType.getSelectedItemPosition() == 0) {
            toast("Please select a room type");
            return;
        }
        if (rgSharable.getCheckedRadioButtonId() == -1) {
            toast("Please say whether the room is sharable");
            return;
        }
        if (!Validators.isValidIdNo(idNo)) {
            etHostelIdNo.setError("Enter your ID number (5-15 letters/digits)");
            etHostelIdNo.requestFocus();
            return;
        }

        HostelBooking booking = new HostelBooking();
        booking.studentIdNo = idNo;
        booking.location = spHostelLocation.getSelectedItem().toString();
        booking.roomType = spRoomType.getSelectedItem().toString();
        booking.sharable = rgSharable.getCheckedRadioButtonId() == R.id.rbSharable;
        booking.bookedAt = System.currentTimeMillis();

        boolean online = FirebaseHelper.setValue(
                FirebaseHelper.hostels().document(idNo), booking,
                FirebaseHelper.toastListener(this, "Hostel choice"));

        Session.setIdNo(this, idNo);
        toast(savedMessage(online, "Hostel choice"));
        goNext();
    }

    private void goNext() {
        startActivity(new Intent(this, PaymentActivity.class));
    }
}
