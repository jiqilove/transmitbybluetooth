package com.example.transmitbybluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.transmitbybluetooth.bt.BluetoothChatService;
import com.example.transmitbybluetooth.bt.Constants;
import com.example.transmitbybluetooth.mybase.recyclerView.BaseViewHolder;
import com.example.transmitbybluetooth.mybase.recyclerView.RecyclerViewHelper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;



public class MainActivity extends AppCompatActivity {
    private static final int RESULT_BT = 11;
    private static final int REQUEST_SYSTEM_PIC = 12;
    private static final String TAG = "MainActivity";
    private static final int READ_STORAGE = 13;
    private static final int RESULT_CHECK_PERMISSION = 14;
    @BindView(R.id.img)
    ImageView img;

    @BindView(R.id.selectImg)
    Button btn_selectImg;

    @BindView(R.id.send)
    Button btn_send;

    @BindView(R.id.progress)
    ProgressBar progress;

    private String imgPath = "";
    private String deviceMac = "";

    private ActionBar actionBar;
    private boolean isOpenBlue = false;

    private BluetoothAdapter mBluetoothAdapter;
    private RecyclerViewHelper mRecyclerViewHelper;
    private BluetoothChatService mChatService = null;
    private List<Device> deviceList;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Common.CONNECT_BT:
                    //连接蓝牙去
                    Log.e(TAG, "handleMessage: " + deviceMac);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceMac);
                    if (device!=null){
                        mChatService.connect(device, true);
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    String name = (String) msg.obj;
                    setConnectStatus(name);
                    break;

                case Constants.MESSAGE_READ_IMG:
                    Bundle bundle = (Bundle) msg.obj;
                    String filePath = bundle.getString("filePath");
                    String fileName = bundle.getString("fileName");
                    img.setImageBitmap(BitmapFactory.decodeFile(filePath + fileName));
                    break;

                case Constants.MESSAGE_TOAST:
                    Bundle data1 = msg.getData();
                    String toast = data1.getString(Constants.TOAST);
                    showToast(toast);
                    break;
                case Constants.MESSAGE_WRITE_OVER:

                    showToast("传输完成");
                    imgPath = "";
                    progress.setVisibility(View.GONE);
                    break;

                case Constants.MESSAGE_STATE_CHANGE:
                    if (msg != null) {
                        int status = (int) msg.arg1;
                        if (status == 1 || status == 0) {
                            setConnectStatus("未连接");
                        } else if (status == 2) {
                            setConnectStatus("连接ing");
                        }
                    }
                    break;


            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                showToast("null——intent");
                return;
            }
            if (TextUtils.equals(action, BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = device.getAddress();
                String name = device.getName();
                if (name == null) {
                    name = "null";
                }
                Log.e(TAG, "onReceive: name:" + name);
                Log.e(TAG, "onReceive: address:" + address);
                deviceList.add(new Device(device.getName(), device.getAddress()));
                mRecyclerViewHelper.refresh(deviceList);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                showToast("扫描结束");

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mChatService = new BluetoothChatService(MainActivity.this, mHandler);
        mChatService.start();
        deviceList = new ArrayList<>();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("快传");
        }
        setConnectStatus("未连接");
        progress.setVisibility(View.GONE);
        //选图片
        btn_selectImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permission();
            }
        });
        initBluetooth();

        //发送图片
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage(imgPath);
            }
        });

    }

    //发送文件--->位置
    private void sendImage(String filepath) {
        Log.e(TAG, "sendImage: ");
        if (mChatService.getState() != Common.STATE_CONNECTED) {
            showToast("未链接");
            return;
        }
        if (filepath == null || filepath.length() == 0) {
            showToast("请选择图片");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        mChatService.write(filepath);


    }


    //蓝牙初始化
    private void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showToast("不支持蓝牙");
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                showToast("未打开蓝牙，准备打开");
                permission_bt();
                isOpenBlue = false;
            } else {
                isOpenBlue = true;
//                mChatService = new BluetoothChatService(MainActivity.this, mHandler);
//                mChatService.start();
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    public void setConnectStatus(String status) {
        if (actionBar != null) {
            actionBar.setSubtitle(status);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.bt_status);
        if (isOpenBlue) {
            logToggle.setIcon(R.drawable.bluetooth);
        } else {
            logToggle.setIcon(R.drawable.bluetooth_un);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_matching: //搜索已经配对的列表
                showBluetoothDialg();
                deviceList.clear();
                if (mBluetoothAdapter != null) {
                    Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices) {
                        deviceList.add(new Device(device.getName(), device.getAddress()));
                    }
                    mRecyclerViewHelper.refresh(deviceList);
                }
                break;

            case R.id.bt_search:// 查询蓝牙设备
                showBluetoothDialg();
                deviceList.clear();
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    mBluetoothAdapter.startDiscovery();
                }
                break;

            case R.id.bt_status:

                if (!isOpenBlue) {
                    // 没打开蓝牙
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, RESULT_BT);
                } else {
                    // 最好在弄一弹框，是否确定
                    mBluetoothAdapter.disable();//关闭蓝牙
                    isOpenBlue = false;
                    supportInvalidateOptionsMenu();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //权限申请判断
    private void permission() {
        if (Build.VERSION.SDK_INT > 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_STORAGE);
            } else {
                //打开系统相册
                openAlbum();
            }
        } else {
            openAlbum();
        }

    }

    private void permission_bt() {
        if (Build.VERSION.SDK_INT > 23) {

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, RESULT_CHECK_PERMISSION);
                showToast("未打开权限");
            } else {
                showToast("打开已经权限");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, RESULT_BT);
            }


        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, RESULT_BT);
        }
    }

    //打开相机
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SYSTEM_PIC);//打开系统相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum();
            } else {
                showToast("你拒绝了使用相机的权限");
            }
        } else if (requestCode == RESULT_CHECK_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, RESULT_BT);
            } else {
                showToast("你拒绝了使用蓝牙的权限");
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SYSTEM_PIC && resultCode == RESULT_OK && null != data) {
            Uri uri = data.getData();
            if (uri != null) {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // 获取选择照片的数据视图
                Cursor cursor = getContentResolver().
                        query(uri, filePathColumn, null, null, null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    // 从数据视图中获取已选择图片的路径
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imgPath = cursor.getString(columnIndex);
                    cursor.close();

                } else {
                    try {
                        File file = new File(new URI(uri.toString()));
                        imgPath = file.getPath();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }

                img.setImageBitmap(BitmapFactory.decodeFile(imgPath));

            } else {
                Log.e(TAG, "onActivityResult: uri==null");
            }
        } else if (requestCode == RESULT_BT) {
            if (resultCode == Activity.RESULT_OK) {
                showToast("已经开启蓝牙");
                isOpenBlue = true;
//                mChatService = new BluetoothChatService(MainActivity.this, mHandler);
//                mChatService.start();
            } else {
                isOpenBlue = false;
            }
            supportInvalidateOptionsMenu();
        }
    }


    public void showBluetoothDialg() {
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.my, null, false);
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("选择设备")
                .setView(view)
                .create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
        RecyclerView recyclerView = view.findViewById(R.id.my_recyclerView);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        mRecyclerViewHelper = new RecyclerViewHelper<Device>(MainActivity.this, recyclerView, R.layout.recycler_view_item) {
            @Override
            public List setDatas() {
                return deviceList;
            }

            @Override
            public void onBindView(BaseViewHolder myViewHolder, final Device item, int i) {
                myViewHolder.setText(R.id.name, "设备名称 ：" + item.getName());
                myViewHolder.setText(R.id.mac, "设备地址 ：" + item.getMac());
                myViewHolder.setOnClickListener(R.id.ll_device, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deviceMac = item.getMac();
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        alertDialog.dismiss();
                        mHandler.obtainMessage(Common.CONNECT_BT).sendToTarget();
                    }
                });
            }
        };
        mRecyclerViewHelper.setLinearLayoutManager(false, false, true);
        mRecyclerViewHelper.addItemDecoration();

    }

    public void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
