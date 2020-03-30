package com.yunda.smartglasses.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yunda.smartglasses.BuildConfig;
import com.yunda.smartglasses.bluetooth.bt.BtClientActivity;
import com.yunda.smartglasses.bluetooth.bt.BtServerActivity;


public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        // 检查蓝牙开关
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            APP.toast("本机没有找到蓝牙硬件或驱动！", 0);
            finish();
            return;
        } else {
            if (!adapter.isEnabled()) {
                //直接开启蓝牙
                adapter.enable();
                //跳转到设置界面
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 112);
            }
        }

        // 检查是否支持BLE蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            APP.toast("本机不支持低功耗蓝牙！", 0);
            finish();
            return;
        }

        // Android 6.0动态请求权限
        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                     .subscribe(granted -> {
                         if (granted) {
                             if ("server".equals(BuildConfig.FLAVOR)) {
                                 btServer(null);
                             } else {
                                 btClient(null);
                             }
                             finish();
                         } else {
                             APP.toast("缺少必须权限！无法正常运行", 0);
                         }
                     });
    }

    public void btClient(View view) {
        startActivity(new Intent(this, BtClientActivity.class));
    }

    public void btServer(View view) {
        startActivity(new Intent(this, BtServerActivity.class));
    }
}
