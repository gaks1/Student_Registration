package ke.ac.usiu.registration.report;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.List;

import ke.ac.usiu.registration.FirebaseHelper;
import ke.ac.usiu.registration.SyncStatus;
import ke.ac.usiu.registration.model.Confirmation;
import ke.ac.usiu.registration.model.HostelBooking;
import ke.ac.usiu.registration.model.LibraryRequest;
import ke.ac.usiu.registration.model.ReportData;
import ke.ac.usiu.registration.model.Student;

/**
 * Loads the data for a report. Works offline too: when the phone has no internet the data is
 * read straight from Firestore's local cache (which includes writes not yet uploaded).
 *
 * If idNo is given only that student's records are read, otherwise all students are read.
 */
public final class ReportLoader {

    public interface Listener {
        void onLoaded(ReportData data, boolean timedOut);
    }

    private static final long TIMEOUT_MS = 8000;

    private ReportLoader() {
    }

    public static void load(String idNo, Listener listener) {
        final ReportData data = new ReportData();
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] delivered = {false};
        final int[] remaining = {4};
        final Source source = SyncStatus.isConnected() ? Source.DEFAULT : Source.CACHE;

        final Runnable timeout = () -> {
            if (!delivered[0]) {
                delivered[0] = true;
                listener.onLoaded(data, true);
            }
        };
        handler.postDelayed(timeout, TIMEOUT_MS);

        final Runnable oneDone = () -> {
            remaining[0]--;
            if (remaining[0] == 0 && !delivered[0]) {
                delivered[0] = true;
                handler.removeCallbacks(timeout);
                listener.onLoaded(data, false);
            }
        };

        // ---- students
        if (idNo != null) {
            FirebaseHelper.students().document(idNo).get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult().exists()) {
                    addStudent(data, idNo, t.getResult());
                }
                oneDone.run();
            });
        } else {
            FirebaseHelper.students().get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        addStudent(data, d.getId(), d);
                    }
                }
                oneDone.run();
            });
        }

        // ---- library requests
        Query libraryQuery = idNo != null
                ? FirebaseHelper.library().whereEqualTo("studentIdNo", idNo)
                : FirebaseHelper.library();
        libraryQuery.get(source).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                addBooks(data, idNo, t.getResult());
            }
            oneDone.run();
        });

        // ---- hostels
        if (idNo != null) {
            FirebaseHelper.hostels().document(idNo).get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult().exists()) {
                    addHostel(data, idNo, t.getResult());
                }
                oneDone.run();
            });
        } else {
            FirebaseHelper.hostels().get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        addHostel(data, d.getId(), d);
                    }
                }
                oneDone.run();
            });
        }

        // ---- confirmations
        if (idNo != null) {
            FirebaseHelper.confirmations().document(idNo).get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult().exists()) {
                    addConfirmation(data, idNo, t.getResult());
                }
                oneDone.run();
            });
        } else {
            FirebaseHelper.confirmations().get(source).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        addConfirmation(data, d.getId(), d);
                    }
                }
                oneDone.run();
            });
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void addStudent(ReportData data, String id, DocumentSnapshot doc) {
        Student s = doc.toObject(Student.class);
        if (s != null) {
            data.students.put(id, s);
        }
    }

    private static void addHostel(ReportData data, String id, DocumentSnapshot doc) {
        HostelBooking h = doc.toObject(HostelBooking.class);
        if (h != null) {
            data.hostels.put(id, h);
        }
    }

    private static void addConfirmation(ReportData data, String id, DocumentSnapshot doc) {
        Confirmation c = doc.toObject(Confirmation.class);
        if (c != null) {
            data.confirmations.put(id, c);
        }
    }

    private static void addBooks(ReportData data, String idNo, QuerySnapshot snapshot) {
        for (DocumentSnapshot d : snapshot.getDocuments()) {
            LibraryRequest r = d.toObject(LibraryRequest.class);
            if (r == null || r.studentIdNo == null) {
                continue;
            }
            List<LibraryRequest> list = data.library.get(r.studentIdNo);
            if (list == null) {
                list = new ArrayList<>();
                data.library.put(r.studentIdNo, list);
            }
            list.add(r);
        }
    }
}
