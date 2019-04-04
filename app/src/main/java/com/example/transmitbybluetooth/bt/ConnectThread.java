package com.example.transmitbybluetooth.bt;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import com.example.transmitbybluetooth.Common;

import java.io.IOException;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 */
public class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String mSocketType;

    private BluetoothAdapter mAdapter;
    OnConnectThreadListener mConnectThreadListener;

    public ConnectThread(BluetoothDevice device, boolean secure, BluetoothAdapter mAdapter, OnConnectThreadListener mConnectThread) {
        Log.e(TAG, "ConnectThread: 准备去链接" + device.getName());
        this.mmDevice = device;
        this.mAdapter = mAdapter;
        BluetoothSocket tmp = null;
        this.mConnectThreadListener = mConnectThread;
        mSocketType = secure ? "Secure" : "Insecure";

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
            if (secure) {
                tmp = device.createRfcommSocketToServiceRecord(
                        Common.MY_UUID_SECURE);
                Log.e(TAG, "ConnectThread: 创建安全的");
            } else {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        Common.MY_UUID_INSECURE);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        setName("ConnectThread" + mSocketType);
        try {

            mmSocket.connect();
            Log.e(TAG, "run:connect");
        } catch (IOException e) {
            // Close the socket
            try {
                mmSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() " + mSocketType +
                        " socket during connection failure", e2);
            }
            mConnectThreadListener.connectFailed();
            return;
        }
        synchronized (this) {
            mConnectThreadListener.connectDone();
        }
        mConnectThreadListener.connectConnected(mmSocket, mmDevice, mSocketType);
    }

    public void cancel() {
        try {
            Log.e(TAG, "cancel: ??????");
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
        }
    }


    public interface OnConnectThreadListener {
        void connectConnected(BluetoothSocket socket, BluetoothDevice device, String socketType);

        void connectFailed();

        void connectDone();
    }


}
