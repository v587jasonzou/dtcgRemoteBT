package com.yunda.smartglasses.bluetooth.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.support.annotation.IntRange;

import com.blankj.utilcode.util.RegexUtils;
import com.yunda.smartglasses.bluetooth.APP;
import com.yunda.smartglasses.bluetooth.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

/**
 * 客户端和服务端的基类，用于管理socket长连接
 */
public class BtBase {
    static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bluetooth/";

    /*数据帧 - 数据类型*/
    private static final int FLAG_MSG = 0;  //消息标记
    private static final int FLAG_FILE = 1; //文件标记
    public static final int FLAG_ORDER_PHOTO = 2;  //指令标记
    public static final int FLAG_ORDER_AUDIO = 3;  //指令标记

    private BluetoothSocket mSocket;
    private DataOutputStream mOut;
    private Listener mListener;
    private boolean isRead;
    private boolean isSending;

    BtBase(Listener listener) {
        mListener = listener;
    }

    /**
     * 循环读取对方数据(若没有数据，则阻塞等待)
     */
    void loopRead(BluetoothSocket socket) {
        mSocket = socket;
        try {
            if (!mSocket.isConnected())
                mSocket.connect();
            notifyUI(Listener.CONNECTED, mSocket.getRemoteDevice());
            mOut = new DataOutputStream(mSocket.getOutputStream());
            DataInputStream in = new DataInputStream(mSocket.getInputStream());
            isRead = true;
            while (isRead) { //死循环读取
                switch (in.readInt()) {
                    case FLAG_MSG: //读取短消息
                        String msg = in.readUTF();
                        notifyUI(Listener.MSG, "接收短消息：" + msg);
                        break;
                    case FLAG_ORDER_PHOTO: //读取短消息
                        notifyUI(Listener.ORDER_PHOTO,null);
                        break;
                    case FLAG_ORDER_AUDIO: //读取短消息
                        notifyUI(Listener.ORDER_AUDIO, null);
                        break;
                    case FLAG_FILE: //读取文件
                        Util.mkdirs(FILE_PATH);
                        String fileName = in.readUTF(); //文件名
                        long fileLen = in.readLong(); //文件长度
                        // 读取文件内容
                        long len = 0;
                        int r;
                        byte[] b = new byte[4 * 1024];//4KB
                        FileOutputStream out = new FileOutputStream(FILE_PATH + fileName);
                        notifyUI(Listener.MSG, "正在接收文件(" + fileName + "),请稍后...");
                        while ((r = in.read(b)) != -1) {
                            out.write(b, 0, r);
                            len += r;
                            notifyUI(Listener.MSG, "文件接收进度:" + (1f * len / fileLen * 100) + "%");
                            if (len >= fileLen)
                                break;
                        }

                        notifyUI(Listener.MSG, "文件接收完成(存放在:" + FILE_PATH + ")");
                        if (RegexUtils.isAudio(fileName)) {
                           notifyUI(Listener.ORDER_AUDIO_RES,FILE_PATH + fileName);
                        }else if (RegexUtils.isImage(fileName)){
                            notifyUI(Listener.ORDER_PHOTO_RES, FILE_PATH + fileName);
                        }
                        break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            close();
        }
    }

    /**
     * 发送短消息
     */
    public void sendMsg(String msg) {
        if (checkSend()) return;
        isSending = true;
        try {
            mOut.writeInt(FLAG_MSG); //消息标记
            mOut.writeUTF(msg);
            mOut.flush();
            notifyUI(Listener.MSG, "发送短消息：" + msg);
        } catch (Throwable e) {
            close();
        }
        isSending = false;
    }

    /**
     * 发送短消息
     */
    public void sendOrder(@IntRange(from = FLAG_ORDER_PHOTO,to = FLAG_ORDER_AUDIO) int order) {
        if (order != FLAG_ORDER_PHOTO && order != FLAG_ORDER_AUDIO) return;
        if (checkSend()) return;
        isSending = true;
        try {
            mOut.writeInt(order); //消息标记
            mOut.flush();
            notifyUI(Listener.MSG, "发送指令：" + (order == FLAG_ORDER_PHOTO ? "拍照" : "录音"));
        } catch (Throwable e) {
            close();
        }
        isSending = false;
    }

    /**
     * 发送文件
     */
    public void sendFile(final String filePath) {
        if (checkSend()) return;
        isSending = true;
        Util.EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(filePath);
                    File file = new File(filePath);
                    mOut.writeInt(FLAG_FILE); //文件标记
                    mOut.writeUTF(file.getName()); //文件名
                    mOut.writeLong(file.length()); //文件长度
                    int r;
                    byte[] b = new byte[4 * 1024];
                    notifyUI(Listener.MSG, "正在发送文件(" + filePath + "),请稍后...");
                    while ((r = in.read(b)) != -1)
                        mOut.write(b, 0, r);
                    mOut.flush();
                    notifyUI(Listener.MSG, "文件发送完成.");
                } catch (Throwable e) {
                    close();
                }
                isSending = false;
            }
        });
    }

    /**
     * 释放监听引用(例如释放对Activity引用，避免内存泄漏)
     */
    public void unListener() {
        mListener = null;
    }

    /**
     * 关闭Socket连接
     */
    public void close() {
        try {
            isRead = false;
            mSocket.close();
            notifyUI(Listener.DISCONNECTED, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 当前设备与指定设备是否连接
     */
    public boolean isConnected(BluetoothDevice dev) {
        boolean connected = (mSocket != null && mSocket.isConnected());
        if (dev == null)
            return connected;
        return connected && mSocket.getRemoteDevice().equals(dev);
    }

    // ============================================通知UI===========================================================
    private boolean checkSend() {
        if (isSending) {
            APP.toast("正在发送其它数据,请稍后再发...", 0);
            return true;
        }
        return false;
    }

    private void notifyUI(final int state, final Object obj) {
        APP.runUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListener != null)
                        mListener.socketNotify(state, obj);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public interface Listener {
        int DISCONNECTED = 0;
        int CONNECTED = 1;
        int MSG = 2;
        int ORDER_PHOTO = 3;
        int ORDER_AUDIO = 4;

        int ORDER_PHOTO_RES = 5;
        int ORDER_AUDIO_RES = 6;

        void socketNotify(int state, Object obj);
    }
}
