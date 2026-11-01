package ke.ac.usiu.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;

import ke.ac.usiu.registration.model.LibraryRequest;
import ke.ac.usiu.registration.util.DateField;
import ke.ac.usiu.registration.util.Session;
import ke.ac.usiu.registration.util.Validators;

/** Activity 2 - request a library book. Works offline. */
public class LibraryActivity extends BaseActivity {

    /** A small sample catalogue. Replace with the real library catalogue as needed. */
    private static final String[][] BOOKS = {
            {"Other (type details below)", "", ""},
            {"Introduction to Algorithms", "9780262046305", "Cormen, Leiserson, Rivest, Stein"},
            {"Clean Code", "9780132350884", "Robert C. Martin"},
            {"Head First Java", "9781491910771", "Kathy Sierra, Bert Bates"},
            {"Database System Concepts", "9780078022159", "Silberschatz, Korth, Sudarshan"},
            {"Computer Networking: A Top-Down Approach", "9780133594140", "Kurose, Ross"},
            {"Operating System Concepts", "9781118063330", "Silberschatz, Galvin, Gagne"},
            {"Design Patterns", "9780201633610", "Gamma, Helm, Johnson, Vlissides"},
            {"Software Engineering", "9780133943030", "Ian Sommerville"},
    };

    private Spinner spBook;
    private TextInputEditText etIsbn;
    private TextInputEditText etAuthor;
    private TextInputEditText etBookTitle;
    private TextInputEditText etDateBorrowed;
    private TextInputEditText etDateReturn;
    private TextInputEditText etLibIdNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        spBook = findViewById(R.id.spBook);
        etIsbn = findViewById(R.id.etIsbn);
        etAuthor = findViewById(R.id.etAuthor);
        etBookTitle = findViewById(R.id.etBookTitle);
        etDateBorrowed = findViewById(R.id.etDateBorrowed);
        etDateReturn = findViewById(R.id.etDateReturn);
        etLibIdNo = findViewById(R.id.etLibIdNo);

        String[] names = new String[BOOKS.length];
        for (int i = 0; i < BOOKS.length; i++) {
            names[i] = BOOKS[i][0];
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBook.setAdapter(adapter);
        spBook.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    etBookTitle.setText(BOOKS[position][0]);
                    etIsbn.setText(BOOKS[position][1]);
                    etAuthor.setText(BOOKS[position][2]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        DateField.attach(this, etDateBorrowed, DateField.daysFromNow(0));
        DateField.attach(this, etDateReturn, DateField.daysFromNow(14));

        etLibIdNo.setText(Session.getIdNo(this));

        findViewById(R.id.btnRequestBook).setOnClickListener(v -> save());
        findViewById(R.id.btnSkipLibrary).setOnClickListener(v -> goNext());
    }

    private void save() {
        String idNo = text(etLibIdNo);
        String isbn = text(etIsbn);
        String author = text(etAuthor);
        String title = text(etBookTitle);

        if (!Validators.isValidIdNo(idNo)) {
            etLibIdNo.setError("Enter your ID number (5-15 letters/digits)");
            etLibIdNo.requestFocus();
            return;
        }
        if (isbn.length() < 3) {
            etIsbn.setError("Enter the book ISBN");
            etIsbn.requestFocus();
            return;
        }
        if (author.isEmpty()) {
            etAuthor.setError("Enter the author");
            etAuthor.requestFocus();
            return;
        }
        if (title.isEmpty()) {
            etBookTitle.setError("Enter the book title");
            etBookTitle.requestFocus();
            return;
        }
        long borrowed = DateField.millis(etDateBorrowed);
        long ret = DateField.millis(etDateReturn);
        if (ret < borrowed) {
            toast("The return date cannot be before the date borrowed");
            return;
        }

        LibraryRequest request = new LibraryRequest();
        request.isbn = isbn;
        request.author = author;
        request.title = title;
        request.dateBorrowed = text(etDateBorrowed);
        request.dateReturn = text(etDateReturn);
        request.studentIdNo = idNo;
        request.requestedAt = System.currentTimeMillis();

        DocumentReference ref = FirebaseHelper.library().document();
        boolean online = FirebaseHelper.setValue(ref, request,
                FirebaseHelper.toastListener(this, "Book request"));

        Session.setIdNo(this, idNo);
        toast(savedMessage(online, "Book request"));
        goNext();
    }

    private void goNext() {
        startActivity(new Intent(this, HostelActivity.class));
    }
}
