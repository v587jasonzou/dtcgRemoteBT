package com.yunda.smartglasses.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.ibbhub.mp3recorderlib.IAudioRecorder;
import com.madgaze.smartglass.hardware.KeyCodeHelper;
import com.yunda.smartglasses.APP;
import com.yunda.smartglasses.R;
import com.yunda.smartglasses.audio.Mp3RecorderManager;
import com.yunda.smartglasses.camcorder.CamcorderActivity;
import com.yunda.smartglasses.camcorder.CamcorderHelper;
import com.yunda.smartglasses.camera.AppConstant;
import com.yunda.smartglasses.camera.CameraHelper;

import java.io.File;
import java.text.SimpleDateFormat;

public class BtServerActivity extends AppCompatActivity implements BtBase.Listener {
    private static final int REQ_CODE_DISCOVERABLE = 1;
    private static final int REQ_CODE_TAKE_PHOTO = 2;//拍照
    private static final int REQ_CODE_TAKE_VIDEO = 3;//请求录像
    private TextView mTips;
    private TextView mLogs;
    private BtServer mServer;

    //录音
    private IAudioRecorder mRecorder= Mp3RecorderManager.initMp3Recorder();
    private boolean isAudioRecording=false;
    private String audioFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btserver);
        mTips = findViewById(R.id.tv_tips);
        mLogs = findViewById(R.id.tv_log);
        //服务端隐藏图片与音频的查看
        mServer = new BtServer(this);

        //启用蓝牙可检测性
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);//永久可检测
        startActivityForResult(discoverableIntent,REQ_CODE_DISCOVERABLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_DISCOVERABLE:
                if (resultCode == Activity.RESULT_CANCELED) {
                    APP.toast("无法被手持机检测到，请准许程序的请求", 0);
                    finish();
                }
                break;
            case REQ_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    String img_path = data.getStringExtra(AppConstant.KEY.IMG_PATH);
                    sendFile(img_path);
                }
                break;
            case REQ_CODE_TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    String videoPath = data.getStringExtra(CamcorderActivity.BUNDLE_VIDEO_PATH);
                    sendFile(videoPath);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServer.unListener();
        mServer.close();
    }

    public void btnStopAudioRecording(View view) {
        toStopAudioRecording();
    }

    @Override
    public void socketNotify(int state, final Object obj) {
        if (isDestroyed())
            return;
        String msg = null;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("与%s(%s)连接成功", dev.getName(), dev.getAddress());
                mTips.setText(msg);
                break;
            case BtBase.Listener.DISCONNECTED:
                mServer.listen();
                msg = "连接断开,正在重新监听...";
                mTips.setText(msg);
                break;
            case BtBase.Listener.MSG:
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                break;
            case BtBase.Listener.ORDER_PHOTO:
                msg = String.format("\n%s", "开始拍照");
                mLogs.append(msg);
                CameraHelper.camera(BtServerActivity.this,REQ_CODE_TAKE_PHOTO);
                break;
            case BtBase.Listener.ORDER_AUDIO:
                msg = String.format("\n%s", "开始录音");
                mLogs.append(msg);
                startAudioRec();
                break;
            case BtBase.Listener.ORDER_VIDEO:
                msg = String.format("\n%s", "开始录像");
                mLogs.append(msg);
                CamcorderHelper.video(BtServerActivity.this,REQ_CODE_TAKE_VIDEO);
                break;
        }
        APP.toast(msg, 0);
    }

    /**
     * 开始录音
     */
    private void startAudioRec() {
        //允许录音
        mLogs.append("\n录音正在录音(点击确定键结束)...");
        FileUtils.createOrExistsDir(BtServer.FILE_PATH);
        audioFilePath = BtServer.FILE_PATH + "AUDIO_" + TimeUtils.getNowString(new SimpleDateFormat("yyyyMMdd_HHmmss")) + ".mp3";
        Mp3RecorderManager.startAudioRecord(mRecorder, audioFilePath);
        isAudioRecording = true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event){
        /////////////////////////////////////标准设备//////////////////////////////////////////////
//        if (event.getKeyCode()== KeyEvent.KEYCODE_BACK) {
//            return toStopAudioRecording();
//        }else {
//            return super.dispatchKeyEvent(event);
//        }

        ////////////////////////////////////X5眼镜///////////////////////////////////////////////
        switch (KeyCodeHelper.CheckKeyType(event.getKeyCode(), event)) {
            case CONFIRM:
                // Equivalent to Button A clicked.
                toStopAudioRecording();
                return true;
            default:
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    toStopAudioRecording();
                    return true;
                } else
                    return super.dispatchKeyEvent(event);
        }
    }

    private boolean toStopAudioRecording() {
        if (isAudioRecording) {
            Mp3RecorderManager.stopAudioRecord(mRecorder);
            mLogs.append("\n录音结束,录音文件地址:" + audioFilePath);
            sendFile(audioFilePath);
            isAudioRecording = false;
        }
        return true;
    }

    private void sendFile(String filePath) {
        if (mServer.isConnected(null)) {
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mServer.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
    }
}