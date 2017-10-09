package com.hyphenate.chatuidemo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hyphenate.chatuidemo.utils.EMNotificationManager;

@Deprecated
public class GCMPushBroadCast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("info", "gcmpush onreceive");
        String alert = intent.getStringExtra("alert");
        EMNotificationManager.getInstance(context).sendNotification(alert);
    }
}
