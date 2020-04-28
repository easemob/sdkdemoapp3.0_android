/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hyphenate.chatuidemo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.VideoCallActivity;
import com.hyphenate.chatuidemo.ui.VoiceCallActivity;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.EasyUtils;

public class CallReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		if(!DemoHelper.getInstance().isLoggedIn())
		    return;
		//username
		String from = intent.getStringExtra("from");
		//call type
		String type = intent.getStringExtra("type");
		if(Build.VERSION.SDK_INT >= 29 && !EasyUtils.isAppRunningForeground(context)) {
            Intent fullScreenIntent;
            String content = "";
            if("video".equals(type)) { //video call
                fullScreenIntent = new Intent(context, VideoCallActivity.class);
                content = context.getString(R.string.alert_request_video, from);
            }else {
                fullScreenIntent = new Intent(context, VoiceCallActivity.class);
                content = context.getString(R.string.alert_request_voice, from);
            }
            fullScreenIntent.putExtra("username", from)
                    .putExtra("isComingCall", true).
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            DemoHelper.getInstance().getNotifier().notify(fullScreenIntent, "Hyphenate", content);
		}else {
            startTargetActivity(context, from, type);
        }

		EMLog.d("CallReceiver", "app received a incoming call");
	}

    private void startTargetActivity(Context context, String from, String type) {
        if("video".equals(type)){ //video call
            context.startActivity(new Intent(context, VideoCallActivity.class).
                    putExtra("username", from).putExtra("isComingCall", true).
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }else{ //voice call
            context.startActivity(new Intent(context, VoiceCallActivity.class).
                    putExtra("username", from).putExtra("isComingCall", true).
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

}
