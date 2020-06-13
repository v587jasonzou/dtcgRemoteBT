package com.yunda.smartglasses.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;

import com.blankj.utilcode.util.RegexUtils;
import com.yunda.smartglasses.APP;
import com.yunda.smartglasses.audio.Mp3RecorderManager;

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
//    public static final String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+File.separator;

    /*数据帧 - 数据类型*/
    private static final int FLAG_MSG = 0;  //消息标记
    private static final int FLAG_FILE = 1; //文件标记
    public static final int FLAG_ORDER_PHOTO = 2;  //拍照指令标记
    public static final int FLAG_ORDER_AUDIO = 3;  //录音指令标记
    public static final int FLAG_ORDER_VIDEO = 4;  //摄像指令标记

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
            //如果 accept() 返回 BluetoothSocket，则表示已连接套接字。因此，您不应像从客户端那样调用 connect()。
            /**
             *  您应始终调用 cancelDiscovery()，以确保设备在您调用 connect() 之前不会执行设备发现。
             *  如果正在执行发现操作，则会大幅降低连接尝试的速度，并增加连接失败的可能性。
             */
            if (!mSocket.isConnected())
                mSocket.connect();
            notifyUI(Listener.CONNECTED, mSocket.getRemoteDevice());
            mOut = new DataOutputStream(mSocket.getOutputStream());
            DataInputStream in = new DataInputStream(mSocket.getInputStream());
            isRead = true;
            while (isRead) { //死循环读取
                switch (in.readInt()) {
                    //消息类型
                    case FLAG_MSG: //读取短消息
                        String msg = in.readUTF();
                        notifyUI(Listener.MSG, "接收短消息：" + msg);
                        break;

                    //指令类型
                    case FLAG_ORDER_PHOTO: //被请求拍照
                        notifyUI(Listener.ORDER_PHOTO,null);
                        break;
                    case FLAG_ORDER_AUDIO: //被请求录音
                        notifyUI(Listener.ORDER_AUDIO, null);
                        break;
                    case FLAG_ORDER_VIDEO: //被请求录像
                        notifyUI(Listener.ORDER_VIDEO, null);
                        break;

                    //  文件类型
                    case FLAG_FILE: //读取文件
                        Mp3RecorderManager.mkdirs(FILE_PATH);
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
                        }else if (RegexUtils.isVideo(fileName)){
                            notifyUI(Listener.ORDER_VIDEO_RES, FILE_PATH + fileName);
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
    public void sendOrder(int order) {
        if (order != FLAG_ORDER_PHOTO && order != FLAG_ORDER_AUDIO && order != FLAG_ORDER_VIDEO)
            return;
        if (checkSend()) return;
        isSending = true;
        try {
            mOut.writeInt(order); //消息标记
            mOut.flush();

            String orderStr = "未定义指令";
            switch (order) {
                case FLAG_ORDER_PHOTO:
                    orderStr = "拍照";
                    break;
                case FLAG_ORDER_AUDIO:
                    orderStr = "录音";
                    break;
                case FLAG_ORDER_VIDEO:
                    orderStr = "录像";
                    break;
            }
            notifyUI(Listener.MSG, "发送指令：" + orderStr);
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
        APP.EXECUTOR.execute(new Runnable() {
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
            if (mSocket!=null) {
                mSocket.close();
            }
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

    /**切换到UI线程，执行接受到的指令逻辑
     * @param state
     * @param obj
     */
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

        //指令类型
        int MSG = 2;
        int ORDER_PHOTO = 3;
        int ORDER_AUDIO = 4;
        int ORDER_VIDEO = 5;

        //指令的返回结果
        int ORDER_OFFSET = 1000;
        int ORDER_PHOTO_RES = ORDER_OFFSET + ORDER_PHOTO;
        int ORDER_AUDIO_RES = ORDER_OFFSET + ORDER_AUDIO;
        int ORDER_VIDEO_RES = ORDER_OFFSET + ORDER_VIDEO;

        void socketNotify(int state, Object obj);
    }
}
