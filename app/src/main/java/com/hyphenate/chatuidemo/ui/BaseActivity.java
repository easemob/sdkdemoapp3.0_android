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

package com.hyphenate.chatuidemo.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import com.hyphenate.chatuidemo.DemoApplication;
import com.hyphenate.chatuidemo.conference.CallFloatWindow;
import com.hyphenate.chatuidemo.conference.ConferenceActivity;
import com.hyphenate.chatuidemo.conference.ConferenceInviteActivity;
import com.hyphenate.easeui.ui.EaseBaseActivity;

import java.util.List;

@SuppressLint("Registered")
public class BaseActivity extends EaseBaseActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfConferenceExit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * 将此方法放置在基类，用于检查是否有正在进行的音视频会议
     */
    private void checkIfConferenceExit() {
        // 如果当前的activity是否是ConferenceActivity
        if(this instanceof ConferenceActivity || this instanceof ConferenceInviteActivity) {
            return;
        }
        UserActivityLifecycleCallbacks lifecycleCallbacks = DemoApplication.getInstance().getLifecycleCallbacks();
        if(lifecycleCallbacks == null) {
            return;
        }
        List<Activity> activityList = lifecycleCallbacks.getActivityList();
        if(activityList != null && activityList.size() > 0) {
            for (Activity activity : activityList) {
                if(activity instanceof ConferenceActivity) {
                    //如果没有显示悬浮框，则启动ConferenceActivity
                    if(activity.isFinishing()) {
                        return;
                    }
                    if(!CallFloatWindow.getInstance(DemoApplication.getInstance()).isShowing()) {
                        ConferenceActivity.startConferenceCall(this, null);
                    }
                }
            }
        }
    }

}
