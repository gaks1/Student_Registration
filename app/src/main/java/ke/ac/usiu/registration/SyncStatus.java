package ke.ac.usiu.registration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import ke.ac.usiu.registration.util.NetworkUtil;

/**
 * Tracks (1) whether the phone currently has internet and (2) how many writes are still
 * waiting for the server to confirm them. The coloured banner at the top of every screen is
 * driven by this class, which makes the offline / online behaviour easy to demonstrate.
 */
public final class SyncStatus {

    public static final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    public static final MutableLiveData<Integer> pending = new MutableLiveData<>(0);

    private static int pendingCount = 0;

    private SyncStatus() {
    }

    /** Starts watching connectivity. Call once from Application.onCreate. */
    public static void start(Context context) {
        connected.setValue(NetworkUtil.isOnline(context));

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return;
        }
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities caps) {
                connected.postValue(
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            }

            @Override
            public void onLost(@NonNull Network network) {
                connected.postValue(false);
            }
        });
    }

    public static boolean isConnected() {
        return Boolean.TRUE.equals(connected.getValue());
    }

    static void writeStarted() {
        pendingCount++;
        pending.setValue(pendingCount);
    }

    /** Called when the SERVER has acknowledged the write (not just the local copy). */
    static void writeFinished() {
        if (pendingCount > 0) {
            pendingCount--;
        }
        pending.setValue(pendingCount);
    }
}
