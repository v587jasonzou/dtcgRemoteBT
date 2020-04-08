package com.yunda.smartglasses;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class ActivityLifecycleCallbacksImpl implements Application.ActivityLifecycleCallbacks {

    public static final String TAG = "生命周期监听";

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.i(TAG,activity + " - onActivityCreated");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i(TAG,activity + " - onActivityStarted");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i(TAG,activity + " - onActivityResumed");
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.i(TAG,activity + " - onActivityPaused");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.i(TAG,activity + " - onActivityStopped");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.i(TAG,activity + " - onActivitySaveInstanceState");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i(TAG,activity + " - onActivityDestroyed");
    }
}
