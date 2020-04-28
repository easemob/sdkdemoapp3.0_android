package com.hyphenate.chatuidemo.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门用于维护声明周期
 */

public class UserActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks, ActivityState {
    private List<Activity> activityList=new ArrayList<>();
    private List<Activity> resumeActivity=new ArrayList<>();

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Log.e("ActivityLifecycle", "onActivityCreated "+activity.getLocalClassName());
        activityList.add(0, activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.e("ActivityLifecycle", "onActivityStarted "+activity.getLocalClassName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.e("ActivityLifecycle", "onActivityResumed "+activity.getLocalClassName());
        if (!resumeActivity.contains(activity)) {
            resumeActivity.add(activity);
            if(resumeActivity.size() == 1) {
                //do nothing
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.e("ActivityLifecycle", "onActivityPaused "+activity.getLocalClassName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.e("ActivityLifecycle", "onActivityStopped "+activity.getLocalClassName());
        resumeActivity.remove(activity);
        if(resumeActivity.isEmpty()) {
            Log.e("TAG", "在后台了");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        Log.e("ActivityLifecycle", "onActivitySaveInstanceState "+activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.e("ActivityLifecycle", "onActivityDestroyed "+activity.getLocalClassName());
        activityList.remove(activity);
    }

    @Override
    public Activity current() {
        return activityList.size()>0 ? activityList.get(0):null;
    }

    @Override
    public List<Activity> getActivityList() {
        return activityList;
    }

    @Override
    public int count() {
        return activityList.size();
    }

    @Override
    public boolean isFront() {
        return resumeActivity.size() > 0;
    }

    /**
     * 跳转到目标activity
     * @param cls
     */
    public void skipToTarget(Class<?> cls) {
        if(activityList != null && activityList.size() > 0) {
            current().startActivity(new Intent(current(), cls));
            for (Activity activity : activityList) {
                activity.finish();
            }
        }

    }

    /**
     * finish target activity
     * @param cls
     */
    public void finishTarget(Class<?> cls) {
        if(activityList != null && !activityList.isEmpty()) {
            for (Activity activity : activityList) {
                if(activity.getClass() == cls) {
                    activity.finish();
                }
            }
        }
    }

    /**
     * 判断app是否在前台
     * @return
     */
    public boolean isOnForeground() {
        return resumeActivity != null && !resumeActivity.isEmpty();
    }
}
