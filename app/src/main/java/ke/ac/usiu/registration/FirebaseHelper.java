package ke.ac.usiu.registration;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

/**
 * All database access goes through here (Cloud Firestore).
 *
 * Collections
 *   students/{idNo}              personal details
 *   library/{autoId}             book requests (field studentIdNo links to the student)
 *   hostels/{idNo}               hostel choice
 *   payments/{checkoutRequestId} M-Pesa attempts (field studentIdNo; PENDING/SUCCESS/FAILED)
 *   confirmations/{idNo}         exists only once payment succeeded = registration confirmed
 */
public final class FirebaseHelper {

    public interface WriteListener {
        /** The Firebase server has stored the data. */
        void onSynced();

        void onFailed(String message);
    }

    private FirebaseHelper() {
    }

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static CollectionReference students() {
        return db().collection("students");
    }

    public static CollectionReference library() {
        return db().collection("library");
    }

    public static CollectionReference hostels() {
        return db().collection("hostels");
    }

    public static CollectionReference payments() {
        return db().collection("payments");
    }

    public static CollectionReference confirmations() {
        return db().collection("confirmations");
    }

    /**
     * Writes a document. The local copy is saved instantly; the listener fires only once the
     * server confirms - immediately when online, or later when connectivity returns.
     *
     * @return true if the device was online when the write was made
     */
    public static boolean setValue(DocumentReference ref, Object value, WriteListener listener) {
        boolean online = SyncStatus.isConnected();
        SyncStatus.writeStarted();
        ref.set(value).addOnCompleteListener(task -> finish(task, listener));
        return online;
    }

    /** Creates the document if needed and updates only the given fields. */
    public static boolean mergeValues(DocumentReference ref, Map<String, Object> values,
                                      WriteListener listener) {
        boolean online = SyncStatus.isConnected();
        SyncStatus.writeStarted();
        ref.set(values, SetOptions.merge()).addOnCompleteListener(task -> finish(task, listener));
        return online;
    }

    private static void finish(Task<Void> task, WriteListener listener) {
        SyncStatus.writeFinished();
        if (listener == null) {
            return;
        }
        if (task.isSuccessful()) {
            listener.onSynced();
        } else {
            Exception e = task.getException();
            listener.onFailed(e == null ? "unknown error" : e.getMessage());
        }
    }

    /** A listener that tells the user (with a toast) when the data has really reached the server. */
    public static WriteListener toastListener(Context context, String what) {
        final Context app = context.getApplicationContext();
        return new WriteListener() {
            @Override
            public void onSynced() {
                Toast.makeText(app, what + " uploaded to the server", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(String message) {
                Toast.makeText(app, "Could not upload " + what + ": " + message,
                        Toast.LENGTH_LONG).show();
            }
        };
    }
}
