/*
 *  * EaseMob CONFIDENTIAL
 * __________________
 * Copyright (C) 2017 EaseMob Technologies. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of EaseMob Technologies.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from EaseMob Technologies.
 */
package com.hyphenate.chatuidemo.receiver;

import android.content.Context;
import android.os.Bundle;

import com.huawei.hms.support.api.push.PushReceiver;
import com.hyphenate.chat.EMClient;
import com.hyphenate.util.EMLog;

public class HMSPushReceiver extends PushReceiver{
    private static final String TAG = HMSPushReceiver.class.getSimpleName();

    @Override
    public void onToken(Context context, String token, Bundle extras){
        if(token != null && !token.equals("")){
            //没有失败回调，假定token失败时token为null
            EMLog.d("HWHMSPush", "register huawei hms push token success token:" + token);
            EMClient.getInstance().sendHMSPushTokenToServer(token);
        }else{
            EMLog.e("HWHMSPush", "register huawei hms push token fail!");
        }
    }
}
