package com.yunda.smartglasses;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yunda.smartglasses.bluetooth.BtClientActivity;
import com.yunda.smartglasses.bluetooth.BtServerActivity;


public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

//        // Android 6.0动态请求权限
//        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance
//        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
//                     .subscribe(granted -> {
//                         if (granted) {
//                             boolean isServer=true;
//                             isServer = "i6310C".equals(DeviceUtils.getModel());
//                             if (isServer) {
//                                 ToastUtils.showShort("启动BlueTooth-服务端");
//                                 startActivity(new Intent(this, BtServerActivity.class));
//                             } else {
//                                 ToastUtils.showShort("启动BlueTooth-客户端");
//                                 startActivity(new Intent(this, BtClientActivity.class));
//                             }
//                             finish();
//                         } else {
//                             APP.toast("缺少必须权限！无法正常运行", 0);
//                         }
//                     });
    }

    public void btClient(View view) {
        // Android 6.0动态请求权限
        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                     .subscribe(granted -> {
                         if (granted) {
                             startActivity(new Intent(this, BtClientActivity.class));
                         } else {
                             APP.toast("缺少必须权限！无法正常运行", 0);
                         }
                     });
    }

    public void btServer(View view) {
        // Android 6.0动态请求权限
        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                     .subscribe(granted -> {
                         if (granted) {
                             startActivity(new Intent(this, BtServerActivity.class));
                         } else {
                             APP.toast("缺少必须权限！无法正常运行", 0);
                         }
                     });
    }
}
