package com.example.transmitbybluetooth.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import com.example.transmitbybluetooth.Common;

import java.io.IOException;


public class AcceptThread extends Thread {
    private static final String TAG = "CNB_AcceptThread";
    // The local server socket
    private final BluetoothServerSocket mmServerSocket;
    private String mSocketType;
    private BluetoothAdapter mAdapter;
    OnConnectListener mListener;

    public AcceptThread(boolean secure, BluetoothAdapter bluetoothAdapter, OnConnectListener listener) {

        BluetoothServerSocket tmp = null;
        this.mAdapter = bluetoothAdapter;
        mSocketType = secure ? "Secure" : "Insecure";
        this.mListener = listener;
        try {
            if (secure) {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(Common.NAME_SECURE,
                        Common.MY_UUID_SECURE);
            } else {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        Common.NAME_INSECURE, Common.MY_UUID_INSECURE);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        Log.e(TAG, "secure: " + mSocketType + "开启线程 mAcceptThread" + this);
        setName("AcceptThread" + mSocketType);

        BluetoothSocket socket = null;

        while (BluetoothChatService.outState != Common.STATE_CONNECTED) {
            try {
                socket = mmServerSocket.accept();
                Log.e(TAG, "run: 有人连接上来了");
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                synchronized (this) {
                    switch (BluetoothChatService.outState) {
                        case Common.STATE_LISTEN:
                        case Common.STATE_CONNECTING:
                            Log.e(TAG, "run: 连接中呢");
                            mListener.acceptConnected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case Common.STATE_NONE:
                        case Common.STATE_CONNECTED:
                            Log.e(TAG, "run: 老子已经链接了，你要我断开？？？");
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }
        }

    }

    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
        }
    }

    public interface OnConnectListener {
        void acceptConnected(BluetoothSocket socket, BluetoothDevice
                device, String socketType);
    }


}