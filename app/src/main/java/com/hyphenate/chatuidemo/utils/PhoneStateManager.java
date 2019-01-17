package com.hyphenate.chatuidemo.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PhoneStateManager {
    public interface PhoneStateCallback {
        void onCallStateChanged(int state, String incomingNumber);
    }

    private static final String TAG = "PhoneStateManager";

    private static PhoneStateManager INSTANCE = null;

    private TelephonyManager telephonyManager;
    private List<PhoneStateCallback> stateCallbacks = null;

    public static PhoneStateManager get(Context context) {
        if (INSTANCE == null) {
            synchronized (PhoneStateManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PhoneStateManager(context);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    protected void finalize() throws Throwable {
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        super.finalize();
    }

    public void addStateCallback(PhoneStateCallback callback) {
        if (!stateCallbacks.contains(callback)) {
            stateCallbacks.add(callback);
        }
    }

    public void removeStateCallback(PhoneStateCallback callback) {
        if (stateCallbacks.contains(callback)) {
            stateCallbacks.remove(callback);
        }
    }

    private PhoneStateManager(Context context) {
        Context appContext = context.getApplicationContext();

        telephonyManager = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        stateCallbacks = new CopyOnWriteArrayList<>();
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            for (PhoneStateCallback callback : stateCallbacks) {
                callback.onCallStateChanged(state, incomingNumber);
            }
        }
    };
}
