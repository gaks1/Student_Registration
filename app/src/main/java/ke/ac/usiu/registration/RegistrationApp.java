package ke.ac.usiu.registration;

import android.app.Application;

/**
 * Cloud Firestore keeps a local disk cache by default on Android, so every write is saved on
 * the phone first and queued. When the phone has no internet the data waits in the queue and
 * Firestore uploads it automatically the moment the connection comes back.
 */
public class RegistrationApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SyncStatus.start(this);
    }
}
