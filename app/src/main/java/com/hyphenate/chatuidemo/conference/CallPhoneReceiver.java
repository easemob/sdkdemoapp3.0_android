package com.hyphenate.chatuidemo.conference;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.util.EMLog;

/**
 * 检测通话状态变化的广播接收器
 */
public class CallPhoneReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        EMLog.i("CallPhoneState", "通话状态变化");
        TelephonyManager tManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        //电话的状态
        switch (tManager.getCallState()) {
        case TelephonyManager.CALL_STATE_RINGING:
            //等待接听状态
            String phoneNumber = intent.getStringExtra("incoming_number");
            EMLog.i("CallPhoneState", "呼入: " + phoneNumber);
            break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
            EMLog.i("CallPhoneState", "通话中");
            EMClient.getInstance().conferenceManager().closeVoiceTransfer();
            EMClient.getInstance().conferenceManager().closeVideoTransfer();
            break;
        case TelephonyManager.CALL_STATE_IDLE:
            EMLog.i("CallPhoneState", "空闲中");
            EMClient.getInstance().conferenceManager().openVoiceTransfer();
            EMClient.getInstance().conferenceManager().openVideoTransfer();
            break;
        }
    }
}
