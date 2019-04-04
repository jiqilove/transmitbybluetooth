package com.example.transmitbybluetooth;

import java.util.UUID;

public class Common {
    // Name for the SDP record when creating server socket
    public static final String NAME_SECURE = "BluetoothChatSecure";
    public static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    public static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public  static  final  int  TYPE_STRING=1000;
    public  static  final  int  TYPE_IMG=1001;
    public  static  final  int  TYPE_VOICE=1002;
    public  static  final  int  TYPE_VIDEO=1003;

    public  static  final  int CONNECT_BT=100;


}
