package p211b7;

import android.util.Log;

/** Keeps the native wrapper's diagnostic calls routed to Android logging. */
public abstract class AbstractC1880e {
    public static void m4021d(String tag, String message, Throwable error) {
        Log.w(tag, message, error);
    }
}
