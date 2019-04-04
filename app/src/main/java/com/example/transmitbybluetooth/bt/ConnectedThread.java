package com.example.transmitbybluetooth.bt;

import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.util.Log;


import com.example.transmitbybluetooth.Common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bluetooth/";

    public interface OnConnectedListener {
        void iConnectionLost();

        void readFile(String filePath, String fileName);

        void readMsg(String readMsg);

        void writeOver();
    }

    private static final String TAG = "CNB_ConnectedThread";
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private DataOutputStream mmOutStream;
    private OnConnectedListener mListener;

    public ConnectedThread(BluetoothSocket socket, String socketType, OnConnectedListener onConnectedLinstener) {
        Log.e(TAG, "ConnectedThread: " + socketType);
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        this.mListener = onConnectedLinstener;
        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        mmInStream = tmpIn;
        mmOutStream = new DataOutputStream(tmpOut);
    }

    public void run() {

        Log.e(TAG, "run: connected 开始啦");

        while (BluetoothChatService.outState == Common.STATE_CONNECTED) {
            try {
                DataInputStream in = new DataInputStream(mmInStream);

                int type = in.readInt();
                if (type == Common.TYPE_IMG) {
                    readFile(in);
                } else if (type == Common.TYPE_STRING) {

                    String readMsg = in.readUTF();
                    mListener.readMsg(readMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
//                connectionLost();
                mListener.iConnectionLost();
                // Start the service over to restart listening mode
                break;
            }
        }
    }


    //读取文件。视频，图片，等都可以。但是得先声明是什么
    private void readFile(DataInputStream in) throws IOException {
        //文件
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            file.mkdir();
        }
        String fileName = in.readUTF();//文件名
        long fileLen = in.readLong();//文件长度
        Log.e(TAG, "run: " + file + "---" + fileLen);
        long len = 0;
        int r;
        byte[] b = new byte[4 * 1024];
        FileOutputStream fileOutputStream = new FileOutputStream(FILE_PATH + fileName);

        while ((r = in.read(b)) != -1) {
            fileOutputStream.write(b, 0, r);
            len += r;
            if (len >= fileLen)
                break;
        }
        mListener.readFile(FILE_PATH, fileName);
    }

    public void writeImg(final String filePath) {
        Log.e(TAG, "writeImg: ????" );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(filePath);
                    File file = new File(filePath);
                    mmOutStream.writeInt(Common.TYPE_IMG); //文件标记
                    mmOutStream.writeUTF(file.getName()); //文件名
                    mmOutStream.writeLong(file.length()); //文件长度
                    int r;
                    byte[] b = new byte[4 * 1024];
                    while ((r = in.read(b)) != -1) {
                        mmOutStream.write(b, 0, r);
                    }
                    Log.e(TAG, "writeImg: 搞定"+currentThread().getName() );
                    mListener.writeOver();
                } catch (IOException ex) {
                    Log.e(TAG, "writeImg: " + ex.getMessage());
                } catch (Exception ex) {
                    Log.e(TAG, "writeImg: " + ex.getMessage());
                }
            }
        }).start();

    }

    public void write(String content) {
        try {
            mmOutStream.writeInt(Common.TYPE_STRING);//文字表示
            mmOutStream.writeUTF(content);
        } catch (IOException ex) {
            Log.e(TAG, "write: " + ex.getMessage());
        } catch (Exception ex) {
            Log.e(TAG, "write: " + ex.getMessage());
        }
    }


    public void cancel() {
        try {
            Log.e(TAG, "cancel: ------???");
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }

    public static int ByteArrayToInt(byte b[]) throws Exception {
        ByteArrayInputStream buf = new ByteArrayInputStream(b);

        DataInputStream dis = new DataInputStream(buf);
        return dis.readInt();

    }

}