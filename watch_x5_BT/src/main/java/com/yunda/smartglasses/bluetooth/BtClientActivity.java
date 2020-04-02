package com.yunda.smartglasses.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yunda.smartglasses.R;
import com.yunda.smartglasses.audio.AudioPlayer;
import com.yunda.smartglasses.APP;

import java.io.File;

public class BtClientActivity extends FragmentActivity implements BtBase.Listener, BtReceiver.Listener, BtDevAdapter.Listener {
    private final BtDevAdapter mBtDevAdapter = new BtDevAdapter(this);
    private final BtClient mClient = new BtClient(this);
    private TextView mTips;
    private EditText mInputMsg;
    private EditText mInputFile;
    private TextView mLogs;

    //播放收到的图片或者音频信息
    private Button btnAudioPlay;
    private ImageView ivImgPreview;
    private ImageView ivLargeImgPrew;

    private BtReceiver mBtReceiver;
    //录音与播放
    private AudioPlayer ap = new AudioPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btclient);
        RecyclerView rv = findViewById(R.id.rv_bt);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mBtDevAdapter);
        mTips = findViewById(R.id.tv_tips);
        mInputMsg = findViewById(R.id.input_msg);
        mInputFile = findViewById(R.id.input_file);
        mLogs = findViewById(R.id.tv_log);
        btnAudioPlay = findViewById(R.id.btnAudioPlay);
        ivImgPreview = findViewById(R.id.ivImgPreview);
        ivLargeImgPrew = findViewById(R.id.ivLargeImgPrew);
        mLogs = findViewById(R.id.tv_log);

        //客户端展示图片与音频的查看
        findViewById(R.id.llClient).setVisibility(View.VISIBLE);
        findViewById(R.id.btnStopAudioRecording).setVisibility(View.GONE);
        btnAudioPlay.setVisibility(View.GONE);

        mBtReceiver = new BtReceiver(this, this);//注册蓝牙广播

        //检测其他
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ap.onDestroy();
        //取消扫描
        mBtDevAdapter.cancelDiscovery();
        unregisterReceiver(mBtReceiver);
        mClient.unListener();
        mClient.close();
    }

    @Override
    public void onItemClick(BluetoothDevice dev) {
        if (mClient.isConnected(dev)) {
            APP.toast("已经连接了", 0);
            return;
        }
        mClient.connect(dev);
        APP.toast("正在连接...", 0);
        mTips.setText("正在连接...");
    }

    public void btnAudioPlay(View view) {
        try {
            if (!ap.isPlaying()) {
                String filepath = (String) view.getTag();
                ap.play(new File(filepath), null);
                btnAudioPlay.setText("播放中(点击停止)");
            } else {
                ap.stop();
                btnAudioPlay.setText("播放音频");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**点击图片，看大图
     * @param view
     */
    public void ivSmallImgPrew(View view) {
        ivLargeImgPrew.setVisibility(View.VISIBLE);
        String filePath = (String) ivImgPreview.getTag(R.id.ivImgPreview);
        Glide.with(this).load(filePath).into(ivLargeImgPrew);
//        ivLargeImgPrew.setImageBitmap(ImageUtils.getBitmap(filePath));
    }

    public void ivLargeImgPrew(View view) {
        view.setVisibility(View.GONE);
    }

    @Override
    public void foundDev(BluetoothDevice dev) {
        mBtDevAdapter.add(dev);
    }

    // 重新扫描
    public void startDiscovery(View view) {
        mBtDevAdapter.startDiscovery();
    }

    public void sendMsg(View view) {
        if (mClient.isConnected(null)) {
            String msg = mInputMsg.getText().toString();
            if (TextUtils.isEmpty(msg))
                APP.toast("消息不能空", 0);
            else {
                mClient.sendMsg(msg);
            }
        } else
            APP.toast("没有连接", 0);
    }

    public void sendFile(View view) {
        String filePath = mInputFile.getText().toString();
        sendFile(filePath);
//        if (mClient.isConnected(null)) {
//            String filePath = mInputFile.getText().toString();
//            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
//                APP.toast("文件无效", 0);
//            else
//                mClient.sendFile(filePath);
//        } else
//            APP.toast("没有连接", 0);
    }

    @Override
    public void socketNotify(int state, final Object obj) {
        if (isDestroyed())
            return;
        String msg = null;
        String filePath;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("与%s(%s)连接成功", dev.getName(), dev.getAddress());
                mTips.setText(msg);
                break;
            case BtBase.Listener.DISCONNECTED:
                msg = "连接断开";
                mTips.setText(msg);
                break;
            case BtBase.Listener.MSG:
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                break;
            case BtBase.Listener.ORDER_PHOTO_RES:
                msg = String.format("\n%s", "请查看左侧图片");
                mLogs.append(msg);

                filePath = (String) obj;
                ivImgPreview.setVisibility(View.VISIBLE);
                btnAudioPlay.setVisibility(View.GONE);

                Glide.with(this).load(filePath).into(ivImgPreview);
                ivImgPreview.setTag(R.id.ivImgPreview,filePath);
//                ivImgPreview.setImageBitmap(ImageUtils.getBitmap(filePath));
                break;
            case BtBase.Listener.ORDER_AUDIO_RES:
                msg = String.format("\n%s", "请查看左侧音频");
                mLogs.append(msg);

                filePath = (String) obj;
                ivImgPreview.setVisibility(View.GONE);
                btnAudioPlay.setVisibility(View.VISIBLE);
                btnAudioPlay.setTag(filePath);
                break;
        }
        APP.toast(msg, 0);
    }


    private void sendFile(String filePath) {
        if (mClient.isConnected(null)) {
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mClient.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
    }

    public void reqTakePhoto(View view) {
        mClient.sendOrder(BtBase.FLAG_ORDER_PHOTO);
    }

    public void reqTakeAudio(View view) {
        mClient.sendOrder(BtBase.FLAG_ORDER_AUDIO);
    }

    public void reqTakeVideo(View view) {
        mClient.sendOrder(BtBase.FLAG_ORDER_VIDEO);
    }
}