package com.yunda.smartglasses;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//import com.lzy.imagepicker.ImagePicker;
//import com.lzy.imagepicker.loader.GlideImageLoader;

public class APP extends Application {
    private static final Handler sHandler = new Handler();
    private static Toast sToast; // 单例Toast,避免重复创建，显示时间过长
    public static final Executor EXECUTOR = Executors.newCachedThreadPool();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        super.onCreate();
        sToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksImpl());

//        ImagePicker imagePicker = ImagePicker.getInstance();
//        imagePicker.setImageLoader(new GlideImageLoader());   //设置图片加载器
    }

    public static void toast(String txt, int duration) {
        sToast.setText(txt);
        sToast.setDuration(duration);
        sToast.show();
    }

    public static void runUi(Runnable runnable) {
        sHandler.post(runnable);
    }
}
