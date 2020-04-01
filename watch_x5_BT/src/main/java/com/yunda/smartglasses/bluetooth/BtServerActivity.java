package com.yunda.smartglasses.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.FileUtils;
import com.ibbhub.mp3recorderlib.IAudioRecorder;
import com.yunda.smartglasses.APP;
import com.yunda.smartglasses.R;
import com.yunda.smartglasses.audio.Mp3RecorderManager;
import com.yunda.smartglasses.camera.AppConstant;
import com.yunda.smartglasses.camera.CameraHelper;

import java.io.File;

public class BtServerActivity extends FragmentActivity implements BtBase.Listener {
    private static final int REQ_CODE_DISCOVERABLE = 1;
    private static final int REQ_CODE_TAKE_PHOTO = 2;
    private TextView mTips;
    private EditText mInputMsg;
    private EditText mInputFile;
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
        mInputMsg = findViewById(R.id.input_msg);
        mInputFile = findViewById(R.id.input_file);
        mLogs = findViewById(R.id.tv_log);
        //服务端隐藏图片与音频的查看
        findViewById(R.id.llClient).setVisibility(View.GONE);
        findViewById(R.id.btnStopAudioRecording).setVisibility(View.VISIBLE);
        mServer = new BtServer(this);

        //启用蓝牙可检测性
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);//永久可检测
        startActivityForResult(discoverableIntent,REQ_CODE_DISCOVERABLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQ_CODE_DISCOVERABLE==requestCode) {
            if (resultCode== Activity.RESULT_CANCELED) {
                APP.toast("无法被手持机检测到，请准许程序的请求",0);
                finish();
            }
        } else if (REQ_CODE_TAKE_PHOTO == requestCode && resultCode == Activity.RESULT_OK) {
            String img_path = data.getStringExtra(AppConstant.KEY.IMG_PATH);
            sendFile(img_path);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServer.unListener();
        mServer.close();
    }

    public void sendMsg(View view) {
        if (mServer.isConnected(null)) {
            String msg = mInputMsg.getText().toString();
            if (TextUtils.isEmpty(msg))
                APP.toast("消息不能空", 0);
            else {
                try {
                    //数字
                    if (Integer.parseInt(msg) == BtBase.FLAG_ORDER_PHOTO) {
                        mServer.sendOrder(BtBase.FLAG_ORDER_PHOTO);
                    } else if (Integer.parseInt(msg) == BtBase.FLAG_ORDER_AUDIO) {
                        mServer.sendOrder(BtBase.FLAG_ORDER_AUDIO);
                    } else {
                        mServer.sendMsg(msg);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    mServer.sendMsg(msg);
                }
            }
        } else
            APP.toast("没有连接", 0);
    }

    public void sendFile(View view) {
        if (mServer.isConnected(null)) {
            String filePath = mInputFile.getText().toString();
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mServer.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
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
                msg = String.format("\n%s", "请求当前设备进行拍照");
                mLogs.append(msg);
                CameraHelper.camera(BtServerActivity.this,REQ_CODE_TAKE_PHOTO);

//                new AlertDialog.Builder(this)
//                        .setTitle("请求拍照")
//                        .setDescription("手持机请求拍照")
//                        .setPositiveButton("拍照", new AlertDialog.OnClickListener() {
//                            @Override
//                            public void onClick(AlertDialog d) {
//                                d.dismiss();
//
//                                //允许拍照
//                                try {
////                                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
////                                    startActivityForResult(takePictureIntent, 1);
//
////                                    //直接拍照，添加到上传队列
////                                    TakePhotoFragment takePhotoFragment = new TakePhotoFragment();
////                                    takePhotoFragment.setTakePhotoListener(new TakePhotoFragment.ItemTakePhotoListener() {
////                                        @Override
////                                        public void onPhotosGetted(ArrayList<ImageItem> photos) {
////                                            String filePath = photos.get(0).path;
////                                            sendFile(filePath);
////                                        }
////                                    });
////                                    takePhotoFragment.show(getSupportFragmentManager(), "");
//
//                                    CameraUtil.getInstance().camera(BtServerActivity.this,REQ_CODE_TAKE_PHOTO);
//
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        })
//                        .setNegativeButton("拒绝", new AlertDialog.OnClickListener() {
//                            @Override
//                            public void onClick(AlertDialog d) {
//                                d.dismiss();
//                            }
//                        })
//                        .show();
                break;
            case BtBase.Listener.ORDER_AUDIO:
                msg = String.format("\n%s", "请求当前设备进行录音");
                mLogs.append(msg);
                startAudioRec();

//                new AlertDialog.Builder(this)
//                        .setTitle("录音")
//                        .setDescription("手持机请求录音")
//                        .setPositiveButton("录音", new AlertDialog.OnClickListener() {
//                            @Override
//                            public void onClick(AlertDialog d) {
//                                d.dismiss();
//
//                                //允许录音
//                                mLogs.append("\n录音正在录音(点击确定键结束)...");
//                                FileUtils.createOrExistsDir(BtServer.FILE_PATH);
//                                audioFilePath=BtServer.FILE_PATH + System.currentTimeMillis() + ".mp3";
//                                Util.startAudioRecord(mRecorder, audioFilePath);
//                                isAudioRecording=true;
//                            }
//                        })
//                        .setNegativeButton("拒绝", new AlertDialog.OnClickListener() {
//                            @Override
//                            public void onClick(AlertDialog d) {
//                                d.dismiss();
//                            }
//                        })
//                        .show();
                break;
        }
        APP.toast(msg, 0);
    }

    private void startAudioRec() {
        //允许录音
        mLogs.append("\n录音正在录音(点击确定键结束)...");
        FileUtils.createOrExistsDir(BtServer.FILE_PATH);
        audioFilePath = BtServer.FILE_PATH + System.currentTimeMillis() + ".mp3";
        Mp3RecorderManager.startAudioRecord(mRecorder, audioFilePath);
        isAudioRecording = true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event){
        //标准设备
        if (event.getKeyCode()== KeyEvent.KEYCODE_BACK) {
            if (isAudioRecording) {
                Mp3RecorderManager.stopAudioRecord(mRecorder);
                mLogs.append("\n录音结束,录音文件地址:" + audioFilePath);
                sendFile(audioFilePath);
                isAudioRecording = false;
                return true;
            }
        }
        return super.dispatchKeyEvent(event);

//        //X5眼镜
//        switch (KeyCodeHelper.CheckKeyType(event.getKeyCode(), event)) {
//            case CONFIRM:
//                // Equivalent to Button A clicked.
//                toStopAudioRecording();
//                return true;
//            default:
//                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
//                    toStopAudioRecording();
//                    return true;
//                } else
//                    return super.dispatchKeyEvent(event);
//        }
    }

    private void toStopAudioRecording() {
        if (isAudioRecording) {
            Mp3RecorderManager.stopAudioRecord(mRecorder);
            mLogs.append("\n录音结束,录音文件地址:" + audioFilePath);
            sendFile(audioFilePath);
            isAudioRecording = false;
        }
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