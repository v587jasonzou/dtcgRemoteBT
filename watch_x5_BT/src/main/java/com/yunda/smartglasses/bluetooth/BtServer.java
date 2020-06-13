package com.yunda.smartglasses.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.yunda.smartglasses.APP;

/**
 * 服务端监听和连接线程，只连接一个设备
 */
public class BtServer extends BtBase {
    private static final String TAG = BtServer.class.getSimpleName();
    private BluetoothServerSocket mSSocket;//作为服务端的蓝牙设备 - 服务器套接字(侦听传入的连接请求)

    BtServer(Listener listener) {
        super(listener);
        listen();
    }

    /**
     * 监听客户端发起的连接
     */
    public void listen() {
        try {
            //代表蓝牙设备
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            //需要是开启当前蓝牙设备的可检测性
//            mSSocket = adapter.listenUsingRfcommWithServiceRecord(TAG, SPP_UUID); //加密传输，Android强制执行配对，弹窗显示配对码
            mSSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(TAG, SPP_UUID); //明文传输(不安全)，无需配对
            // 开启子线程
            APP.EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //连接成功后,socket代表端到端的连接
                        BluetoothSocket socket = mSSocket.accept(); // 监听连接
                        /**
                         *  与 TCP/IP 不同，RFCOMM 一次只允许每个通道有一个已连接的客户端，
                         *  因此大多数情况下，在接受已连接的套接字后，您可以立即在 BluetoothServerSocket 上调用 close()。
                         */
                        mSSocket.close(); // 关闭监听，只连接一个设备
                        loopRead(socket); // 循环读取
                    } catch (Throwable e) {
                        close();
                    }
                }
            });
        } catch (Throwable e) {
            close();
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            mSSocket.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}