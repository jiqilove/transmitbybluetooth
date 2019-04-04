/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.transmitbybluetooth.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.transmitbybluetooth.Common;


/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService implements AcceptThread.OnConnectListener {
    // Debugging
    private static final String TAG = "CNB_BT_service";


    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    public static int outState;

    // Constants that indicate the current connection state


    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        outState = mState = Common.STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.e("cnb", "setState() " + mState + " -> " + state);
        mState = state;
        outState = mState;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(Common.STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true, mAdapter, this);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false, mAdapter, this);
            mInsecureAcceptThread.start();
        }
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.e(TAG, "connect:deviceName " + device.getName() + "secure:" + secure + "device" + device);

        // Cancel any thread attempting to make a connection
        if (mState == Common.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure, mAdapter, new ConnectThread.OnConnectThreadListener() {
            @Override
            public void connectConnected(BluetoothSocket socket, BluetoothDevice device, String socketType) {
                Log.e(TAG, "connectConnected: " + device.getName());
                connected(socket, device, socketType);
            }

            @Override
            public void connectFailed() {
                connectionFailed();
            }

            @Override
            public void connectDone() {
                mConnectThread = null;
            }
        });
        mConnectThread.start();
        setState(Common.STATE_CONNECTING);
    }

    @Override
    public void acceptConnected(BluetoothSocket socket, BluetoothDevice device, String socketType) {
        connected(socket, device, socketType);
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {


        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType, new ConnectedThread.OnConnectedListener() {
            @Override
            public void iConnectionLost() {
                connectionLost();
            }

            @Override
            public void readMsg(String readMsg) {
                Bundle bundle = new Bundle();
                bundle.putString("msg", readMsg);
                mHandler.obtainMessage(Constants.MESSAGE_READ, bundle).sendToTarget();
            }

            @Override
            public void writeOver() {
                mHandler.obtainMessage(Constants.MESSAGE_WRITE_OVER).sendToTarget();
            }

            @Override
            public void readFile(String filePath, String fileName) {


                Bundle bundle = new Bundle();
                bundle.putString("filePath", filePath);
                bundle.putString("fileName", fileName);
                mHandler.obtainMessage(Constants.MESSAGE_READ_IMG, bundle).sendToTarget();
                ;

            }
        });


        mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME, device.getName()).sendToTarget();

        setState(Common.STATE_CONNECTED);
        mConnectedThread.start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(Common.STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * <p>
     * The bytes to writeImg
     * //     * @see ConnectedThread#writeImg(byte[])
     */
    public void writeMsg(String msg) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != Common.STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the writeImg unsynchronized
        r.write(msg);
    }

    public void write(String filePath) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != Common.STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        Log.e(TAG, "write: " + r.getName());
        r.writeImg(filePath);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Log.e(TAG, "connectionFailed:链接失败 ");
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.e(TAG, "connectionLost:连接失败 ");
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        this.start();
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */

}
